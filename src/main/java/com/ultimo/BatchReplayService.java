package com.ultimo;

import io.undertow.server.HttpServerExchange;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.security.handlers.IAuthToken;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class BatchReplayService extends ApplicationLogicHandler implements IAuthToken {

	static MongoClient mongoClient;
	public BatchReplayService(PipedHttpHandler next, Map<String, Object> args) 
	{
		super(next, args);
	}

	@Override
	public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception 
	
	{
		mongoClient = getMongoConnection(exchange, context);
		String payload= "";
		InputStream inputS = exchange.getInputStream();
		BufferedReader payloadReader = new BufferedReader(new InputStreamReader(inputS));
		while(true)
		{
			String input = payloadReader.readLine();
			if (input != null)
			{
				payload = payload + input;
			}
			else
			{
				break;
			}
		}
		System.out.println(payload);
		
		JSONObject input = new JSONObject(payload);
		BatchHandleRequest(input);

	}
	
	public static void BatchHandleRequest(JSONObject input)
	{
		ArrayList<ObjectId> objectIDs = new ArrayList<ObjectId>();
		
		String auditID = input.get("auditID").toString();
		auditID = auditID.replace("[", "").replace("]", "").replace("\"", "");
		String[] objectIDStrings = auditID.split(",");
		
		for (String id : objectIDStrings)
		{
			ObjectId object = new ObjectId(id);
			objectIDs.add(object);
		}
		
		JSONObject replayDestinationInfo = input.getJSONObject("replayDestinationInfo");

		String result = "";
		if (replayDestinationInfo.get("type").toString().equalsIgnoreCase("REST"))
		{
			// Call Method Handling Rest Request
			result = handleRestBatch(input, objectIDs);
			
		}
		else if (replayDestinationInfo.get("type").toString().equalsIgnoreCase("WS"))
		{
			
		}
		else if (replayDestinationInfo.get("type").toString().equalsIgnoreCase("FILE"))
		{
			
		}
		else if (replayDestinationInfo.get("type").toString().equalsIgnoreCase("FTP"))
		{
			
		}
	}
	
	public static String handleRestBatch(JSONObject input, ArrayList<ObjectId> objectIDs)
	{
		//Declare and Extract all Necessary Information
		JSONObject replayDestinationInfo = input.getJSONObject("replayDestinationInfo");
		String restMethod = replayDestinationInfo.getString("method");
		String restEndpoint = replayDestinationInfo.getString("endpoint");
		String contentType = replayDestinationInfo.getString("contentType");			
		String replaySavedTimestamp = input.getString("replaySavedTimestamp");
		String restHeaders = "[" + input.get("restHeaders").toString().replace(":", "=").replace("{", "").replace("}", "") + "]";
		String replayedBy = input.getString("replayedBy");
		String batchProcessedTimestamp = input.getString("batchProcessedTimestamp");
		String auditCollectionName = MongoDBClientSingleton.getErrorSpotConfig("u-audit-collection");
		String payloadCollectionName = MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection");
		String mongoDatabase = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
		
		
		//Create MongoDB Connection
		DB db = mongoClient.getDB(mongoDatabase);
		DBCollection auditCollection = db.getCollection(auditCollectionName);
		DBCollection payloadCollection = db.getCollection(payloadCollectionName);
		
		// Get DataLocations
		BasicDBList objectIds = new BasicDBList();
		objectIds.addAll(objectIDs);
		BasicDBObject auditSearchInClause = new BasicDBObject("$in",objectIds); 
		BasicDBObject auditSearchClause = new BasicDBObject("_id",auditSearchInClause); 

		DBCursor auditsResult = auditCollection.find(auditSearchClause);
		ArrayList<ObjectId> dataLocations = new ArrayList<ObjectId>();

		while (auditsResult.hasNext())
		{
			DBObject audit = auditsResult.next();
			if (audit.containsField("dataLocation"))
			{
				String ObjectID = audit.get("dataLocation").toString();
				//System.out.println(ObjectID);
				dataLocations.add(new ObjectId(ObjectID));
			}

		}
		
		BasicDBList payloadIds = new BasicDBList();
		payloadIds.addAll(dataLocations);
		DBObject inClause = new BasicDBObject("$in",payloadIds);
		DBObject payloadQuery = new BasicDBObject("_id" , inClause);
		DBCursor payloadQueryResult = payloadCollection.find(payloadQuery);
		while (payloadQueryResult.hasNext())
		{
			DBObject payload = payloadQueryResult.next();
			String convertedPayload = PayloadService.jsonToPayload(payload);
			String [] restRequestInput = new String[6];
			restRequestInput[0] = restEndpoint;
			restRequestInput[1] = restEndpoint;
			restRequestInput[2] = restMethod;
			restRequestInput[3] = contentType;
			restRequestInput[4] = restMethod;
			restRequestInput[5] = restHeaders;
			
			try {
				//ReplayService.handleREST(exchange, context, restRequestInput);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return "Success";
		
	}

	private static MongoClient getMongoConnection(HttpServerExchange exchange,RequestContext context) {
		MongoClient client = MongoDBClientSingleton.getInstance().getClient();   
		return client;
			}
}
