package com.ultimo;

import io.undertow.server.HttpServerExchange;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.http.HttpStatus;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.security.handlers.IAuthToken;
import org.restheart.utils.ResponseHelper;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

public class BatchReplayService extends ApplicationLogicHandler implements IAuthToken {

	static MongoClient mongoClient= getMongoConnection();
	public BatchReplayService(PipedHttpHandler next, Map<String, Object> args) 
	{
		super(next, args);
	}
	private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart");

	@Override
	public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception 
	
	{
		
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
				break;//
			}
		}
		
		JSONObject input = new JSONObject(payload);
		/*
		LOGGER.trace("Starting Insert into Database" );
		String dbname = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
		String collectionName = MongoDBClientSingleton.getErrorSpotConfig("u-batch-replay-collection");
		MongoClient db = MongoDBClientSingleton.getInstance().getClient();
        DB database = db.getDB(dbname);
        DBCollection collection = database.getCollection(collectionName);
        BasicDBObject object =  (BasicDBObject) collection.findOne(new BasicDBObject("_id", new ObjectId("55bfaebb231a6071ccdb43f9")));
        batchHandleRequest(new JSONObject(object.toString()));
        */
		handleBatchCalls(exchange, context, input.toString());
		
		

	}
	
	public static Map<String,String> batchHandleRequest(JSONObject input) throws Exception
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
		System.out.println(replayDestinationInfo.get("type").toString());
		Map<String,String> result = null;
		if (replayDestinationInfo.get("type").toString().equalsIgnoreCase("REST"))
		{
			// Call Method Handling Rest Request
			result = handleRestBatch(input, objectIDs);
			
		}
		else if (replayDestinationInfo.get("type").toString().equalsIgnoreCase("WS"))
		{
			//result = handleWSBatch(input, objectIDs);
		}
		else if (replayDestinationInfo.get("type").toString().equalsIgnoreCase("FILE"))
		{
			result = handleFileBatch(input, objectIDs);
		}
		else if (replayDestinationInfo.get("type").toString().equalsIgnoreCase("FTP"))
		{
			result = handleFTPBatch(input, objectIDs);
		}		
		return result;
		
	}
	
	private static Map<String,String> handleRestBatch(JSONObject input, ArrayList<ObjectId> objectIDs) throws Exception
	{
		//Declare and Extract all Necessary Information
		JSONObject replayDestinationInfo = input.getJSONObject("replayDestinationInfo");
		
		String auditCollectionName = MongoDBClientSingleton.getErrorSpotConfig("u-audit-collection");
		String payloadCollectionName = MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection");
		String mongoDatabase = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
		Map<String,String> output = new HashMap<String,String>();
		
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
		ArrayList<DBObject> auditList = new ArrayList<DBObject>();
		Map<String,String> payloadAndAuditId = new HashMap<String,String>();

		while (auditsResult.hasNext())
		{
			DBObject audit = auditsResult.next();
			if (audit.containsField("dataLocation"))
			{
				auditList.add(audit);
				String ObjectID = audit.get("dataLocation").toString();
				dataLocations.add(new ObjectId(ObjectID));
				payloadAndAuditId.put(ObjectID,audit.get("_id").toString());
			}
		}
		
		BasicDBList payloadIds = new BasicDBList();
		payloadIds.addAll(dataLocations);
		DBObject inClause = new BasicDBObject("$in",payloadIds);
		DBObject payloadQuery = new BasicDBObject("_id" , inClause);
		DBCursor payloadQueryResult = payloadCollection.find(payloadQuery);
		JSONObject replayInput = input.getJSONObject("replayDestinationInfo");
		if (replayDestinationInfo.has("restHeaders"))
		{
		replayInput.put("restHeaders", replayDestinationInfo.getJSONArray("restHeaders"));
		}
		replayInput.put("replayedBy", input.getString("replayedBy"));
		replayInput.put("content-type", replayDestinationInfo.getString("content-type"));
		replayInput.put("type", "REST");

		while (payloadQueryResult.hasNext())
		{
			DBObject payload = payloadQueryResult.next();
			String payloadID = payload.get("_id").toString();
			String convertedPayload = PayloadService.jsonToPayload(payload);
			String auditID = payloadAndAuditId.get(payloadID);
			replayInput.put("auditID", auditID);

			try
			{
			String handleResult = ReplayService.handleReplays(replayInput , convertedPayload);
			if (handleResult !=null & !handleResult.equals("Success"))
			{
			output.put(auditID, handleResult);
			}
			}
			catch(Exception e)
			{
				output.put(auditID, "Undefined ErrorSpot Error");
				e.printStackTrace();
			}
		}
			System.out.println(output.size());
		return output;
		
	}
/*	
	private static Map<String,String> handleWSBatch(JSONObject input, ArrayList<ObjectId> objectIDs)
		{
		//Declare and Extract all Necessary Information
		String auditCollectionName = MongoDBClientSingleton.getErrorSpotConfig("u-audit-collection");
		String payloadCollectionName = MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection");
		String mongoDatabase = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
		JSONObject replayDestinationInfo = input.getJSONObject("replayDestinationInfo");
		String wsSoapAction= replayDestinationInfo.getString("soapaction");
		String wsdl = replayDestinationInfo.getString("wsdl");
		String wsBinding = replayDestinationInfo.getString("binding");
		String wsOperation = replayDestinationInfo.getString("operation");
		Map<String,String> output = new HashMap<String,String>();
		Map<String,String> payloadAndAuditId = new HashMap<String,String>();

		
		
		
		
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
				System.out.println(ObjectID);
				dataLocations.add(new ObjectId(ObjectID));
				payloadAndAuditId.put(ObjectID,audit.get("_id").toString());

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
			String payloadID = payload.get("_id").toString();
			String auditID = payloadAndAuditId.get(payloadID);

			String convertedPayload = PayloadService.jsonToPayload(payload);
			System.out.println(convertedPayload);
			String [] wsRequestInput = new String[6];
			wsRequestInput[0] = "";
			wsRequestInput[1] = wsdl;
			wsRequestInput[2] = wsOperation;
			wsRequestInput[3] = wsSoapAction;
			wsRequestInput[4] = wsBinding;
			wsRequestInput[5] = convertedPayload;

			try 
			{
				String[] handleResult = ReplayService.handleWS(wsRequestInput);
				if (handleResult[1] !=null)
				{
				output.put(auditID, handleResult[1]);
				}
				}
				catch(Exception e)
				{
					if (e.getMessage() != null)
					{
						output.put(auditID, e.getMessage());

					}
					else
					{
					output.put(auditID, "Undefined ErrorSpot Error");
					e.printStackTrace();
					}
				}					}
		return output;
		
	}
	*/
	private static Map<String,String> handleFileBatch(JSONObject input, ArrayList<ObjectId> objectIDs)
	{
		//Declare and Extract all Necessary Information
		JSONObject replayDestinationInfo = input.getJSONObject("replayDestinationInfo");
		String fileLocation = replayDestinationInfo.getString("fileLocation");
		String fileName =fileLocation.split("\\.")[0];
		String fileType = fileLocation.split("\\.")[1];
		String auditCollectionName = MongoDBClientSingleton.getErrorSpotConfig("u-audit-collection");
		String payloadCollectionName = MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection");
		String mongoDatabase = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
		Map<String,String> output = new HashMap<String,String>();
		
		
		//Create MongoDB Connection
		DB db = mongoClient.getDB(mongoDatabase);
		DBCollection auditCollection = db.getCollection(auditCollectionName);
		DBCollection payloadCollection = db.getCollection(payloadCollectionName);
		
		// Get DataLocations
		BasicDBList objectIds = new BasicDBList();
		objectIds.addAll(objectIDs);
		BasicDBObject auditSearchInClause = new BasicDBObject("$in",objectIds); 
		BasicDBObject auditSearchClause = new BasicDBObject("_id",auditSearchInClause); 
		Map<String,String> payloadAndAuditId = new HashMap<String,String>();

		DBCursor auditsResult = auditCollection.find(auditSearchClause);
		ArrayList<ObjectId> dataLocations = new ArrayList<ObjectId>();

		while (auditsResult.hasNext())
		{
			DBObject audit = auditsResult.next();
			if (audit.containsField("dataLocation"))
			{
				String ObjectID = audit.get("dataLocation").toString();
				dataLocations.add(new ObjectId(ObjectID));
				payloadAndAuditId.put(ObjectID,audit.get("_id").toString());
				
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
			String payloadID = payload.get("_id").toString();
			String auditID = payloadAndAuditId.get(payloadID);

			String id = payload.get("_id").toString();
			System.out.println(payload.toString());
			String convertedPayload = PayloadService.jsonToPayload(payload);
			Calendar cal = Calendar.getInstance();
			DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss_");
			String sysDate = dateFormat.format(cal.getTime());
			// Payload ID to Track them. 
			JSONObject replayInput = input.getJSONObject("replayDestinationInfo");
			replayInput.put("replayedBy", input.getString("replayedBy"));
			replayInput.put("type", "File");
			replayInput.put("auditID", auditID);
			replayInput.put("fileLocation",fileName +"_"+ sysDate + id +  "."+ fileType);

			try {
				String handleResult = ReplayService.handleReplays(replayInput, convertedPayload);
				if (handleResult !=null & !handleResult.equals("Success"))
				{
				output.put(auditID, handleResult);
				}
				}
				catch(Exception e)
				{
					if (e.getMessage() != null)
					{
						output.put(auditID, e.getMessage());

					}
					else
					{
					output.put(auditID, "Undefined ErrorSpot Error");
					e.printStackTrace();
					}
				}			
		}
		return output;
		
	}
	
	
	private static Map<String,String> handleFTPBatch(JSONObject input, ArrayList<ObjectId> objectIDs)
	{
		//Declare and Extract all Necessary Information
		
		JSONObject replayDestinationInfo = input.getJSONObject("replayDestinationInfo");
		String location = replayDestinationInfo.getString("location");
		String hostname = replayDestinationInfo.getString("host");
		String username = replayDestinationInfo.getString("username");
		String password = replayDestinationInfo.getString("password");
		String fileType = replayDestinationInfo.getString("filetype");
		String fileName = replayDestinationInfo.getString("filename");
		String replayedBy = input.getString("replayedBy");
		int port = replayDestinationInfo.getInt("port");

		String auditCollectionName = MongoDBClientSingleton.getErrorSpotConfig("u-audit-collection");
		String payloadCollectionName = MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection");
		String mongoDatabase = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
		Map<String,String> output = new HashMap<String,String>();

		
		//Create MongoDB Connection
		DB db = mongoClient.getDB(mongoDatabase);
		DBCollection auditCollection = db.getCollection(auditCollectionName);
		DBCollection payloadCollection = db.getCollection(payloadCollectionName);
		Map<String,String> payloadAndAuditId = new HashMap<String,String>();

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
				payloadAndAuditId.put(ObjectID,audit.get("_id").toString());
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
			String payloadID = payload.get("_id").toString();
			String auditID = payloadAndAuditId.get(payloadID);

			String id = payload.get("_id").toString();
			String convertedPayload = PayloadService.jsonToPayload(payload);
			Calendar cal = Calendar.getInstance();
			DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss_");
			String sysDate = dateFormat.format(cal.getTime());
			// Payload ID to Track them. 
			
			
			JSONObject replayInput = new JSONObject();
			replayInput.put("host", hostname);
			replayInput.put("username", username);
			replayInput.put("password", password);
			replayInput.put("location", location);
			replayInput.put("fileName", fileName +"_"+ sysDate + id);
			replayInput.put("fileType", fileType);
			replayInput.put("type", "FTP");
			replayInput.put("replayedBy", replayedBy);
			replayInput.put("auditID", auditID);
			replayInput.put("port", port);

			try {
				String handleResult = ReplayService.handleReplays(replayInput, convertedPayload);				
				if (handleResult !=null & !handleResult.equals("Success"))
				{
				output.put(auditID, handleResult);
				}
				}
				catch(Exception e)
				{
					if (e.getMessage() != null)
					{
						output.put(auditID, e.getMessage());

					}
					else
					{
					output.put(auditID, "Undefined ErrorSpot Error");
					e.printStackTrace();
					}
				}	
		}
		return output;
		
	}
	
	private static MongoClient getMongoConnection() {
		MongoClient client = MongoDBClientSingleton.getInstance().getClient();   
		return client;
			}
	
	public static void handleBatchCalls(HttpServerExchange exchange,RequestContext context, String payload) throws java.text.ParseException{
	      //if the thing is a JSON and query is batch, insert it
      	try{
          	BasicDBObject batchObject=(BasicDBObject)JSON.parse(payload);
          	
		            String replaySavedTimestamp =  batchObject.get("replaySavedTimestamp").toString();
			        Date gtDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(replaySavedTimestamp);
		 	        
			        batchObject.removeField("replaySavedTimestamp");
			        batchObject.put("replaySavedTimestamp",gtDate);
          	
          	insertBatch(batchObject);
          	exchange.getResponseSender().send("batch sucessfully inserted");
          }
          catch(JSONParseException e){
          	LOGGER.error("the error: ", e);
      		ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "batch was unable to be inserted");
          }
      	return;
      }
	
	
	public static void insertBatch(DBObject batchObject){
		LOGGER.info("excecuting BatchReplayJob");
		//connect to appropriate cdb and collection
		MongoClient client = MongoDBClientSingleton.getInstance().getClient();
		String dbname = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
		String collectionName = MongoDBClientSingleton.getErrorSpotConfig("u-batch-replay-collection");
      DB database = client.getDB(dbname);
      DBCollection collection = database.getCollection(collectionName);
      LOGGER.trace("connected to db: "+dbname);
      LOGGER.info("connected to collection: "+collectionName);
      collection.insert(batchObject);
	}




}
