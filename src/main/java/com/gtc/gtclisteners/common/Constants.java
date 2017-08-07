package com.gtc.gtclisteners.common;

/**
 * Contains constant data to be used throughout the system.  All class variables should be
 * publicly accessable.
 */
public final class Constants {

	/**
	 * Private constructor
	 */
	private Constants() {}
	
	public enum EventType {TRAVEL_START, DRIVING, TRAVEL_STOP, RAPID_ACCELERATION, HARD_BRAKE, SUDDEN_STOP, HARD_TURN, OTHER}
	
	/**
	 * Identifier for the V3(Fleet) website
	 */
	public static final String V3_WEBSITE = "V3";
	
	/**
	 * Identifier for the APUS website
	 */
	public static final String APUS_WEBSITE = "APUS";
	
	/**
	 * Data time standard format
	 */
	public static final String MYSQL_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	/**
	 * Data time default value
	 */
	public static final String MYSQL_DEFAULT_TIME = "0000-00-00 00:00:00";
	
	/**
	 * ELog DataQueue API key
	 */
	public static final String ELOG_API_KEY = "4167541396";
}
