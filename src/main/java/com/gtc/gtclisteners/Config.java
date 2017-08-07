package com.gtc.gtclisteners;

import java.io.File;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
/*
import org.apache.commons.configuration.AbstractConfiguration;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.DynamicURLConfiguration;
*/
/**
 * Configuration class that holds all configurable properties for the GTC Listeners system.<br>
 * <br>
 * This class implements the Netflix Archaius configuration tool.  When this class is instantiated,
 * it consults a configuration file.  This configuration file is polled at regular intervals.  If
 * the file is changed, the configuration will be appropriately updated the next time the file is polled.<br>
 * <br>
 * All configurable properties are defined in several enumerations:<br>
 * <ul>
 * <li>ConfigStringProperty - lists all properties of type <i>String</i></li>
 * <li>ConfigIntProperty - lists all properties of type <i>int</i></li>
 * <li>ConfigBooleanProperty - lists all properties of type <i>boolean</i></li>
 * </ul>
 * <br>
 * The configurable properties are accessed via 'getter' methods.
 */
public class Config {

	/**
	 * Time (in milliseconds) to wait before initially polling the configuration file.
	 */
	private static final int INITIAL_DELAY_MILLIS = 0;

	/**
	 * Time (in milliseconds) to wait when polling the configuration file.
	 */
	private static final int FIXED_DELAY_MILLIS = 60000;

	/**
	 * Flag indicating if the Configuration should ignore properties that are removed
	 * from the configuration file.
	 */
	private static final boolean IGNORE_DELETES_FROM_SOURCE = true;

	/**
	 * Delimiter used to separate entries in the config file with multiple values
	 */
	private static final char CONFIG_FILE_DELIMITER = '~';

	/**
	 * Name of the component system
	 */
	public final String SYSTEM_NAME;

	/**
	 * Name of the component application
	 */
	public final String APPLICATION_NAME;

	/**
	 * Map where the key is a {@link ConfigStringProperty}, and the value is the
	 * corresponding {@link DynamicStringProperty}.
	 */
	//private Map<ConfigStringProperty, DynamicStringProperty> stringPropertiesMap =
			//new HashMap<ConfigStringProperty, DynamicStringProperty>();

	/**
	 * Map where the key is a {@link ConfigIntProperty}, and the value is the
	 * corresponding {@link DynamicStringProperty}.
	 */
	//private Map<ConfigIntProperty, DynamicIntProperty> integerPropertiesMap =
			//new HashMap<ConfigIntProperty, DynamicIntProperty>();

	/**
	 * Map where the key is a {@link ConfigBooleanProperty}, and the value is the
	 * corresponding {@link DynamicStringProperty}.
	 */
	//private Map<ConfigBooleanProperty, DynamicBooleanProperty> booleanPropertiesMap =
			//new HashMap<ConfigBooleanProperty, DynamicBooleanProperty>();

	/**
	 * Enumeration that encapsulates the levels of scope.<br>
	 * <br>
	 * <ul>
	 * <li>{@link #GLOBAL}</li>
	 * <li>{@link #SYSTEM}</li>
	 * <li>{@link #APPLICATION}</li>
	 * </ul>
	 */
	private enum Scope {

		/**
		 * Applies globally to all applications.
		 */
		GLOBAL,

		/**
		 * Applies to all applications within the same system (e.g. Receivers, Processors, RGeo, etc).
		 */
		SYSTEM,

		/**
		 * Applies only to the specified application (e.g. Calamp8Receiver, WebtechProcessor, etc).
		 */
		APPLICATION
	}


	/**
	 * Enumeration listing the configurable properties of type <i>String</i>.
	 */
	public enum ConfigStringProperty {

		/**
		 * URL of the ActiveMQ server.
		 */
		ACTIVE_MQ_URL("amqUrl", Scope.GLOBAL),

		/**
		 * URL of the External ActiveMQ server.
		 */
		EXTERNAL_ACTIVE_MQ_URL("externalAmqUrl", Scope.GLOBAL),

		/**
		 * URL of the MySQL read/write server.
		 */
		MYSQL_RW_SERVER_URL("mysqlRwServerUrl", Scope.GLOBAL),

		/**
		 * URL of the MySQL read only server.
		 */
		MYSQL_RO_SERVER_URL("mysqlRoServerUrl", Scope.GLOBAL),

		/**
		 * URL of the Memcached server.
		 */
		MEMCACHED_URL("memcachedUrl", Scope.GLOBAL),

		/**
		 * The MySQL user name specific to the application
		 */
		MYSQL_USERNAME("databaseUsername", Scope.APPLICATION),

		/**
		 * The password for the application.
		 */
		MYSQL_PASSWORD("databasePassword", Scope.APPLICATION),

		/**
		 * URL of the poller gateway
		 */
		POLLER_GATEWAY_URL("pollerGateway", Scope.APPLICATION),

		/**
		 * Poller gateway login for the first SkyWave account
		 */
		POLLER_LOGIN1("pollerLogin1", Scope.APPLICATION),

		/**
		 * Poller gateway login for the second SkyWave account
		 */
		POLLER_LOGIN2("pollerLogin2", Scope.APPLICATION),

		/**
		 * Poller gateway password for the first SkyWave account
		 */
		POLLER_PASSWORD1("pollerPassword1", Scope.APPLICATION),

		/**
		 * Poller gateway password for the second SkyWave account
		 */
		POLLER_PASSWORD2("pollerPassword2", Scope.APPLICATION),

		/**
		 * URL of the primary reverse geocoding server.
		 */
		RGEO_URL_1("rgeoUrl1", Scope.APPLICATION),

		/**
		 * URL of the secondary reverse geocoding server.
		 */
		RGEO_URL_2("rgeoUrl2", Scope.APPLICATION),

		/**
		 * URL of the speed limit server.
		 */
		RGEO_SPEED_LIMIT_URL("rgeoSpeedLimitUrl", Scope.APPLICATION),

		/**
		 * EMail address to send the alert to.
		 */
		MONITOR_ALERT_EMAIL("monitorAlertEmail", Scope.SYSTEM),

		/**
		 * IP address of the error reporting TCP listening program
		 */
		ERROR_REPORTER_URL("errorTcpUrl", Scope.SYSTEM),

		/**
		 * List of email recipients that receive error reports
		 */
		ERROR_REPORTER_RECIPIENTS("errorReporterRecipients", Scope.SYSTEM),

		/**
		 * List of email recipients that receive daily unit message count reports
		 */
		DAILY_TOTAL_RECIPIENTS("dailyTotalRecipients", Scope.SYSTEM),

		/**
		 * List of email recipients that receive daily spike and duplicate reports
		 */
		DAILY_SPIKE_RECIPIENTS("dailySpikeRecipients", Scope.SYSTEM),

		/**
		 * URL of the stats reporting server.
		 */
		STATS_REPORTER_URL("statsReporterUrl", Scope.SYSTEM),

		/**
		 * URL of the packet counter server.
		 */
		PACKET_COUNTER_LISTENER_URL("packetCounterListenerUrl", Scope.GLOBAL),

		/**
		 * URL of the packet counter server.
		 */
		DROPPED_PACKET_LISTENER_URL("droppedPacketListenerUrl", Scope.GLOBAL),

		/**
		 * URL of the Redis server.
		 */
		REDIS_URL("redisUrl", Scope.GLOBAL),

		/**
		 * URL of the MySQL read only server.
		 */
		SKYWAVE_ALERT_URL("SkywaveAlertURL", Scope.GLOBAL),

		/**
		 * URL of the DataQueue web service.
		 */
		DATA_QUEUE_WEB_SERVICE_URL("webServiceUrl", Scope.SYSTEM),

		/**
		 * URL of a listener web service.
		 */
		WEB_SERVICE_URL("webServiceUrl", Scope.APPLICATION);

		/**
		 * String that acts as unique identifier for the configuration property
		 */
		private String key;

		/**
		 * {@link Scope} if the configuration property
		 */
		private Scope scope;

		/**
		 * enumeration constructor
		 *
		 * @param {@link #key} associated with a configuration property
 		 * @param {@link #scope} associated with a configuration property
		 */
		private ConfigStringProperty(String key, Scope scope) {
			this.key = key;
			this.scope = scope;
		}

		/**
		 * returns the {@link #key}
		 */
		public String getKey() {
			return key;
		}

		/**
		 * returns the {@link #scope}
		 */
		public Scope getScope() {
			return scope;
		}
	}

	/**
	 * Enumeration listing the configurable properties of type <i>int</i>.
	 */
	public enum ConfigIntProperty {

		/**
		 * Active MQ queue prefetch
		 */
		ACTIVE_MQ_QUEUE_PREFETCH("amqQueuePrefetch", Scope.GLOBAL),

		/**
		 * Active MQ topic prefetch
		 */
		ACTIVE_MQ_TOPIC_PREFETCH("amqTopicPrefetch", Scope.GLOBAL),

		/**
		 * Active MQ redelivery policy - maximum number of redeliveries
		 */
		ACTIVE_MQ_MAXIMUM_REDELIVERIES("amqMaximumRedeliveries", Scope.GLOBAL),


		/**
		 * External Active MQ queue prefetch
		 */
		EXTERNAL_ACTIVE_MQ_QUEUE_PREFETCH("externalAmqQueuePrefetch", Scope.GLOBAL),

		/**
		 * External Active MQ topic prefetch
		 */
		EXTERNAL_ACTIVE_MQ_TOPIC_PREFETCH("externalAmqTopicPrefetch", Scope.GLOBAL),

		/**
		 * External Active MQ redelivery policy - maximum number of redeliveries
		 */
		EXTERNAL_ACTIVE_MQ_MAXIMUM_REDELIVERIES("externalAmqMaximumRedeliveries", Scope.GLOBAL),


		/**
		 * Time (in milliseconds) between heartbeat 'beats'.
		 */
		HEARTBEAT_SPEED("heartbeatSpeed", Scope.GLOBAL),

		/**
		 * Memcached server port
		 */
		MEMCACHED_PORT("memcachedPort", Scope.GLOBAL),

		/**
		 * For Memcached, the time (in seconds) to retain entries.<br>
		 * See http://dustin.github.com/java-memcached-client/apidocs/net/spy/memcached/MemcachedClient.html#add(java.lang.String, int, java.lang.Object)
		 */
		MEMCACHED_KEY_STORE_TIME("memcachedKeyStoreTime", Scope.GLOBAL),

		/**
		 * For receivers, the network port to listen on for incoming messages
		 */
		RECIEVER_PORT("receiverPort", Scope.APPLICATION),

		/**
		 * For receivers, the number of PooledSession objects to start with.
		 */
		RECEIVER_SESSION_COUNT("receiverSessionCount", Scope.APPLICATION),

		/**
		 * For receiverd, the time to wait (in milliseconds) from when an a message is
		 * received to acknowledge the message.
		 */
		RECEIVER_ACK_DELAY("receiverAckDelay", Scope.APPLICATION),

		/**
		 * Amount of time to pause between queries to the data source.
		 */
		POLLER_POLL_TIME("pollerPollTime", Scope.APPLICATION),

		/**
		 * For TCP receivers, the TCP socket timeout
		 */
		TCP_SOCKET_TIMEOUT("tcpSocketTimeout", Scope.APPLICATION),

		/**
		 * For TCP receivers, the TCP buffer size.
		 */
		TCP_RECEIVER_BUFFER_SIZE("tcpReceiveBufferSize", Scope.APPLICATION),

		/**
		 * For RGeo, the time to wait to send another request after receiving
		 * an 'over limit' error from the data source
		 */
		RGEO_OVER_LIMIT_DELAY("rgeoOverLimitDelay", Scope.APPLICATION),

		/**
		 * Port of the error reporting TCP listening program
		 */
		ERROR_TCP_PORT("errorTcpPort", Scope.SYSTEM),

		/**
		 * Timeout duration (in milliseconds) for the error reporting TCP Socket<br>
		 * See {@link Socket#setSoTimeout}
		 */
		ERROR_TCP_SOCKET_TIMEOUT("errorTcpSocketTimeout", Scope.SYSTEM),

		/**
		 * Port of the heartbeat store server
		 */
		HEARTBEAT_STORE_PORT("heartbeatStorePort", Scope.SYSTEM),

		/**
		 * Timeout of the heartbeat store server
		 */
		HEARTBEAT_STORE_SOCKET_TIMEOUT("heartbeatStoreSocketTimeout", Scope.SYSTEM),

		/**
 		 * Port of the real time maps server
		 */
		REAL_TIME_MAPS_PORT("realTimeMapsPort", Scope.SYSTEM),

		/**
 		 * Port of the stats server
		 */
		STATS_REPORTER_PORT("statsReporterPort", Scope.SYSTEM),

		/**
		 * Port of the packet counter server.
		 */
		PACKET_COUNTER_LISTENER_PORT("packetCounterListenerPort", Scope.GLOBAL),

		/**
		 * Port of the dropped packet server.
		 */
		DROPPED_PACKET_LISTENER_PORT("droppedPacketListenerPort", Scope.GLOBAL),

		/**
		 * Time (in milliseconds) that a processed message will wait for RGeo and landmark
		 * lookup before inserting into the database.
		 */
		DATA_QUEUE_MESSAGE_CONSOLIDATION_WAIT_TIME("consolidationWaitTime", Scope.SYSTEM),

		/**
		 * Port of the DataQueue web service.
		 */
		DATA_QUEUE_WEB_SERVICE_PORT("webServicePort", Scope.SYSTEM),

		/**
		 * Maximum number of message returned by the web service on a single call.
		 */
		DATA_QUEUE_WEB_SERVICE_MAX_MESSAGES_RETURNED("webServiceMaxMessagesReturned", Scope.SYSTEM),

		/**
		 * Port of a listener web service.
		 */
		WEB_SERVICE_PORT("webServicePort", Scope.APPLICATION);

		/**
		 * String that acts as unique identifier for the configuration property
		 */
		private String key;

		/**
		 * {@link Scope} if the configuration property
		 */
		private Scope scope;

		/**
		 * enumeration constructor
		 *
		 * @param {@link #key} associated with a configuration property
 		 * @param {@link #scope} associated with a configuration property
		 */
		private ConfigIntProperty(String key, Scope scope) {
			this.key = key;
			this.scope = scope;
		}

		/**
		 * returns the {@link #key}
		 */
		public String getKey() {
			return key;
		}

		/**
		 * returns the {@link #scope}
		 */
		public Scope getScope() {
			return scope;
		}
	}

	/**
	 * Enumeration listing the configurable properties of type <i>boolean</i>.
	 */
	public enum ConfigBooleanProperty {

		/**
		 * For TCP receivers, the TCP keep alive time
		 */
		TCP_KEEP_ALIVE("tcpKeepAlive", Scope.APPLICATION),

		/**
		 * Flag which, if <code>true</code>, makes Google RGeo use Memcached
		 */
		GOOGLE_USE_MEMCACHED("googleUseMemcached", Scope.APPLICATION),

		/**
		 * Flag which, if <code>true</code>, makes Google RGeo use the speed limit database
		 */
		GOOGLE_USE_SPEED_LIMIT_DB("googleUseSpeedLimit", Scope.APPLICATION),

		/**
		 * Flag which, if <code>true</code>, makes MysqlQuery objects use the read-only(RO) server.
		 * Otherwise, MysqlQuery objects us the read/write(RW) server
		 */
		MYSQLQUERY_USE_RO_SERVER("mysqlQueryUseRoServer", Scope.GLOBAL),

		/**
		 * Enable error reporting.  If <code>true</code>, the system will send out error reports.
		 */
		REPORT_ERRORS("reportErrors", Scope.GLOBAL),

		/**
		 * Flag which, if <code>true</code>, causes the application to include debug statements in
		 * the log.
		 */
		LOG_DEBUG("logDebug", Scope.GLOBAL),

		/**
		 * Enable stats reporting.  If <code>true</code>, the system will report stats.
		 */
		REPORT_STATS("reportStats", Scope.APPLICATION),

		/**
		 * Enable real time maps reporting.  If <code>true</code>, the system will report data to real time maps.
		 */
		REPORT_REAL_TIME_MAPS("reportRealTimeMaps", Scope.APPLICATION),

		/**
		 * Enable receiver OTA.  If <code>true</code>, the receiver will send OTA messages.
		 */
		PERFORM_OTA("performOTA", Scope.APPLICATION),

		/**
		 * Flag which, if <code>true</code>, makes the Webtech UDP receiver process 'TO' messages.
		 * If <code>true</code>, the Webtech UDP receiver will process 'TO' messages.  Otherwise, 'TO'
		 * messages will be ignored.
		 */
		PROCESS_TO_MESSAGES("processTOMessages", Scope.APPLICATION);


		/**
		 * String that acts as unique identifier for the configuration property
		 */
		private String key;

		/**
		 * {@link Scope} if the configuration property
		 */
		private Scope scope;

		/**
		 * enumeration constructor
		 *
		 * @param {@link #key} associated with a configuration property
 		 * @param {@link #scope} associated with a configuration property
		 */
		private ConfigBooleanProperty(String key, Scope scope) {
			this.key = key;
			this.scope = scope;
		}

		/**
		 * returns the {@link #key}
		 */
		public String getKey() {
			return key;
		}

		/**
		 * returns the {@link #scope}
		 */
		public Scope getScope() {
			return scope;
		}
	}
/*
	static {
		// Configuration parameters can consist of lists of values.  The default list delimiter is the comma(,).
		// Comma is currently used in some entries that are not lists.  So, the delimiter is changed.
		DynamicURLConfiguration.setDefaultListDelimiter(CONFIG_FILE_DELIMITER);
		AbstractConfiguration configuration = new DynamicURLConfiguration(
				INITIAL_DELAY_MILLIS, FIXED_DELAY_MILLIS, IGNORE_DELETES_FROM_SOURCE, setConfigUrl());
		DynamicPropertyFactory.initWithConfigurationSource(configuration);
	}
*/
	/**
	 * constructor
	 *
	 * @param systemName - name of the application's system (e.g. Receivers, Processors, etc.)
	 * @param applicationName - name of the application (e.g. Calamp8, WebtechProcessor, etc)
	 */
	public Config(String systemName, String applicationName) {
		this.SYSTEM_NAME = systemName;
		this.APPLICATION_NAME = applicationName;
/*
		// Populate the String map
		for(ConfigStringProperty stringPropertyKey : ConfigStringProperty.values()) {
			String keyString = composeKey(stringPropertyKey.getScope(), stringPropertyKey.getKey());
			DynamicStringProperty property = DynamicPropertyFactory.getInstance().getStringProperty(keyString, "");
			stringPropertiesMap.put(stringPropertyKey, property);
		}

		// Populate the Integer map
		for(ConfigIntProperty integerPropertyKey : ConfigIntProperty.values()) {
			String keyString = composeKey(integerPropertyKey.getScope(), integerPropertyKey.getKey());
			DynamicIntProperty property = DynamicPropertyFactory.getInstance().getIntProperty(keyString, 0);
			integerPropertiesMap.put(integerPropertyKey, property);
		}

		// Populate the Boolean map
		for(ConfigBooleanProperty booleanPropertyKey : ConfigBooleanProperty.values()) {
			String keyString = composeKey(booleanPropertyKey.getScope(), booleanPropertyKey.getKey());
			DynamicBooleanProperty property = DynamicPropertyFactory.getInstance().getBooleanProperty(keyString, false);
			booleanPropertiesMap.put(booleanPropertyKey, property);
		}
*/
	}

	/**
	 * constructor
	 *
	 * @param systemName - name of the application's system (e.g. Receivers, Processors, etc.)
	 */
	public Config(String systemName) {
		this(systemName, "");
	}

	/**
	 * It checks to see if file live.server in resources exists, and returns the correct URL for Arc
	 */
	private static String setConfigUrl(){
		String configUrl = "";

		File live = new File("../resources/live.server");
		File aws = new File("../resources/aws.server");
		if (aws.exists()) {
			if (live.exists()) {
				configUrl = "http://amq.fleettrackit.com/settings/config.php";
			}
			else {
				// TODO set when the AWS staging environment is ready
				configUrl = "";
			}

		}
		else {
			if (live.exists()) {
				configUrl = "http://amq.gpstrackit.net/settings/config.php";
			}
			else {
				configUrl = "http://192.168.3.245/settings/staging.php";
			}
		}
		return configUrl;
	}//setConfigUrl

	/**
	 * Returns the value of the property with the specified key.
	 *
	 * @param propertyKey - unique identifier for the property
	 */
	//public String getProperty(ConfigStringProperty propertyKey) {
		//return this.stringPropertiesMap.get(propertyKey).get();
	//}

	/**
	 * Returns the value of the property with the specified key.
	 *
	 * @param propertyKey - unique identifier for the property
	 */
	//public int getProperty(ConfigIntProperty propertyKey) {
		//return this.integerPropertiesMap.get(propertyKey).get();
	//}

	/**
	 * Returns the value of the property with the specified key.
	 *
	 * @param propertyKey - unique identifier for the property
	 */
	//public boolean getProperty(ConfigBooleanProperty propertyKey) {
		//return this.booleanPropertiesMap.get(propertyKey).get();
	//}

	/**
	 * Returns a well formatted String consisting of the system name, application name,
	 * and property key.
	 *
	 * @param scope - {@link Scope} of the property
	 * @param key - String that acts as unique identifier for the property
	 */
	private String composeKey(Scope scope, String key) {
		String keyString = "";
		if(scope == Scope.GLOBAL) {
			keyString = key;
		}
		else if(scope == Scope.SYSTEM) {
			keyString = SYSTEM_NAME + "." + key;
		}
		else if(scope == Scope.APPLICATION) {
			keyString = SYSTEM_NAME + "." + APPLICATION_NAME + "." + key;
		}

		return keyString;
	}
}
