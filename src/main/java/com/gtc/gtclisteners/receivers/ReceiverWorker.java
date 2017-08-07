package com.gtc.gtclisteners.receivers;

import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;

//import org.apache.logging.log4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gtc.gtclisteners.Config;
import com.gtc.gtclisteners.Config.ConfigBooleanProperty;
import com.gtc.gtclisteners.Config.ConfigIntProperty;
//import com.gtc.gtclisteners.common.AmqSessionPool;
import com.gtc.gtclisteners.common.Constants;
//import com.gtc.gtclisteners.common.MemcachedManager;
//import com.gtc.gtclisteners.common.MysqlConnection;
//import com.gtc.gtclisteners.common.MysqlQuery;
//import com.gtc.gtclisteners.common.PooledSession;
//import com.gtc.gtclisteners.common.UdpConnection;
import com.gtc.gtclisteners.common.Utilities;

/**
 * Defines common attributes and behaviors for all receiver workers.  Receiver workers
 * receive messages (usually via TCP or UDP) and place the processor via ActiveMQ.  In
 * some cases, the worker must decode some information from the message to send an
 * acknowledgment back to the GPS device or to send an over-the-air update.
 *
 * @author T. Benedict
 * @author J. Stairs
 */
public abstract class ReceiverWorker implements Runnable{

	//protected final Logger logger = Logger.getLogger(this.getClass().getName());
	protected static final Logger logger = LoggerFactory.getLogger(ReceiverWorker.class);

	/**
	 * Application name used to indicate the processor name when sending a message.
	 */
	protected String applicationName;

	/**
	 * Container of configurable parameters
	 */
	protected Config config;

	/**
	 * Used in the thread name to identify the worker thread.
	 */
	protected int count;

	/**
	 * {@link AmqSessionPool} used to send messages to the processors
	 */
	//protected AmqSessionPool sessionPool;

	/**
	 * {@link MysqlConnection} used to interact with the database.
	 */
	//protected MysqlConnection mysqlConnection;

	/**
	 * Manager used to access the Memcached store
	 */
	//protected MemcachedManager memcachedManager;

	/**
	 * Callback used to notify the listener of duplicate messages.
	 */
	//protected DuplicateCallback duplicateCallback;

	/**
	 * Used to time messages.
	 */
	protected long startTime;

	/**
	 * GPS device unit ID
	 */
	protected String uid;

	/**
	 * Website - used to determine whether the data is V3(Fleet) or APUS
	 */
	protected String website;

	/**
	 * Name of the database associated with a particular receiver.<br>
	 */
	protected String databaseName;

	/**
	 * UDP connection used to send packet count to a packet counting application.
	 */
	//protected UdpConnection packetCounterConnection;

	/**
	 * Processes an OTA message and sends the message to the device.<br>
	 *
	 * @param message - message to process and send
	 *
	 * @return <code>true</code> if the message successfully processed and sent, otherwise <code>false</code>
	 */
	protected abstract boolean processOtaMessage(String message);

	/**
	 * Constructor
	 *
	 * @param container - {@link ReceiverWorkerContainer} used to populate the data members
	 * 		of the worker.
	 */
	public ReceiverWorker(ReceiverWorkerContainer container) {
		this.count = container.getCount();
		this.config = container.getConfig();
		//this.sessionPool = container.getAmqSessionQueue();
		//this.mysqlConnection = container.getMysqlConnection();
		this.applicationName = container.getApplicationName();
		//this.packetCounterConnection = container.getPacketCounterConnection();
		//this.memcachedManager = container.getMemcachedManager();
		//this.duplicateCallback = container.getDuplicateCallback();
		this.website = container.getWebsite();
		//this.databaseName = container.getDataName();
		//this.duplicateCallback = container.getDuplicateCallback();
	}

	/**
	 * Sends the total time it took the worker to complete to statsd
	 *
	 * @param sTime - This is the milliseconds from the start of the measurement being done.
	 *
	 */
	public void statsWorkerTime(long sTime){
		statsWorkerTime(sTime, true);
	}

	/**
	 * Sends the total time it took the worker to complete to statsd.  If the <i>inlcudeAckDelay</i>
	 * flag is <code>true</code>, the ack delay is included in the worker time calculation.
	 * Otherwise, the ack delay is not include in the calculation.
	 *
	 * @param sTime - This is the milliseconds from the start of the measurement being done.
	 * @param inlcudeAckDelay - Flag indicating if the ack delay should be inlcuded in the worker time calculation
	 */
	public void statsWorkerTime(long sTime, boolean inlcudeAckDelay){
		long workerTime;
		if(inlcudeAckDelay) {
			//workerTime = (System.currentTimeMillis() - sTime) - config.getProperty(ConfigIntProperty.RECEIVER_ACK_DELAY);
		}
		else {
			workerTime = (System.currentTimeMillis() - sTime);
		}
		//Utilities.sendTimingStat("gtclisteners.Receivers."+ applicationName +".worker_time", workerTime, config);
	}

	/**
	 * Returns the GPS device Unit Id
	 *
	 * @return GPS device Unit Id
	 */
	public String getUid() {
		return this.uid;
	}

	/**
	 * Sends the specified ASCII message to the appropriate processor.
	 *
	 * @param message - message to send to the processor
	 * @param startTime - This is the milliseconds from the function you want to measure
	 *
	 * @return <code>true</code> if the message successfully sent, otherwise <code>false</code>
	 */
	protected boolean sendMessageToProcessor(String message, String uid, Long startTime) {
		return sendMessageObjectToProcessor(message, uid, startTime);
	} //sendMessageToProcessor - ASCII

	/**
	 * Sends the specified binary message to the appropriate processor.
	 *
	 * @param message - message to send to the processor
	 * @param startTime - This is the milliseconds from the function you want to measure
	 *
	 * @return <code>true</code> if the message successfully sent, otherwise <code>false</code>
	 */
	protected boolean sendMessageToProcessor(byte[] message, String uid, Long startTime){
		return sendMessageObjectToProcessor(message, uid, startTime);
	} //sendMessageToProcessor - binary

	private boolean sendMessageObjectToProcessor(Object message, String uid, Long startTime) {

		boolean isSent = false;
/*
		try {
			PooledSession session = null;
			if(sessionPool != null){
				session = sessionPool.borrowSession();
				if(session != null) {
					session.createMessage();
					if(message instanceof String) {
						session.bm_writeString((String)message);
					}
					else if(message instanceof byte[]) {
						session.bm_writeBytes((byte[])message);
					}
					session.bm_setStringProperty("UID", uid);
					session.bm_setLongProperty("start_time", startTime);

					session.bm_setStringProperty("vm_pid", ManagementFactory.getRuntimeMXBean().getName());
					session.bm_setStringProperty("application", "Receivers." + applicationName);

					isSent = session.sendMessage();
					session.closeMessage();
					sessionPool.returnSession(session);
				}
				else{
					logger.error("PoolSession is null.");
					sessionPool.close();
				}
			}
			else{
				logger.error("AmqSessionPool is null");
			}
		}
		catch(Exception e) {
			logger.error("Error in AmqSessionPool");
			Utilities.errorHandle(e);
		}
*/
		isSent = true;
		return isSent;
	}

	/**
	 * Returns <code>true</code> if there is an OTA to be sent and the necessary services are available,
	 * otherwise <code>false</code>.
	 *
	 * @return <code>true</code> if there is an OTA to be sent and the necessary services are available,
	 * 	otherwise <code>false</code>
	 */
	protected boolean checkSendOTA() {
    /*
		List<String> pendingOtaList = (List<String>)memcachedManager.get(databaseName + "_ota_list");
		boolean otaPending = pendingOtaList != null && pendingOtaList.contains(uid);
		boolean sendOTA = config.getProperty(ConfigBooleanProperty.PERFORM_OTA) &&
				otaPending && mysqlConnection.isAlive();

		if(!sendOTA && otaPending) {
			// only show this message if there was an OTA pending, but it was not sent for some other reason.
			logger.error("Could not send OTA - the 'PERFORM_OTA' flag is false OR mysql is down.");
		}
    */

    boolean sendOTA = false;

		return sendOTA;
	}

	/**
	 * Retrieves an out going over-the-air message from the database and sends the message
	 * to a GPS device.
	 *
	 * @return <code>true</code> if the message successfully sent, otherwise <code>false</code>
	 */
	protected boolean sendOTA(String uid) {

		boolean sendSuccess = false;

		if(checkSendOTA()) {
/*
			// Get the out-going message from the database.
			String sqlQuery = "";
			if(website.equals(Constants.V3_WEBSITE)) {
				sqlQuery = "SELECT id,msg from " + databaseName + ".OTA WHERE UID='" + uid +"' AND sent='0' order by time";
			}
			else if(website.equals(Constants.APUS_WEBSITE)) {
				sqlQuery = "SELECT ota_id AS id,message AS msg from " + databaseName + ".ota WHERE SN='" + uid +"' AND sent='0' order by time";
			}

			HashMap<String, String> resultsHash = MysqlQuery.queryLimitOne(mysqlConnection, sqlQuery, false);

			if(resultsHash != null) {
				String message = resultsHash.get("msg");
				String otaId = resultsHash.get("id");

				// Ensure the message is not null or empty
				if(Utilities.stringHasValue(message)) {

					sendSuccess = processOtaMessage(message);

					// If the message was successfully sent, update the database to
					// indicate that the message has been sent.
					if(sendSuccess) {
						String sqlUpdate = "";
						if(website.equals(Constants.V3_WEBSITE)) {
							sqlUpdate = "UPDATE " + databaseName + ".OTA SET sent='1' where id='"+ otaId +"' ";
						}
						else if(website.equals(Constants.APUS_WEBSITE)) {
							sqlUpdate = "UPDATE " + databaseName + ".ota SET sent='1' where ota_id='"+ otaId +"' ";
						}

						MysqlQuery.update(mysqlConnection, sqlUpdate, false);

					}
				}
			}
			else {
				logger.error("Could not send OTA - OTA query results is null.");
			}
      */
		}

		return sendSuccess;
	}//sendOTA

	/**
	 * Determines if the submitted message is a duplicate.  If so, <code>true</code> is returned and the
	 * the message is hashed and stored.  Otherwise, <code>false</code> is returned.
	 *
	 * @return <code>true</code> if the message is a duplicate, otherwise <code>false</code>
	 */
	protected boolean isMessageDuplicate(byte[] rawMessage) {

		boolean isDuplicate = false;
/*
		try {

			// Determine the hash of the message.  The hash is used rather than the
			// entire message to conserve space in memcached.
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			byte[] messagehashbyte = messageDigest.digest(rawMessage);
			String messageHashString = Utilities.toHexString(messagehashbyte);

			// Check if the message hash is in the cache.  If so, then the message
			// is a duplicate.
			if(memcachedManager.get(messageHashString) != null) {
				isDuplicate = true;
				int numberDuplicates = (Integer)memcachedManager.get(messageHashString);
				numberDuplicates++;
				memcachedManager.set(messageHashString, 60000, numberDuplicates);
				duplicateCallback.duplicateMessageFound(uid);
			}
			else { // The message is not in the cache.  Set the message with 0 duplicates.
				memcachedManager.set(messageHashString, 60000, 0);
			}

		}
		catch(Exception e) {
			logger.error("hash error : " + e.getMessage(), e);
		}
*/
		return isDuplicate;
	}//sendOTA
}
