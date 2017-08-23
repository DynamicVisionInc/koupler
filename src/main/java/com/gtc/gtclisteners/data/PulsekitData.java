package com.gtc.gtclisteners.data;

import com.gtc.gtclisteners.common.Utilities;
import java.util.Properties;
import java.util.ArrayList;
import com.gtc.gtclisteners.data.Outbound;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class PulsekitData {
	protected static final Logger logger = LoggerFactory.getLogger(PulsekitData.class);

	private Properties props;
	public PulsekitData() {
		this.props = Utilities.loadProperties();
	}

	
	public ArrayList<Outbound> getOutboundMessages(String mobileId) {
		logger.info("preparing to query for " + mobileId);
		ArrayList<Outbound> messages = new ArrayList<Outbound>();

		Table table = getOTATable();
		
		ItemCollection<QueryOutcome> result = table.getIndex(Outbound.MOBILE_ID_INDEX).query(Outbound.MOBILE_ID, mobileId);
		for(Item item:result) {
			logger.info("Outbound Message: \n" + item.toJSONPretty());
			messages.add(new Outbound(item));
		}

		return messages;
	} 

	public DeleteItemOutcome deleteOutboundItem(Outbound item) {
		return getOTATable().deleteItem(Outbound.ID, item.getId());
	}

	private Table getOTATable() {
		String tableName = props.getProperty("OUTBOUND_TABLE");
		return getTable(tableName);
	}
	private Table getTable(String tableName) {
		return getDynamoDB().getTable(tableName);
	}
	private DynamoDB getDynamoDB() {

		String region = props.getProperty("Region");

		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
		.withRegion(region)
		.build();

		return new DynamoDB(client);
	}
}