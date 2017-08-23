package com.gtc.gtclisteners.data;

import com.amazonaws.services.dynamodbv2.document.Item;
import java.util.Date;
public class Outbound {
	public static final String MOBILE_ID_INDEX = "MobileId";

	public static final String ID = "Id";
	public static final String DATA = "Data";
	public static final String MOBILE_ID = "MobileId";
	public static final String CREATED = "Created";

	String id;
	String mobileId;
	String data;
	Date created;
	

	public Outbound(Item dynamoDbItem) {
		id = dynamoDbItem.getString(ID);
		mobileId = dynamoDbItem.getString(MOBILE_ID);
		data = dynamoDbItem.getString(DATA);
		created = new Date(dynamoDbItem.getLong(CREATED));
	}

	public String getId() {
		return id;
	}

	public String getData() {
		return data;
	}

	public String getMobileId() {
		return mobileId;
	}

	public Date getCreated() {
		return created;
	}

	
    @Override
    public String toString() {
		return mobileId + " - " + data;
	}
}