package com.gtc.gtclisteners.receivers.calamp.calamp32;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import org.apache.log4j.Logger;

import com.gtc.gtclisteners.common.Utilities;

/**
 * This class holds a list of geofences providing methods to parse raw geofence strings and
 * generate a byte message containing the geofence data.
 */
public class CalampGeofenceStructure {

	//private static final Logger logger = Logger.getLogger(CalampGeofenceStructure.class);

	/**
	 * List of {@link Geozone} objects.
	 */
	private List<Geozone> geozones;

	public CalampGeofenceStructure() {
		geozones = new ArrayList<Geozone>();
	}

	/**
	 * Parses the specified geozone data and places it in the list of
	 * {@link Geozone} objects.
	 *
	 * @param geozoneData string array containing the geozone data to be parsed
	 */
	public void parseGeozoneData(String[] geozoneData) {

		String operation = geozoneData[2];

		if(operation.equals("create")) {
			int geozoneId = Integer.parseInt(geozoneData[3]);
			int startingIndex = Integer.parseInt(geozoneData[4]) * 20;
			Geozone geozone = new Geozone(geozoneId, startingIndex);
			for(int i = 5; i < geozoneData.length; i++) {
				String[] recordArray = geozoneData[i].split(":");
				Record record  = new Record(recordArray[0], recordArray[1]);
				geozone.addRecord(record);
			}
			geozones.add(geozone);
		}
		else if(operation.equals("delete")) {
			int geozoneId = Integer.parseInt(geozoneData[3]);
			int startingIndex = Integer.parseInt(geozoneData[4]) * 20;
			geozones.add(new Geozone(geozoneId, startingIndex));
			// Don't add any Records.  All records will be filled in with blank data when
			// the geozone data is retrieved.  This will effectively delete the geozone.
		}
		else if(operation.equals("recycle")) {
			// TODO
		}
	}

	/**
	 * Returns a byte array that is a complete message to be sent to the Calamp unit.
	 * The message includes all geozone data.  If a geozone has less than 20 records,
	 * it's padded with blank records.  This ensures that the geozones will start in increments
	 * of 20.
	 *
	 * @param mobileIdLength
	 * @param esnBytes
	 * @param messageId
	 *
	 * @return a byte array representing a single 'blank' geofence point
	 */
	public byte[] getGeozoneData(int mobileIdLength, byte[] esnBytes, int messageId) {

		byte[] geozoneData = null;

		byte[] messageHeader = null;
		byte[] geofenceBytes = new byte[0];
		byte[] geofenceHeader = new byte[4];

		int geozoneCount = geozones.size();
		int recordCount = geozoneCount * 20;

		// Prepare the message header
		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(0x83);			//options byte
			baos.write(mobileIdLength); //Mobile ID length
			baos.write(esnBytes);		//Mobile ID
			baos.write(0x01);//Mobile ID type length
			baos.write(0x01);//Mobile ID type
			baos.write(0x00);//Service type 0=no ack
			baos.write(0x05);//Message type 5=Application Data Message
			baos.write(0x00);//Sequence - Used to identify a specific message
			baos.write(messageId);//Sequence cont'd
			baos.write(0x00);//GeoZone Action Message
			baos.write(0x75);//GeoZone Action Message - Update Message
			baos.write(0x00);//Application Message Length
			baos.write((recordCount * 12) + 4);//Application Message Length

			messageHeader = baos.toByteArray();
			baos.close();
		}
		catch(IOException ioe) {
			//logger.error("Error preparing the message header: "+ Utilities.errorHandle(ioe));
		}

		// Prepare the geofence header
		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(0x00);
			baos.write(recordCount);
			int startingIndex = geozones.get(0).getStartingIndex();
			baos.write(startingIndex / 0x100); // Start Index
			baos.write(startingIndex % 0x100); // Start Index
			geofenceHeader = baos.toByteArray();
			baos.close();
		}
		catch(IOException ioe) {
			//logger.error("Error preparing the geofence header: "+ Utilities.errorHandle(ioe));
		}

		for(Geozone geozone : geozones) {

			for(Record record : geozone.records) {
				double latitudeD = Double.parseDouble(record.latitude);
				long latitudeL = Math.round(latitudeD * 10000000D);
				// The copyOf method ensures that the array will have a length of 4, padded with zeros.
				byte[] latitudeBytes = Arrays.copyOf(Utilities.hexStringToByteArray(Long.toHexString(latitudeL)), 8);

				double longitudeD = Double.parseDouble(record.longitude);
				long longitudeL = Math.round(longitudeD * 10000000D);
				// The copyOf method ensures that the array will have a length of 4, padded with zeros.
				byte[] longitudeBytes = Arrays.copyOf(Utilities.hexStringToByteArray(Long.toHexString(longitudeL)), 8);

				byte[] pointBytes = null;
				try{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					baos.write((byte)0x02);	//Zone Type  2= polygon
					baos.write(geozone.id);	//Zone ID
					baos.write(0x00);		//Range (not used in polygons)
					baos.write(0x00);		//Range (not used in polygons)

					// Latitude
					if(latitudeD < 0) {
						baos.write((byte)latitudeBytes[4]);
						baos.write((byte)latitudeBytes[5]);
						baos.write((byte)latitudeBytes[6]);
						baos.write((byte)latitudeBytes[7]);
					}
					else {
						baos.write((byte)latitudeBytes[0]);
						baos.write((byte)latitudeBytes[1]);
						baos.write((byte)latitudeBytes[2]);
						baos.write((byte)latitudeBytes[3]);
					}

					// Longitude
					if(longitudeD < 0) {
						baos.write((byte)longitudeBytes[4]);
						baos.write((byte)longitudeBytes[5]);
						baos.write((byte)longitudeBytes[6]);
						baos.write((byte)longitudeBytes[7]);
					}
					else {
						baos.write((byte)longitudeBytes[0]);
						baos.write((byte)longitudeBytes[1]);
						baos.write((byte)longitudeBytes[2]);
						baos.write((byte)longitudeBytes[3]);
					}

					pointBytes = baos.toByteArray();
					baos.close();
				}
				catch(IOException ioe) {
					//logger.error("Error preparing the geofence point message: "+ Utilities.errorHandle(ioe));
				}

				byte[] old = geofenceBytes;
				byte[] n = Arrays.copyOf(old, old.length + pointBytes.length);
				System.arraycopy(pointBytes, 0, n, old.length, pointBytes.length);
				geofenceBytes = n;
			} //for each record

			int remainingRecordCount = 20 - geozone.getRecordCount();
			byte[] remainingRecords = new byte[remainingRecordCount * 12];
			for(int i = 0; i < remainingRecordCount; i++) {
				System.arraycopy(generateBlankRecord(), 0, remainingRecords, i * 12, 12);
			}

			byte[] old = geofenceBytes;
			byte[] n = Arrays.copyOf(old, old.length + remainingRecords.length);
			System.arraycopy(remainingRecords, 0, n, old.length, remainingRecords.length);
			geofenceBytes = n;
		} //for each geofence

		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(messageHeader);	// add the message header
			baos.write(geofenceHeader);	// add the geofence header
			baos.write(geofenceBytes);	// add the geofence points
			geozoneData = baos.toByteArray();
			baos.close();
		}
		catch(IOException ioe){
			//logger.error("Error constructing the geofence message: "+ Utilities.errorHandle(ioe));
		}

		return geozoneData;
	}

	/**
	 * Returns a byte array representing a single 'blank' geofence point.
	 *
	 * @return a byte array representing a single 'blank' geofence point
	 */
	private byte[] generateBlankRecord() {

		byte[] blankRecord = new byte[12];

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for(int i = 0; i < 12; i++) {
				baos.write(0x00);
			}

			blankRecord = baos.toByteArray();
			baos.close();
		}
		catch (IOException ioe) {
			//logger.error("Error generating blank geozone record: "+ Utilities.errorHandle(ioe));	
		}

		return blankRecord;
	}

	/**
	 * Returns the number of geofences in this structure.
	 *
	 * @return the number of geofences in this structure
	 */
	public int getGeozoneCount() {
		return geozones.size();
	}

	/**
	 * Returns the starting index of the next consecutive geofence.
	 *
	 * @return the starting index of the next consecutive geofence
	 */
	public int getNextIndex() {
		Geozone lastGeozone = geozones.get(geozones.size() - 1);
		return lastGeozone.getStartingIndex() + 20;
	}

	/**
	 * Structure representing a single geofence.
	 */
	private class Geozone {

		/**
		 * Geozone (Geofence) ID.
		 */
		private int id;

		/**
		 * List of Geozone (Geofence) points.
		 */
		private List<Record> records;

		/**
		 * Memory location of the first record of the geozone
		 */
		private int startingIndex;

		public Geozone(int id, int startingIndex) {
			this.id = id;
			this.startingIndex = startingIndex;
			records = new ArrayList<Record>();
		}

		public int getStartingIndex() {
			return startingIndex;
		}

		public int getRecordCount() {
			return records.size();
		}

		public void addRecord(Record record) {
			records.add(record);
		}
	}

	/**
	 * Structure representing a single point within a geofence.
	 */
	private class Record {
		private String latitude;
		private String longitude;

		public Record(String latitude, String longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}
	}
}
