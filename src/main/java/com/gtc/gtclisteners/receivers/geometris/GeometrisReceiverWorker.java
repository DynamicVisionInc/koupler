package com.gtc.gtclisteners.receivers.geometris;

import java.util.Arrays;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import com.gtc.gtclisteners.Config.ConfigIntProperty;
import com.gtc.gtclisteners.common.Utilities;
import com.gtc.gtclisteners.receivers.ReceiverWorker;
import com.gtc.gtclisteners.receivers.ReceiverWorkerContainer;

import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class GeometrisReceiverWorker extends ReceiverWorker implements Runnable {

	/**
	 * Temporarily needed to ACK back to device
	 */
	protected DatagramSocket socket = null;

	/**
	 * String containing the message from the GPS device
	 */
	private String message;

	/**
	 * {@link IoSession} used to send and receive messages with the GPS device
	 */
	private IoSession session;

	/**
	 * Constructor
	 *
	 * @param message - String containing the message from the GPS device.
	 * @param session - the {@link IoSession} used to send and receive messages
	 * @param container - {@link ReceiverWorkerContainer} used to populate the data members
	 * 		of the worker.
	 */
	public GeometrisReceiverWorker(String message, IoSession session, ReceiverWorkerContainer container, DatagramSocket socket) {
		super(container);

		this.message = message;
		this.session = session;
		this.socket = socket;
	}

	@Override
	public void run() {

		startTime = System.currentTimeMillis();

		// Clear the thread name so that the previous thread name will not be displayed.
		Thread.currentThread().setName("");
		logger.info("raw message=" + message);
		String[] messageParts = message.split(",");

		//	sample message
		//	19AC,81A162910062,1487283429,33.49260,-117.14679,62,5,1,0,0,0.1,5,N,302.20000

		// Only accept messages that contain at least the CRC, serial number, and coordinates.
		// Also accept 'UPDATE' messages.  These are used to reset the odometer counter after a firmware update.
		if(messageParts.length > 4 || message.contains("UPDATE")) {
			// If the first term is more than 5 characters, the message is a legacy message.
			// TODO remove this check when all units have been updated to the formatting scheme.
			String timestamp;
			if(message.contains("UPDATE")) {
				messageParts = message.split(" ");
				uid = messageParts[0];
				timestamp = "0";
			}
			else if(messageParts[0].length() < 5) {
				uid = messageParts[1];
				timestamp = messageParts[2];
			}
			else {
				uid = messageParts[0];
				timestamp = "0";
			}

			//packetCounterConnection.sendMessage(config.APPLICATION_NAME + "@" + uid);
			Thread.currentThread().setName(uid + "-" + count);

			boolean ackBack;
			// Reject duplicate messages
			if(isMessageDuplicate(message.getBytes())) {
				logger.error("DROPPING MESSAGE: duplicate");
				ackBack = true;
			}
			else {
				ackBack = sendMessageToProcessor(message, uid, startTime);
			}

			// If the message was successfully sent to the processor, send an acknowledgment to the GPS device.
			if(ackBack) {
				String ack = "ACK " + timestamp;

				//Utilities.currentThreadSleep(config.getProperty(ConfigIntProperty.RECEIVER_ACK_DELAY));

				try {
					IoBuffer buffer = IoBuffer.allocate(ack.getBytes().length);
					buffer.put(ack.getBytes());
					buffer.flip();
					//session.write(buffer);

					InetAddress address = InetAddress.getByName("127.0.0.1");

					DatagramPacket packet = new DatagramPacket(ack.getBytes(), ack.getBytes().length, address, 4241);
					socket.send(packet);

					buffer.free();
					buffer = null;

				} catch (Exception e) {

				}
				logger.info("ack=" + ack);
			}
			//sendOTA(uid);
		}
		else {
			logger.error("DROPPING MESSAGE: too short");
		}

		session.close(false);

		statsWorkerTime(startTime);

	}

	@Override
	protected boolean processOtaMessage(String message) {

		boolean success = false;

		if(message != null){
			try {
				logger.info("OTA: "+ message);

				if(message.startsWith("geofence")) {
					message = parseGeofenceData(message);
				}

				logger.info("sending: " + message);
				IoBuffer buffer = IoBuffer.allocate(message.getBytes().length);
				buffer.put(message.getBytes());
				buffer.flip();
				session.write(buffer);
				buffer.free();
				buffer = null;

				success = true;
			}
			catch(Exception e) {
				logger.error("Error sending OTA: "+ Utilities.errorHandle(e));
			}
		}
		else {
			logger.error("Error processing message - message is null");
		}

		return success;
	} // processOtaMessage

	private String parseGeofenceData(String geofenceString) {

		String geofenceOta = "";

		// geofence|<OPERATION>|<ID>|<RADIUS>|<COORDINATES>
		String[] geofenceData = geofenceString.split("\\|");

		String operation = geofenceData[1];
		if(operation.equals("create")) {
			String geofenceId = geofenceData[2];
			int radius = Integer.parseInt(geofenceData[3]);
			String[] coordinates = Arrays.copyOfRange(geofenceData, 4, geofenceData.length);
			String originLongitude = coordinates[0].split(":")[1];
			String originLatitude = coordinates[0].split(":")[0];

			StringBuilder geofenceMessage = new StringBuilder();
			geofenceMessage.append("ADDFENCE ");
			geofenceMessage.append(geofenceId);
			geofenceMessage.append(" ");
			geofenceMessage.append(originLongitude);
			geofenceMessage.append(" ");
			geofenceMessage.append(originLatitude);
			geofenceMessage.append(" TYPE=3 ");

			if(radius > 0) {
				geofenceMessage.append("RADIUS=");
				geofenceMessage.append(radius);
				geofenceMessage.append(" ");
			}
			else {
				geofenceMessage.append("POLY=");
				int numPoints = coordinates.length;
				geofenceMessage.append(numPoints);

				double lastLatitude = Double.parseDouble(originLatitude);
				double lastLongitude = Double.parseDouble(originLongitude);
				double latitude;
				double longitude;
				for(int i = 1; i < numPoints; i++) {
					latitude = Double.parseDouble(coordinates[i].split(":")[0]);
					longitude = Double.parseDouble(coordinates[i].split(":")[1]);

					long deltaX = Math.round((latitude - lastLatitude) * 1000000);
					long deltaY = Math.round((longitude - lastLongitude) * 1000000);

					geofenceMessage.append(",");
					geofenceMessage.append(deltaX);
					geofenceMessage.append(",");
					geofenceMessage.append(deltaY);

					lastLatitude = latitude;
					lastLongitude = longitude;
				}
			}

//			geofenceMessage.append([DEBOUNCE=]);
//			geofenceMessage.append([IDLE=]);
//			geofenceMessage.append([ON_FREQ=]);
//			geofenceMessage.append([OFF_FREQ=]);

			geofenceOta = geofenceMessage.toString();
		}
		else if(operation.equals("delete")) {
			String geofenceId = geofenceData[2];
			geofenceOta = "DELETEFENCE " + geofenceId;
		}

		return geofenceOta;
	}
}
