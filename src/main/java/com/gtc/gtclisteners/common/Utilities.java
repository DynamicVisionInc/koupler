package com.gtc.gtclisteners.common;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Base64;


import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

//import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gtc.gtclisteners.Config;
import com.gtc.gtclisteners.Config.ConfigBooleanProperty;
//import com.gtc.gtclisteners.common.errorreporting.ErrorReport;
//import com.gtc.gtclisteners.common.errorreporting.ErrorReporter;
//import com.gtc.gtclisteners.common.stats.StatsReporter;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
/**
 * This class contains general purpose methods that are used throughout the system.
 * All methods are static.
 *
 * @author T. Benedict
 * @author J. Stairs
 */
public class Utilities {

	/**
	 * Types of binary messages:
	 *
	 * <ul>
	 * <li>{@link #SEND}</li>
	 * <li>{@link #RECEIVE}</li>
	 * <li>{@link #DATA}</li>
	 * </ul>
	 */
	public enum BinaryMessageType {
		/**
		 * {@link BinaryMessageType} to be sent.
		 */
		SEND,

		/**
		 * {@link BinaryMessageType} that has been received.
		 */
		RECEIVE,

		/**
		 * {@link BinaryMessageType} containing data.
		 */
		DATA
	}

	//private static final Logger logger = Logger.getLogger(Utilities.class);

	private static final Logger logger = LoggerFactory.getLogger(Utilities.class);

	/**
	 * private constructor
	 */
	private Utilities() {}

	/**
	 * Returns a string containing the hex representation of an array of bytes.
	 *
	 * @param byteArray - array of bytes to be converted to a hex string
	 *
	 * @return string containing the hex representation of the specified byte array
	 */
	public static String toHexString(byte[] byteArray) {
		String hexString = "";

		try{

			if(byteArray.length >= 1){
				byte oneByte = 0x0;
				String hexByte = "";
				int stopIndex = byteArray.length;

				for(int counter = 0; counter < stopIndex; counter++)
				{
					// Get one byte
					oneByte = byteArray[counter];

					// In java a byte is sign-extended to 32 bits.  The 0xFF mask is
					// is applied to effectively undo the sign extension.
					hexByte = java.lang.Integer.toHexString((oneByte & 0xFF));

					// If the hex representation of the byte is a single digit,
					// prefix the hex representation with a '0'.
					if(hexByte.length() == 1)
					{
						hexByte = "0" + hexByte;
					}

					hexString += hexByte + "";
				}
			}

		}catch(Exception e){
			logger.error("Utilities.toHexString: "+  Utilities.errorHandle(e));
		}

		return hexString;
	}

	/**
	 * Sends a hex representation of the specified byte array to the debug log.
	 *
	 * The {@link BinaryMessageType} is specified.  The log entry will be colored
	 * based on the the type of message:
	 *
	 * <ul>
	 * <li>RECEIVE - green</li>
	 * <li>SEND - purple</li>
	 * <li>DATA - red</li>
	 * </ul>
	 *
	 * @param binaryMessage - array of bytes to be logged as a hex string
	 * @param binaryMessageType - array of bytes to be converted to a hex string
	 */
	public static void logBinaryMessageAsHex(byte[] binaryMessage, BinaryMessageType binaryMessageType){

			String hexString = toHexString(binaryMessage);

			if(binaryMessageType == BinaryMessageType.RECEIVE){
				logger.info("Receive: "+ hexString +"");//green
			}
			else if(binaryMessageType == BinaryMessageType.SEND){
				logger.info("Send: "+ hexString +"");//purple
			}
			else if(binaryMessageType == BinaryMessageType.DATA){
				logger.info("Data: "+ hexString +"");//red
			}
			else {
				logger.info(hexString);
			}


	}//logBinaryMessageAsHex

	/**
	 * Returns <code>true</code> if the specified latitude and longitude strings
	 * represent valid latitude and longitude values, respectively.
	 *
	 * @param lat - latitude string to be checked
	 * @param lon - longitude string to be checked
	 *
	 * @return <code>true</code> if the specified latitude and longitude strings represent values, otherwise <code>false</code>.
	 */
	public static boolean checkLatLong(String lat, String lon) {
		boolean success = false;

		if(stringHasValue(lat) && stringHasValue(lon)) {

			try {
				double latDouble = Double.valueOf(lat);
				double lonDouble = Double.valueOf(lon);
				success = latDouble >= -90.0 && latDouble <= 90.0 &&
						  lonDouble >= -180.0 && lonDouble <= 180.0;

				// Origin coords (0.0, 0.0) is considered a reporting error
				if(latDouble == 0.0 && lonDouble == 0.0) {
					success = false;
				}
			}
			catch(Exception e) {
				logger.error("Utilities.checkLatLong: "+  Utilities.errorHandle(e));
			}
		}

		return success;
	}//checkLatLong

	/**
	 * Returns a byte array equivalent to the hex value represented in the passed string.
	 *
	 * @param hexString - string representing the hex value to be converted
	 *
	 * @return byte array equivalent to the hex value represented in the passed string.
	 */
	public static byte[] hexStringToByteArray(String hexString) {

		// If the specified hex string is an odd length, add a leading '0'
		if(hexString.length() % 2 != 0) {
			hexString = "0"+hexString;
		}

		int len = hexString.length();
	    byte[] data = new byte[len / 2];

		try{
		    for (int i = 0; i < len; i += 2) {
		        data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
		                             + Character.digit(hexString.charAt(i+1), 16));
		    }
		}catch(Exception e){
			logger.error("Utilities.hexStringToByteArray: " +  e);
			logger.error("Utilities.hexStringToByteArray - hex string: " +  hexString);
			Utilities.printStackTrace(e.getStackTrace());
		}

	    return data;
	}

	/**
	 * Puts the current thread to sleep for the specified number of milliseconds.
	 *
	 * @param milliseconds - milliseconds the current thread will sleep.
	 */
	public static void currentThreadSleep(long milliseconds) {
		try{
			Thread.currentThread();
			Thread.sleep(milliseconds);
		}
		catch(InterruptedException e) {
			logger.error("Utilities.currentThreadSleep: "+  Utilities.errorHandle(e));
		}
	} // currentThreadSleep

	/**
	 * Sends messages submitted to <code>System.out</code> to the output log and messages to
	 * <code>System.err</code> to the error log.
	 */
    public static void tieSystemOutAndErrToLog() {
        System.setOut(createLoggingProxy(System.out));
        System.setErr(createLoggingProxy(System.err));
    }

    /**
	 * Sends messages submitted to <code>System.out</code> to the output log and messages to
	 * <code>System.err</code> to the error log.
	 */
    public static void tieSystemOutAndErrToLog(Logger logger) {
    	System.setOut(createLoggingProxy(System.out, logger));
      System.setErr(createLoggingProxy(System.err, logger));
    }

    /**
	 * Used by tieSystemOutAndErrToLog.
	 */
    private static PrintStream createLoggingProxy(final PrintStream realPrintStream, final Logger logger) {
        return new PrintStream(realPrintStream) {
            public void print(final String string) {
                realPrintStream.print(string);
                logger.info(string);
            }
        };
    }

	/**
	 * Used by tieSystemOutAndErrToLog.
	 */
    private static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            public void print(final String string) {
                realPrintStream.print(string);
                logger.info(string);
            }
        };
    }

	/**
	 * Returns <code>true</code> if the bit at location <code>bit</code> is set to 1.
	 *
	 * @param b - byte to be examined
	 * @param bit - bit location in <code>b</code> to be examined
	 *
	 * @return <code>true</code> if the bit at location <code>bit</code> is set to 1
	 */
	public static boolean isBitSet(byte b, int bit){
    	return isBitSet((int)b & 0xFF, bit);
	}//isBitSet

	/**
	 * Returns <code>true</code> if the bit at location <code>bit</code> is set to 1.
	 *
	 * @param b - integer to be examined
	 * @param bit - bit location in <code>b</code> to be examined
	 *
	 * @return <code>true</code> if the bit at location <code>bit</code> is set to 1
	 */
	public static boolean isBitSet(int b, int bit){
		// Generate a mask to isolate the bit to be examined.
		// e.g. bit=0, mask=0
		//		bit=1, mask=2 (10 in binary)
		int mask = 1 << bit;

		// When the mask is applied, the 'bit of interest' will be ANDED to 1.
		// All other bits will be ANDED with 0.
    	return (b & mask) != 0;
	}//isBitSet

	/**
	 * Returns <code>true</code> unless the specified <code>String</code> is empty,
	 * <code>null</code>, or contains only white space.
	 *
	 * @param str - string to be examined
	 *
	 * @return <code>true</code> unless the specified <code>String</code> is empty,
	 * 		<code>null</code>, or contains only white space
	 */
	public static boolean stringHasValue(String str) {
		boolean hasValue = true;

		// check if the string is null
		if(str == null) {
			hasValue = false;
		}
		else {  // if the string is not null, check if it is empty or white space
			if(str.trim().equals("")) {
				hasValue = false;
			}
		}

		return hasValue;
	}//hasStringValue

	/**
	 * Returns <code>true</code> if the specified <code>String</code> is empty,
	 * <code>null</code>, or contains only white space.
	 *
	 * @param str - string to be examined
	 *
	 * @return <code>true</code> if the specified <code>String</code> is empty,
	 * 		<code>null</code>, or contains only white space
	 */
	public static boolean isStringEmpty(String str) {
		return !stringHasValue(str);
	}//isStringEmpty

	/**
	 * Returns a random integer from the inclusive range defined by the specified
	 * start value and the specified end value.
	 *
	 * @param start - the smallest integer that can be returned
	 * @param end - the greatest integer that can be returned
	 *
	 * @return a random integer from the inclusive range defined by the specified
	 * 		start value and the specified end value
	 */
	public static int generateRandomInteger(int start, int end) {

		Random random = new Random();
	    if ( start > end ) {
	      throw new IllegalArgumentException("Start cannot exceed End.");
	    }
	    //get the range, casting to long to avoid overflow problems
	    long range = (long)end - (long)start + 1;
	    // compute a fraction of the range, 0 <= frac < range
	    long fraction = (long)(range * random.nextDouble());
	    int randomNumber =  (int)(fraction + start);
		return randomNumber;
	}

	/**
	 * Returns <code>true</code> if the specified <code>String</code> can be successfully
	 * parsed to a <code>long</code>
	 *
	 * @param string - string to be examined
	 *
	 * @return <code>true</code> if the specified <code>String</code> can be
	 * successfully parsed to a <code>long</code>
	 */
	public static boolean isLong(String string) {
		boolean canBeParsed = false;

		// Attempt to parse the string as a long.  If the parse operation is successful,
		// 'canBeParsed' will be set to 'true'.  If the the parse operation fails due to
		// NumberFormatException, 'canBeParsed' will remain false.
		try {
			Long.valueOf(string);
			canBeParsed = true;
		}
		catch(NumberFormatException nfe) {
			// do nothing
		}

		return canBeParsed;
	}

	/**
	 *
	 * This method is used to print out the stack trace of a StackTraceElement
	 *
	 * @param elements - e.getStackTrace()
	 *
	 */
	public static void printStackTrace(StackTraceElement[] elements){

		 for(StackTraceElement element : elements) {
			 logger.error("##### "+ element.toString());
		 }

	}

	/**
	 * Returns <code>String</code> from the Exception getMessage to be used in logger
	 *
	 * @param exception - JMS type Exception
	 *
	 * @return <code>String</code> from JMSException.getMessage()
	 */
	//public static String errorHandle(JMSException exception) {
	//	return errorHandle(exception);
	//}

	/**
	 * Returns <code>String</code> from the Exception getMessage to be used in logger
	 *
	 * @param exception - SQLException type Exception
	 *
	 * @return <code>String</code> from SQLException.getMessage()
	 */
	//public static String errorHandle(SQLException exception) {
	//	logger.error(exception.toString());
	//	//return errorHandle(exception);
	//}

	/**
	 * Returns <code>String</code> from the Exception getMessage to be used in logger
	 *
	 * @param exception - IOException type Exception
	 *
	 * @return <code>String</code> from IOException.getMessage()
	 */
	//public static String errorHandle(IOException exception) {
	//	return errorHandle(exception);
	//}

	/**
	 * Sends an {@link ErrorReport} via the {@link ErrorReporter} based on the specified Exception
	 * and prints the stack trace.<br>
	 * <br>
	 * Returns the detail message from the Exception.<br>
	 * <br>
	 * NOTE: The error report will not be sent if the configuration parameter 'REPORT_ERRORS' is set to false.
	 *
	 * @param exception - Exception to be reported
	 *
	 * @return detail message from Exception.getMessage()
	 */
	public static String errorHandle(Exception exception) {
		return errorHandle(exception, "");
	}

	/**
	 * Sends an {@link ErrorReport} via the {@link ErrorReporter} based on the specified Exception
	 * and message.  Prints the stack trace.<br>
	 * <br>
	 * Returns the detail message from the Exception.<br>
	 * <br>
	 * NOTE: The error report will not be sent if the configuration parameter 'REPORT_ERRORS' is set to false.
	 *
	 * @param exception - Exception type Exception
	 * @param message - message to be included in the error report
	 *
	 * @return detail message from Exception.getMessage()
	 */
	public static String errorHandle(Exception exception, String message) {
/*
		ErrorReporter reporter = ErrorReporter.ERROR_REPORTER;
		ErrorReport report = new ErrorReport(exception, message);
		reporter.reportError(report);
*/
		printStackTrace(exception.getStackTrace());
		String errorMessage = exception.toString();

		if(message != null && errorMessage != "") {
			errorMessage = message + " - " + errorMessage;
		}
		return errorMessage;
	}

	public static Properties loadProperties() {
		Properties props = new Properties();
		String propertiesFile = "./conf/kpl.properties";
		try {
			InputStream input = new FileInputStream(propertiesFile);
			props.load(input);
		} catch(FileNotFoundException e) {
			Utilities.errorHandle(e);
		} catch(IOException e) {
			Utilities.errorHandle(e);
		}
		return props;
	}





	/**
	 * Returns the rounded value of the specified number to the specified decimal place. For,
	 * example:<br>
	 * <ul>
	 * <li>1.234 rounded to the 1 decimal place yields 1.2</li>
	 * <li>4.567 rounded to the 1 decimal place yields 4.6</li>
	 * <li>4.567 rounded to the 2 decimal place yields 4.57</li>
	 * <li>4.567 rounded to the 3 decimal place yields 4.567</li>
	 * <ul>
	 *
	 * @param doubleToRound 	number to be rounded
	 * @param decimalPosition 	decimal position to round to
	 *
	 * @return rounded value of the specified number to the specified decimal place
	 */
	public static double roundDouble(double doubleToRound, int decimalPosition) {
		// BigDecimal must be used to correctly round in certain situations.
		// Simply using Math.round(doubleToRound * decimalPosition) / decimalPosition
		// will occasionally yield the wring answer due to errors in floating-point rounding.
		BigDecimal roundedValue = new BigDecimal(doubleToRound).setScale(decimalPosition, RoundingMode.HALF_UP);
		return roundedValue.doubleValue();
	}

	/**
	 * Replaces foreign characters within a string to the equivalent US English
	 * characters.
	 *
	 * @param in - String to have foreign characters replaced
	 *
	 * @return input string with foreign characters replaced with the equivalent
	 * 		US English characters
	 */
	public static String foreignCharacterConverter(String in){
		//logger.info("Foreign Character Converter: "+ in);

		try{
			in = in.replaceAll("[\\xC0-\\xC6]", "A");
			in = in.replaceAll("[\\xC8-\\xCB]", "E");
			in = in.replaceAll("[\\xCC-\\xCF]", "I");
			in = in.replaceAll("[\\xD2-\\xD6]", "O");
			in = in.replaceAll("[\\xD9-\\xDC]", "U");
			in = in.replaceAll("[\\xD1]", "N");

			in = in.replaceAll("[\\xE0-\\xE6]", "a");
			in = in.replaceAll("[\\xE8-\\xEB]", "e");
			in = in.replaceAll("[\\xEC-\\xEF]", "i");
			in = in.replaceAll("[\\xF2-\\xF6]", "o");
			in = in.replaceAll("[\\xF9-\\xFC]", "u");
			in = in.replaceAll("[\\xF1]", "n");

			//catch all, or anything left
			in = in.replaceAll("([\\x7F-\\xFF]|[\\x00-\\x1F]|[\\u00FF-\\uFFFF])", "");

		}
		catch (Exception e) {
			logger.info("Foreign Character Converter: "+ in);
			logger.error("Utilities.foreignCharacterConverter: "+  Utilities.errorHandle(e));
		}

		return in;
	}

	/**
	 * Returns the number of milliseconds until midnight the next day.
	 *
	 * @return the number of milliseconds until midnight the next day
	 */
	public static long getMillisTilNextDay() {
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		long currentMillis = cal.getTimeInMillis();
		System.out.println(cal.getTime());

		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		long nextDayMillis = cal.getTimeInMillis();
		if(nextDayMillis - currentMillis < 0) {
			cal.roll(Calendar.DAY_OF_MONTH, 1);
			nextDayMillis = cal.getTimeInMillis();
		}
		System.out.println(cal.getTime());

		return nextDayMillis - currentMillis;
	}

	/**
	 * Returns the distance in meters between the specified coordinate pairs.
	 *
	 * Based on the haversine formula:
	 * https://en.wikipedia.org/wiki/Haversine_formula
	 *
	 * @param lat1 - latitude of the first coordinate pair.
	 * @param lon1 - longitude of the first coordinate pair.
	 * @param lat2 - latitude of the second coordinate pair.
	 * @param lon2 - longitude of the second coordinate pair.
	 *
	 * @return the distance in meters between the specified coordinate pairs.
	 */
	public static int coordinatesToMeters(double lat1, double lon1, double lat2, double lon2) {
		double R = 6378.137; // Earth radius in km
		double lat1Rad = (lat1 * Math.PI / 180);
		double lon1Rad = (lon1 * Math.PI / 180);
		double lat2Rad = (lat2 * Math.PI / 180);
		double lon2Rad = (lon2 * Math.PI / 180);
		double term1 = Math.pow(Math.sin((lat2Rad - lat1Rad)/2), 2);
		double term2 = Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.pow(Math.sin((lon2Rad - lon1Rad)/2), 2);
		double term3 = term1 + term2;
		double distance = R * 2 * Math.asin(Math.sqrt(term3)) * 1000;
		return (int)Math.round(distance);
	}

	/**
	 * Returns the distance in meters between latitude values at the specified reference longitude.
	 *
	 * Based on the haversine formula:
	 * https://en.wikipedia.org/wiki/Haversine_formula
	 *
	 * @param lat1 - first latitude.
	 * @param lat2 - second latitude.
	 * @param lon - reference longitude
	 *
	 * @return the distance in meters between latitude values
	 */
	public static int deltaLatitudeToMeters(double lat1, double lat2, double lon) {
		double R = 6378.137; // Earth radius in km
		double lat1Rad = (lat1 * Math.PI / 180);
		double lat2Rad = (lat2 * Math.PI / 180);
		double inner = Math.pow(Math.sin((lat2Rad - lat1Rad)/2), 2);
		double distance = R * 2 * Math.asin(Math.sqrt(inner)) * 1000;
		return (int)Math.round(distance);
	}

	/**
	 * Returns the distance in meters between longitude values at the specified reference latitude.
	 *
	 * Based on the haversine formula:
	 * https://en.wikipedia.org/wiki/Haversine_formula
	 *
	 * @param lat1 - first longitude.
	 * @param lat2 - second longitude.
	 * @param lon - reference latitude
	 *
	 * @return the distance in meters between longitude values
	 */
	public static int deltaLongitudeToMeters(double lon1, double lon2, double lat) {
		double R = 6378.137; // Earth radius in km
		double lon1Rad = (lon1 * Math.PI / 180);
		double lon2Rad = (lon2 * Math.PI / 180);
		double latRad = (lat * Math.PI / 180);
		double inner = Math.pow(Math.cos(latRad), 2) * Math.pow(Math.sin((lon2Rad - lon1Rad)/2), 2);
		double distance = R * 2 * Math.asin(Math.sqrt(inner)) * 1000;
		return (int)Math.round(distance);
	}

	/**
	 * Returns <code>true</code> if the specified Vehicle Identification Number (VIN) is valid.
	 *
	 * @param vin - VIN to be examined
	 *
	 * @return <code>true</code> if the specified Vehicle Identification Number (VIN) is valid.
	 */
	public static boolean validateVin(String vin) {
		boolean valid = false;

		if(vin.trim().length() == 17) {
			String transliterationTable = "0123456789.ABCDEFGH..JKLMN.P.R..STUVWXYZ";
			String map = "0123456789X";
			String weights = "8765432X098765432";
			int sum = 0;
			for (int i = 0; i < 17; ++i) {
				sum += (transliterationTable.indexOf(vin.charAt(i)) % 10) * (map.indexOf(weights.charAt(i)));
			}
			char checkDigit = map.charAt(sum % 11);
			valid = checkDigit == vin.charAt(8);
		}

		if(!valid) {
			logger.error("Invalid VIN: " + vin);
		}

		return valid;
	}//validateVin

}// Utilities
