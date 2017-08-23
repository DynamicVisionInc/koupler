package com.gtc.gtclisteners.receivers.calamp;


import java.util.Arrays;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import com.gtc.gtclisteners.Config.ConfigIntProperty;
import com.gtc.gtclisteners.common.Utilities;
import com.gtc.gtclisteners.common.Utilities.BinaryMessageType;
import com.gtc.gtclisteners.receivers.ReceiverWorker;
import com.gtc.gtclisteners.receivers.ReceiverWorkerContainer;

import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Defines common attributes and behaviors for all Calamp receiver workers.
 *
 * @author T. Benedict
 * @author J. Stairs
 */
public abstract class CalampBaseWorker extends ReceiverWorker {

	/**
	 * Temporarily needed to ACK back to device
	 */
	protected DatagramSocket socket = null;

	/**
	 * Service type of the message
	 */
	protected Integer serviceType;

	/**
	 * Message type
	 */
	protected byte messageType;

	/**
	 * Part one of the message sequence ID
	 */
	private byte seq1;

	/**
	 * Part two of the message sequence ID
	 */
	private byte seq2;

	/**
	 * Byte array containing the raw message from the GPS device
	 */
	protected byte[] rawMessage;

	/**
	 * Session used to communicate with the GPS device
	 */
	protected IoSession session;

	/**
	 * Length of the mobile ID/ESN
	 */
	protected Integer mobileIdLength = 0;

	/**
	 * Byte array containing the mobile ID/ESN
	 */
	protected byte[] esnBytes;

	/**
	 * Unit ID of the Garmin device connected to the Calamp unit (if a Garmin device is connected).
	 */
	protected String garminUnitId;

	/**
	 * Flag indicating the handling of Garmin messages.  If true, Garmin messages will be
	 * processed. Otherwise, Garmin messages will be accepted, but the Gramin conent will
	 * not be processed.
	 */
	private boolean garminEnabled;

	/**
	 * Determines if the current raw message is a Garmin message.  If the messages is from a Garmin,
	 * the appropriate ACK or messaging is performed.
	 *
	 * @return <code>true</code> if the message should be sent to the processor, otherwise <code>false</code>
	 */
	protected abstract boolean handleGarminMessage();

	/**
	 * Constructor
	 *
	 * @param container - {@link ReceiverWorkerContainer} used to populate the data members
	 * 		of the worker.
	 * @param container - indicating the handling of Garmin messages. See {@link #garminEnabled}
	 */
	public CalampBaseWorker(ReceiverWorkerContainer container, boolean garminEnabled) {
		super(container);
		this.garminEnabled = garminEnabled;
	}

	/**
	 * Constructor
	 *
	 * @param container - {@link ReceiverWorkerContainer} used to populate the data members
	 * 		of the worker.
	 */
	public CalampBaseWorker(ReceiverWorkerContainer container) {
		this(container, false);
	}

	@Override
	public void run() {

		startTime = System.currentTimeMillis();

		Thread.currentThread().setName("");

		//Utilities.logBinaryMessageAsHex(rawMessage, BinaryMessageType.RECEIVE);

		boolean sendSuccessful = false;
		boolean ackBack = false;

		if(extractESN()) {

			//packetCounterConnection.sendMessage(config.APPLICATION_NAME + "@" + uid);

			// Indicates when to send the message to the processor
			boolean sendMessage = true;

			// Indicates when the exact message has been received previously
			boolean isDuplicate = isMessageDuplicate(rawMessage);

			if(extractHeaderData()){
				boolean isNullMessage = messageType == (byte)0x00 || messageType == (byte)0x01;

				// Don't send the message if it's a duplicate.  However, null messages are
				// all the same.  So, null messages are not treated as other duplicates.
				if(isDuplicate && !isNullMessage) {
					sendMessage = false;
					logger.debug("duplicate message - " + Utilities.toHexString(rawMessage));
				}
				else {

					if(garminEnabled) {
						// Determine if the message is a Garmin message, and if the message should be sent
						// to the processor.
						sendMessage = handleGarminMessage();
					}
					else {
						sendMessage = true;
					}

					if(isNullMessage) {
						// Do not send then message if it's a Null message
						sendMessage = false;
					}

					// Calamp UIDs are all 'long' values.
					if(sendMessage && Utilities.stringHasValue(uid) && Utilities.isLong(uid)){
						logger.info("Sending " + Utilities.toHexString(rawMessage) + " to the Processor");
						sendSuccessful = sendMessageToProcessor(rawMessage, uid, startTime);
					}
					else if(sendMessage) { // only log an invalid UID if the message was supposed to be sent
						//logger.error("UID is invalid: " + uid);
					}
				}

				// Ack back to the LMU if the service type is not Unacknowledged(0) or
				// a response to an ack(2).  Also, the message must have been successfully sent or
				// not intended to be sent.

				ackBack = serviceType != 0 && serviceType != 2 && (sendSuccessful || !sendMessage);
				if(ackBack) {

					//Utilities.currentThreadSleep(config.getProperty(ConfigIntProperty.RECEIVER_ACK_DELAY));
					ack();
				}
				else {
					// for debugging
					logger.info("Calamp message not acked - UID="+ uid + " service_type="+ serviceType + " sendSuccessful="+ sendSuccessful);
				}

				if(!isDuplicate) {
					sendOTA(uid);
				}
			} // tf2
			else {
				logger.error("Failed to obtain the Message ID sequence");
			}
		}//setESN
		else{
			logger.error("Failed to obtain the ESN");
		}
/*
		if(session != null) {
			session.close(false);
			session = null;
		}
*/
		rawMessage = null;

		//statsWorkerTime(startTime, ackBack);

	}//run



	/**
	 * Sends a acknowledgment back to the GPS device confirming receipt of the message.
	 */
	protected void ack() {
		logger.info("in ack");
		try{

      int out_count = 0;
			byte[] out = new byte[10];
			out[out_count++] = (byte)0x02;// svc type ack req resp
			out[out_count++] = (byte)0x01;// msg type ack

			out[out_count++] = seq1;
			out[out_count++] = seq2;
			out[out_count++] = messageType;
			out[out_count++] = (byte)0x00;// seq_2;
			out[out_count++] = (byte)0x00;
			out[out_count++] = (byte)0x00;
			out[out_count++] = (byte)0x00;
			out[out_count++] = (byte)0x00;
			logger.debug("ack=" + Utilities.toHexString(out));

			IoBuffer buffer = IoBuffer.allocate(out.length);
			buffer.put(out);
			buffer.flip();
			//session.write(buffer);

			InetAddress address = InetAddress.getByName("127.0.0.1");
			DatagramPacket packet = new DatagramPacket(out, out.length, address, 4241);
			socket.send(packet);

			buffer.free();
			buffer = null;
			out = null;
			logger.info("ack done");
		}
		catch(Exception e) {
			//logger.error("CalampBaseWorker.ack: " + Utilities.errorHandle(e) );
      logger.error("CalampBaseWorker.ack: ERROR" );
		}
	}//ack

	/**
	 * Populates the data members related to the ESN (or mobile ID).
	 *
	 * @return <code>true</code> if the ESN is successfully set, otherwise <code>false</code>
	 */
	public boolean extractESN() {
		boolean success = false;

		// Need to check the first 2 bytes of the packet to make sure its a Calamp packet
		// We can not check the size, because the size will very based on the Message type.
		if(rawMessage[0] == (byte)0x83 && rawMessage[1] == (byte)0x05){

			boolean mobileIdEnabled = false;
			try {

				// Determine if the mobile id/ESN is set
				mobileIdEnabled = Utilities.isBitSet(rawMessage[0], 0);

				// If the ESN is set, copy the ESN to 'esnBytes' and 'uid'
				if(mobileIdEnabled == true) {
					mobileIdLength = Integer.valueOf(rawMessage[1]);
					esnBytes = Arrays.copyOfRange(rawMessage, 2, mobileIdLength + 2);
					uid = Utilities.toHexString(esnBytes);
					Thread.currentThread().setName(uid +"-"+count);
					success = true;
					
				}
				logger.info("ESN/UID=" + uid);
				logger.info("MobileId=" + Long.parseLong(uid, 16));
			}
			catch(Exception e) {
				success=false;
				logger.error("CalampBaseWorker.setESN: "+  Utilities.errorHandle(e));
			}
		}

		return success;
	}//getESN

	/**
	 * Populates data members related to the message ID.  This data is used to ack back to
	 * the GPS device.
	 *
	 * @return <code>true</code> if the values are successfully set, otherwise <code>false</code>
	 */
	public boolean extractHeaderData() {
		boolean success = false;

		try{
			// Done with header + option stuff
			int pointer = esnBytes.length + 4;

			// Service Type
			// 0 = Unacknowledged Request
			// 1 = Acknowledged Request
			// 2 = Response to an Acknowledged Request
			serviceType = Integer.valueOf(rawMessage[pointer++]);
			messageType = rawMessage[pointer++];
			seq1 = rawMessage[pointer++];
			seq2 = rawMessage[pointer];
			success = true;

      logger.error("CalampBaseWorker.serviceType: "+  serviceType);
      logger.error("CalampBaseWorker.messageType: "+  messageType);
		}
		catch(Exception e) {
			success=false;
			logger.error("CalampBaseWorker.setMessageId: "+  Utilities.errorHandle(e));
		}

		return success;
	}//getMessageId

	/**
	 * Sends a Calamp Type 4 (User) message with the specified user ID message containing the
	 * specified payload.
	 *
	 * @param userMessageId	Calamp user message ID
	 * @param payloadBytes	message content
	 */
	protected void sendUserMessage(byte[] payloadBytes, byte userMessageId) {
		try {
			int index = 0;
			byte[] out = new byte[20 + esnBytes.length + payloadBytes.length];
			out[index++] = (byte)0x83;	// options byte
			out[index++] = (byte)0x05;	// id length

			// append the UID
			for(int i = 0; i < esnBytes.length; i++) {
				out[index++] = esnBytes[i];
			}

			out[index++] = (byte)0x01; // mobile ID type length
			out[index++] = (byte)0x01; // mobile ID type
			out[index++] = (byte)0x01; // service ID
			out[index++] = (byte)0x04; // message type (user message)
			out[index++] = (byte)0x00; // sequence number (byte 1)
			out[index++] = (byte)0x00; // sequence number (byte 2)
			out[index++] = (byte)0x00; // user message route
			out[index++] = userMessageId; // user message ID
			out[index++] = (byte)((payloadBytes.length << 8) & 0xFF);	// message length (byte 1)
			out[index++] = (byte)(payloadBytes.length & 0xFF);			// message length (byte 2)

			// Garmin message
			for(int i = 0; i < payloadBytes.length; i++) {
				out[index++] = payloadBytes[i];
			}

			logger.info("sending to calamp = " + Utilities.toHexString(out));
/*
			IoBuffer buffer = IoBuffer.allocate(out.length);
			buffer.put(out);
			buffer.flip();
			session.write(buffer);
			buffer.free();
			buffer = null;
			out = null;
*/
		}
		catch(Exception e) {
			logger.error("CalampBaseWorker.sendGarminMessage: " + Utilities.errorHandle(e) );
		}
	}
}
