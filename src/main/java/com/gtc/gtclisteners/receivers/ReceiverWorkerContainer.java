package com.gtc.gtclisteners.receivers;

import com.gtc.gtclisteners.Config;
//import com.gtc.gtclisteners.common.AmqSessionPool;
//import com.gtc.gtclisteners.common.MemcachedManager;
//import com.gtc.gtclisteners.common.MysqlConnection;
//import com.gtc.gtclisteners.common.UdpConnection;

/**
 * Container class for the parameters passed into the ReceiverWorker constructor
 */
public class ReceiverWorkerContainer {

	/**
	 * See {@link ReceiverWorker#count}
	 */
	private int count;

	/**
	 * See {@link ReceiverWorker#config}
	 */
	private Config config;

	/**
	 * See {@link ReceiverWorker#applicationName}
	 */
	private String applicationName;

	/**
	 * See {@link ReceiverWorker#website}
	 */
	private String website;

	/**
	 * See {@link ReceiverWorker#databaseName}
	 */
	private String databaseName;

	/**
	 * See {@link ReceiverWorker#amqSessionQueue}
	 */
	//private AmqSessionPool amqSessionQueue;

	/**
	 * See {@link ReceiverWorker#mysqlConnection}
	 */
	//private MysqlConnection mysqlConnection;

	/**
	 * Manager used to access the Memcached store
	 */
	//private MemcachedManager memcachedManager;

	/**
	 * See {@link ReceiverWorker#packetCounterConnection}
	 */
	//private UdpConnection packetCounterConnection;

	/**
	 * Callback used to notify the listener of duplicate messages.
	 */
	//private DuplicateCallback duplicateCallback;

	/**
	 * @return the count
	 */
	public int getCount() {
		return count;
	}

	/**
	 * @param count the count to set
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * @return the config
	 */
	public Config getConfig() {
		return config;
	}

	/**
	 * @param config the config to set
	 */
	public void setConfig(Config config) {
		this.config = config;
	}

	/**
	 * @return the applicationName
	 */
	public String getApplicationName() {
		return applicationName;
	}

	/**
	 * @param applicationName the applicationName to set
	 */
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * @return the website
	 */
	public String getWebsite() {
		return website;
	}

	/**
	 * @param website the website to set
	 */
	public void setWebsite(String website) {
		this.website = website;
	}

	/**
	 * @return the databaseName
	 */
	public String getDataName() {
		return databaseName;
	}

	/**
	 * @param databaseName the databaseName to set
	 */
	public void setDataName(String databaseName) {
		this.databaseName = databaseName;
	}

	/**
	 * @return the amqSessionQueue
	 */
	//public AmqSessionPool getAmqSessionQueue() {
		//return amqSessionQueue;
	//}

	/**
	 * @param amqSessionQueue the amqSessionQueue to set
	 */
	//public void setAmqSessionQueue(AmqSessionPool amqSessionQueue) {
		//this.amqSessionQueue = amqSessionQueue;
	//}

	/**
	 * @return the mysqlConnection
	 */
	//public MysqlConnection getMysqlConnection() {
		//return mysqlConnection;
	//}

	/**
	 * @param mysqlConnection the mysqlConnection to set
	 */
	//public void setMysqlConnection(MysqlConnection mysqlConnection) {
		//this.mysqlConnection = mysqlConnection;
	//}

	/**
	 * @return the memcachedManager
	 */
	//public MemcachedManager getMemcachedManager() {
		//return memcachedManager;
	//}

	/**
	 * @param memcachedManager the memcachedManager to set
	 */
	//public void setMemcachedManager(MemcachedManager memcachedManager) {
		//this.memcachedManager = memcachedManager;
	//}

	/**
	 * @return the packetCounterConnection
	 */
	//public UdpConnection getPacketCounterConnection() {
		//return packetCounterConnection;
	//}

	/**
	 * @param packetCounterConnection the packetCounterConnection to set
	 */
	//public void setPacketCounterConnection(UdpConnection packetCounterConnection) {
		//this.packetCounterConnection = packetCounterConnection;
	//}

	/**
	 * @return the duplicateCallback
	 */
	//public DuplicateCallback getDuplicateCallback() {
		//return duplicateCallback;
	//}

	/**
	 * @param duplicateCallback the duplicateCallback to set
	 */
	//public void setDuplicateCallback(DuplicateCallback duplicateCallback) {
		//this.duplicateCallback = duplicateCallback;
	//}
}
