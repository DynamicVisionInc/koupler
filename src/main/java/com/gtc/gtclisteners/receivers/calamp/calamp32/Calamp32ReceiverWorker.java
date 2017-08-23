package com.gtc.gtclisteners.receivers.calamp.calamp32;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import com.gtc.gtclisteners.Config.ConfigIntProperty;
//import com.gtc.gtclisteners.common.MysqlQuery;
import com.gtc.gtclisteners.common.Utilities;
import com.gtc.gtclisteners.common.Utilities.BinaryMessageType;
//import com.gtc.gtclisteners.common.garmin.GarminCache;
//import com.gtc.gtclisteners.common.garmin.GarminDataAccessLayer;
//import com.gtc.gtclisteners.common.garmin.GarminUtilities;
import com.gtc.gtclisteners.receivers.ReceiverWorkerContainer;
import com.gtc.gtclisteners.receivers.calamp.CalampBaseWorker;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.GetItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.regions.Regions;

import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * This class contains methods specific to the Calamp32 worker.  This includes the
 * methods to send over-the-air messages to the GPS device.
 *
 * Geofences
 * Calamp uses a call and response for OTAs (including geofences).  If there are outgoing geofence
 * OTAs, the geofence requests are sorted by operation (create, delete, recycle) and grouped.
 * Each time a message is sent to a device, the message database row ID is cached.  When the unit
 * responds, the corresponding database record is updated to indicate the message was sent.  A
 * {@link CalampGeofenceFlag} object is used to indicate that a unit is in the process of receiving
 * a geofence OTA.  If a unit fails to respond to geofence request, the OTA flag will timeout and
 * reset to <code>false</code>.
 *
 * @author T. Benedict
 * @author J. Stairs
 */
public class Calamp32ReceiverWorker extends CalampBaseWorker implements Runnable {

	/**
	 * Indicates that unit is in the process of a geofence OTA.  The flag is <code>true</code> if
	 * the unit is currently receiving a geofence OTA.
	 */
	private CalampGeofenceFlag geofenceFlag;

	/**
	 * Constructor
	 *
	 * @param message - byte array containing the message from the GPS device.
	 * @param session - the {@link IoSession} used to send and receive messages
	 * @param container - {@link ReceiverWorkerContainer} used to populate the data members
	 * @param container - {@link CalampGeofenceFlag} used to indicate that unit is in the
	 *  		process of a geofence OTA.
	 */
	public Calamp32ReceiverWorker(byte[] message, IoSession session,
			ReceiverWorkerContainer container, CalampGeofenceFlag geofenceFlag, boolean garminEnabled, DatagramSocket socket) {
		super(container, garminEnabled);

		Thread.currentThread().setName("");
		this.rawMessage = message;
		this.session = session;
		this.geofenceFlag = geofenceFlag;
		this.socket = socket;
	} //Calamp32ReceiverWorker

	/**
	 * This method sends over the air (OTA) messages (currently only geofences) to the currently
	 * connected unit.  The database if queried for outgoing geofence OTAs.  There are three types
	 * of geofences requests:
	 * <ul>
	 * <li><b>create</b> creates a new geofence at the specified memory location</li>
	 * <li><b>deletes</b> deletes the geofence at the specified memory location</li>
	 * <li><b>recycle</b> removes all previous requests and clears the entire memory space</li>
	 * </ul>
	 *
	 * Requests are sorted into create operations and delete operations.  The raw request is passed
	 * to a {@link CalampGeofenceStructure} where it is parsed and converted to byte array that
	 * can be added to Calamp byte message.  Delete requests are performed first.  If there are no
	 * delete requests, create requests are performed.  When a request if performed, the
	 * corresponding database row ID is cached.  When the unit responds that request was
	 * successful, the row ID is retrieved from cache and the database is updated.
	 *
	 * @param uid Unit ID of device to which the OTA will be sent
	 *
	 * @return <code>true</code> if the OTA was sent successfully, otherwise <code>false</code>
	 */
	@Override
	protected boolean sendOTA(String uid) {
		
		boolean sendSuccess = true;

		// Only send the OTA if:
		// -the configuration flag 'PERFORM_OTA' is set to true, and
		// -there is pending OTA to be sent, and
		// -the MySQL connection is live, and
		// -the geofence flag is not set OR the geofence flag is set and the message is a geofence response (0x74).
		// The last condition prevents the Receiver from starting a new geofence transaction while one is
		// currently ongoing.  Geofence responses are allowed so that the current transaction can continue.

		String mobileId = String.valueOf(Long.parseLong(uid, 16));
		
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
		.withRegion(Regions.US_EAST_1)
		.build();

		DynamoDB docClient = new DynamoDB(client);

		Table table = docClient.getTable("pulsekit-api-austin-dev-outbound");

		Item item = table.getItem("Id", "a2e7d3e7-51f8-44d3-a691-585e6c675c62");

		//GetItemOutcome outcome = table.getItemOutcome("Id", "a2e7d3e7-51f8-44d3-a691-585e6c675c62");
		System.out.println("OTA Message: \n" + item.toJSONPretty());

/*
		boolean gefenceValid = !geofenceFlag.getFlag() ||
				(geofenceFlag.getFlag() && rawMessage.length >= 58 && rawMessage[50] == 0x74);

		if(checkSendOTA() && gefenceValid) {
			logger.debug("checking database");
			int messageId = 0;
			List<String> otaRowIds = new ArrayList<String>();
			List<String> canceledOtaRowIds = new ArrayList<String>();
			String cachekey = "geofence" + uid;

			// Determine if this is response to a geofence message (0x74).  If so, use the message ID to
			// obtain the row IDs used to construct that message.  Update the OTA table to indicate that
			// the message was successfully delivered.  Also, cancel the OTA timer - the flag will be set
			// to false if there are no more messages to send.
			if(rawMessage.length >= 58 && rawMessage[50] == 0x74) {
				messageId = ((rawMessage[57] & 0xFF) << 8) + (rawMessage[58] & 0xFF) + 1;
				// Ensure that the message is not a response to a 'complete'.
				if(rawMessage[53] != 0x03) {
					otaRowIds = (List<String>) memcachedManager.get(cachekey);
					memcachedManager.delete(cachekey);
					String sqlUpdate = "UPDATE " + databaseName + ".OTA SET sent='1' WHERE id IN ("+ StringUtils.join(otaRowIds, ",") +") ";
					logger.debug(sqlUpdate);
					MysqlQuery.update(mysqlConnection, sqlUpdate, false);
					otaRowIds.clear();
				}
			}

			// These maps are hold the create and delete requests.
			Map<String, String[]> createMessages = new HashMap<String, String[]>();
			Map<String, String[]> deleteMessages = new HashMap<String, String[]>();

			// This flag indicates that there's a recycle request.
			boolean eraseAll = false;

			// Get the out-going messages from the database.
			String sqlQuery = "SELECT id,msg FROM " + databaseName + ".OTA WHERE UID='" + uid +"' AND sent='0' ORDER BY id ASC";
			List<HashMap<String, String>> results = MysqlQuery.queryMultiReturn(mysqlConnection, sqlQuery, false);

			if(results == null || results.isEmpty()) {
				// Message type 0x74 indicates a geofence response.  There are no outgoing messages.
				// Send the 'complete' message.  The last clause (rawMessage[53] == 0x03) ensures
				// that this is not a response to a 'complete' message.  This prevents an infinite
				// loop where the 'complete' request is sent when a 'complete' response is received.
				if(rawMessage.length >= 58 && rawMessage[50] == 0x74 && rawMessage[53] != 0x03) {
					byte[] completeMessage = generateGeofenceCompleteMessage(messageId);
					logger.info("Sending 'complete' message");
					Utilities.logBinaryMessageAsHex(completeMessage, BinaryMessageType.SEND);
					sendBytes(completeMessage);
				}
				geofenceFlag.setFlag(false);
			}
			else { // there are outgoing messages
				// Iterate over the database results.
				for(HashMap<String, String> result : results) {
					// Set the geofence flag true to indicate that a geofence transaction has begun.
					geofenceFlag.setFlag(true);

					// Obtain the row ID and add it the beginning of the geofence request.
					String otaRowId = result.get("id");
					String messageText = otaRowId + "|" + result.get("msg");

					String[] otaParts = messageText.split("\\|");

					// Only send the message if it is a geofence message.
					if(otaParts[1].equals("geofence")) {
						logger.debug("OTA:" + messageText);

						// Ensure the message is not null or empty
						if(StringUtils.isNotBlank(messageText)) {
							if(otaParts[2].equals("recycle")) {
								// Clear all preceding messages.  Capture the row IDs of all
								// preceding requests.  These requests will be marked 'sent'.
								for(String[] createMessage : createMessages.values()) {
									otaRowIds.add(createMessage[0]);
								}
								createMessages.clear();

								for(String[] deleteMessage : deleteMessages.values()) {
									otaRowIds.add(deleteMessage[0]);
								}
								deleteMessages.clear();

								// Add the row ID for the 'recycle' request
								otaRowIds.add(otaParts[0]);

								// Set the flag indicating a recycle event.
								eraseAll = true;
								break;
							}
							else if(otaParts[2].equals("create")) {
								String geofenceId = otaParts[3];
								// If a 'create' is requested, do not add it to the outgoing
								// messages if there is already an outgoing 'delete'.  The 'create'
								// will get picked up on the next round.
								if(deleteMessages.get(geofenceId) == null) {
									createMessages.put(geofenceId, otaParts);
								}
							}
							else if(otaParts[2].equals("delete")) {
								String geofenceId = otaParts[3];
								// If a 'delete' is requested, remove any previous 'create' request
								// for that geofence ID.  These requests cancel eachother out.
								if(createMessages.get(geofenceId) != null) {
									// Add the 'create' RID to canceledOtaRowIds so it will be marked
									// as 'sent' in the database
									canceledOtaRowIds.add(createMessages.get(geofenceId)[0]);
									createMessages.remove(geofenceId);
									// Add the 'delete' RID to canceledOtaRowIds so it will be marked
									// as 'sent' in the database
									canceledOtaRowIds.add(otaParts[0]);
									// Placing these RIDs in the canceledOtaRowIds list, they will be
									// marked 'sent', but they won't be sent to the unit.
								}
								else {
									deleteMessages.put(geofenceId, otaParts);
								}
							}
						}
					}
					else {
						logger.info("OTA is not a geofence message: " + messageText);
					}
				}

				// Set the cancelled messages as 'sent'.
				if(canceledOtaRowIds.size() > 0) {
					String sqlUpdate = "UPDATE " + databaseName + ".OTA SET sent='1' " +
							"WHERE id IN ("+ StringUtils.join(canceledOtaRowIds, ",") +") ";

					logger.debug("Canceled: " + sqlUpdate);
					MysqlQuery.update(mysqlConnection, sqlUpdate, false);
				}

				CalampGeofenceStructure geofenceStructure = new CalampGeofenceStructure();

				// If all messages are to be erased (recycle request), generate and execute the
				// proper SQL statement.
				if(eraseAll) {
					byte[] eraseMessage = generateGeofenceEraseAll();
					logger.info("Sending 'erase' message - erasing all geofences.");
					Utilities.logBinaryMessageAsHex(eraseMessage, BinaryMessageType.SEND);
					sendBytes(eraseMessage);
				}
				else { // proceed with delete and create operations.
					// Process delete operations first.  If there are no more delete operations, perform
					// create operations.
					String[][] orderedEntries = null;
					if(deleteMessages.size() > 0) {
						orderedEntries = deleteMessages.values().toArray(new String[0][]);
					}
					else if(createMessages.size() > 0) {
						orderedEntries = createMessages.values().toArray(new String[0][]);
					}

					// If there are no create or delete operations, don't send any updates.
					if(orderedEntries != null && orderedEntries.length > 0) {
						// Sort based on the starting index (element 4)
						Arrays.sort(orderedEntries, new Comparator<String[]>() {
						    @Override
						    public int compare(String[] str1, String[] str2) {
						    	return compareStringsAsIntegers(str1[4], str2[4]);
						    }
						});

						// Add the messages to the geofence structure.  The message record indexes must be
						// consecutive.  There can be a gap in the record indexes if a geofence was created
						// and then deleted.
						int messageCounter = 0;
						for(String[] entry : orderedEntries) {
							// The Calamp message format allows room for only 3 messages.
							if(messageCounter < 3) {
								// The geofence starting index in the database does not refer to the actual
								// location in memory.  It's necessary to multiply by 20 to get this value.
								int entryIndex = Integer.parseInt(entry[4]) * 20;

								// Ensure that the messages in the geofence structure have consecutive starting
								// indexes.  This entry can be added to the geofence structure if:
								// 1. there are no existing geofences in the geofence structure, or
								// 2. the next consecutive index of the existing geofences in the structure is
								//	  the same as the starting index of this entry.
								if(geofenceStructure.getGeozoneCount() == 0 ||
										geofenceStructure.getNextIndex() == entryIndex) {
									geofenceStructure.parseGeozoneData(entry);
									otaRowIds.add(entry[0]);
									messageCounter++;
								}
							}
						}

						// Send the message to the unit.
						sendSuccess = sendGeofence(geofenceStructure, messageId);
					}
				}

				// If the message was successfully sent, update the database to
				// indicate that the message has been sent.
				if(sendSuccess) {
					memcachedManager.set(cachekey, 60000, otaRowIds);
				}
				else {
					logger.error("Error sending OTA - operation unsuccessful.");
				}
			}
		}
		else if(!gefenceValid) {
			logger.error("Could not send OTA - the geofence flag is false or a geofence transaction " +
					"is currently ongoing.");
			geofenceFlag.setFlag(false);
		}
*/
		return sendSuccess;
	} // processOtaMessage

	@Override
	protected boolean processOtaMessage(String message) {
		// not implemented
		return true;
	} // processOtaMessage

	/**
	 * Returns a properly formatted geofence 'complete' message using the specified message ID.
	 *
	 * @param messageId message ID used in the construction of the message.
	 *
	 * @return a properly formatted geofence 'complete' message
	 */
	private byte[] generateGeofenceCompleteMessage(int messageId) {

		byte[] complete = null;

		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(0x83);			//options byte
			baos.write(mobileIdLength);	//Mobile ID length
			baos.write(esnBytes);		//Mobile ID
			baos.write(0x01);//Mobile ID type length
			baos.write(0x01);//Mobile ID type
			baos.write(0x00);//Service type  1=ack
			baos.write(0x05);//Message type 5=Application Data Message
			baos.write(0x00);//Sequence - Used to identify a specific message
			baos.write(messageId);//Sequence cont'd
			baos.write(0x00);//GeoZone Action Message
			baos.write(0x74);//GeoZone Action Message
			baos.write(0x00);//Application Message Length
			baos.write(0x0a);//Application Message Length
			baos.write(0x03);//Action Code  3=Update Complete
			baos.write(0x00);//Response Code  0=No Error
			baos.write(0x00);//Block Number - memory block where this action is taking place
			baos.write(0x00);//Spare (reserved for future use)
			baos.write(0x00);//Start Index (only used on reads)
			baos.write(0x00);//Start Index cont'd
			baos.write(0x00);//Record Count (only used on reads)
			baos.write(0x00);//Record Count cont'd
			baos.write(0x00);//Major version
			baos.write(0x00);//Minor version
			complete = baos.toByteArray();
			baos.close();
		}
		catch(IOException ioe){
			logger.error("Error preparing the 'update complete' message: "+ Utilities.errorHandle(ioe));
		}

		return complete;
	}

	/**
	 * Returns a properly formatted geofence 'recycle' message.
	 *
	 * @return a properly formatted geofence 'recycle' message
	 */
	private byte[] generateGeofenceEraseAll() {

		byte[] erase = null;

		try {
			// Prepare a Geo-Zone Action Message to erase the Geo-Zone memory space
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(0x83);			//options byte
			baos.write(mobileIdLength);	//Mobile ID length
			baos.write(esnBytes);		//Mobile ID
			baos.write(0x01);//Mobile ID type length
			baos.write(0x01);//Mobile ID type
			baos.write(0x00);//Service type 0=no ack  1=ack
			baos.write(0x05);//Message type 5=Application Data Message
			baos.write(0x00);//Sequence - Used to identify a specific message
			baos.write(0x00); //Sequence cont'd
			baos.write(0x00);//GeoZone Action Message
			baos.write(0x74);//GeoZone Action Message
			baos.write(0x00);//Application Message Length
			baos.write(0x0a);//Application Message Length
			baos.write(0x01);//Action Code  1=Delete
			baos.write(0x00);//Response Code  0=No Error
			baos.write(0x00);//Block Number - memory block where this action is taking place
			baos.write(0x00);//Spare (reserved for future use)
			baos.write(0x00);//Start Index (only used on reads)
			baos.write(0x00);//Start Index cont'd
			baos.write(0x00);//Record Count (only used on reads)
			baos.write(0x00);//Record Count cont'd
			baos.write(0x00);//Major version
			baos.write(0x00);//Minor version
			erase = baos.toByteArray();
			baos.close();
		}
		catch(IOException ioe){
			logger.error("Error erasing the Geo-Zone memory space: "+ Utilities.errorHandle(ioe) );
		}

		return erase;
	}

	/**
	 * Sends the specified geofence message using the specified messge ID.
	 *
	 * @param geofenceStructure geofence data to send
	 * @param messageId integer indicating message order
	 *
 	 * @return <code>true</code> if the geofence was sent successfully, otherwise <code>false</code>
	 */
	private boolean sendGeofence(CalampGeofenceStructure geofenceStructure, int messageId) {
		boolean success = true;

		try {
			int geofenceCount = Integer.valueOf(geofenceStructure.getGeozoneCount());

			// Check that there is geofence data to send
			if(geofenceCount >= 1) {

				logger.debug("geofence count=" + geofenceCount);

				byte[] geofenceMessage = geofenceStructure.getGeozoneData(mobileIdLength, esnBytes, messageId);

				Utilities.logBinaryMessageAsHex(geofenceMessage, BinaryMessageType.SEND);
				sendBytes(geofenceMessage);
			}
		}
		catch(Exception e) {
			Utilities.errorHandle(e);
			success = false;
		}

		return success;
	}//sendGeofence

	/**
	 * Returns <code>true</code> if the message is a Garmin message, otherwise <code>false</code>
	 *
	 * @return <code>true</code> if the message is a Garmin message, otherwise <code>false</code>
	 */
	private boolean isGarminMessage() {
		boolean gramin = false;

		if(rawMessage.length >= 51) {
			byte userMessageId = rawMessage[50];
			// Garmin messages are Type 4 (user messages) with a User Message ID 63.
			if(messageType == (byte)0x04 && userMessageId == (byte)0x63) {
				gramin = true;
			}
		}
		return gramin;
	} // isGarminMessage

	/**
	 * Sends the specified bytes to the current session.
	 *
	 * @param out - bytes to send
	 */
	private void sendBytes(byte[] out){
		try{
			IoBuffer buffer = IoBuffer.allocate(out.length);
			buffer.put(out);
			buffer.flip();
			session.write(buffer);
			buffer.free();
			buffer = null;
		}catch(Exception ioe){
			logger.error("Calamp32ReceiverWorker.sendBytes: "+ Utilities.errorHandle(ioe));
		}
	}//sendBytes

	/**
	 * Determines if the current raw message is a Garmin message.  If the messages is from a Garmin,
	 * the appropriate ACK or messaging is performed.
	 *
	 * @return <code>true</code> if the message should be sent to the processor, otherwise <code>false</code>
	 */
	@Override
	protected boolean handleGarminMessage() {
		boolean sendMessage = true;
/*
		// Attempt to get the Garmin unit ID from Memcached.  If there is no entry in
		// Memcached, then the unit will not have a Garmin connected and Garmin messaging
		// can be ignored.
		garminUnitId = (String)memcachedManager.get(GarminCache.GARMIN_UID + uid);
		if(Utilities.stringHasValue(garminUnitId)) {
			// Check to see if the incoming message is a Garmin message.
			if(isGarminMessage()) {
				logger.debug("serviceType=" + serviceType);
				// Update Memcached with the last time a Garmin message was received by this unit.
				memcachedManager.set(GarminCache.GARMIN_LAST_TIME + uid,
						config.getProperty(ConfigIntProperty.MEMCACHED_KEY_STORE_TIME),
						Calendar.getInstance().getTimeInMillis());

				// Split Garmin messages.  Multiple Garmin messages can be send in a single Calamp message.
				List<byte[]> garminMessages = GarminUtilities.splitMessages(Arrays.copyOfRange(rawMessage, 53, rawMessage.length));
				logger.info("received " + garminMessages.size() + " gramin message(s)");
				for(byte[] garminMessage : garminMessages) {
					logger.info("garmin message: " + Utilities.toHexString(garminMessage));

					// Don't send the message to the processor unless the message is not an Ack.
					sendMessage = false;

					// If the message is not a Garmin Ack, the messge must be Acked.
					// Acks from the Garmin are currently ignored.
					if(isGarminAck(garminMessage)) {
						byte ackedPacketId = garminMessage[3];
						GarminDataAccessLayer.removeOutgoingMessage(garminUnitId, memcachedManager,
								config, ackedPacketId);
					}
					else {
						// Ack back to the Garmin, 0x63 is the user message ID for Garmin
						byte[] garminAck = GarminUtilities.constructAck(garminMessage);
						if(garminAck == null) {
							logger.info("No ack for message - not acking");
						}
						else {
							sendUserMessage(garminAck, (byte)0x63);
							logger.info("acking to Garmin: " + Utilities.toHexString(garminAck));
						}
						sendMessage = true;
					}
				}
			}

			// Check for outgoing messages from the server to the Garmin.  Do not check when the
			// incoming message is a Calamp ACK  (serviceType 2).
			if(serviceType != 2) {
				sendNextOutgoingGarminMessage();
			}
		}
*/
		return sendMessage;
	}

	/**
	 * Returns <code>true</code> if the message is a Garmin ack, otherwise <code>false</code>
	 *
	 * @param garminMessage if the message is a Garmin ack, otherwise <code>false</code>
	 *
	 * @return <code>true</code> if the message is a Garmin ack, otherwise <code>false</code>
	 */
	private boolean isGarminAck(byte[] garminMessage) {
		boolean graminAck = false;

		byte packetId = garminMessage[1];
		// Garmin acks have a packet ID of 0x06.
		if(packetId == (byte)0x06) {
			graminAck = true;
		}

		return graminAck;
	} // isGarminMessage

	/**
	 * Collect all outgoing answer, text, stop, canned, and driver status messages from Memcached
	 * and sends them to the Gramin unit.
	 */
	private void sendNextOutgoingGarminMessage() {
/*
		// Collect all outgoing answer, text, stop, canned, and driver status messages
		byte[] outgoingMessage = GarminDataAccessLayer.getNextOutgoingMessage(garminUnitId, memcachedManager);

//		outgoingMessages.addAll(GarminDataAccessLayer.getOutgoingStopMessages(garminUnitId, memcachedManager));
//		outgoingMessages.addAll(GarminDataAccessLayer.getOutgoingCannedMessages(garminUnitId, memcachedManager));
//		outgoingMessages.addAll(GarminDataAccessLayer.getOutgoingDriverStatusMessages(garminUnitId, memcachedManager));

		// Occasionally, the unit must be pinged to check if the Garmin device is connected.
		// If there's an outgoing message, then the ping is not necessary as the device will
		// respond to the outgoing message.
		if(outgoingMessage == null) {
			// If there is no ping request, getPingRequest will return null.
			outgoingMessage = GarminDataAccessLayer.getPingRequest(garminUnitId, memcachedManager);
		}

		if(outgoingMessage != null) {
			sendUserMessage(outgoingMessage, (byte)0x63);
		}
*/
	}

	/**
	 * TODO
	 */
	private int compareStringsAsIntegers(String str1, String str2) {
		// Convert the strings into integers, and subtract.  If index 1 is greater
    	// than index 2, a positive number will be returned.  If index 1 is less,
    	// a negative number will be returned.
    	int index1 = Integer.parseInt(str1);
    	int index2 = Integer.parseInt(str2);
        return index1 - index2;
	}
}
