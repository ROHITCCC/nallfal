package com.ultimo;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Attachable;
import io.undertow.util.AttachmentKey;
import io.undertow.util.AttachmentList;
import io.undertow.util.Headers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.json.XML;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.hal.Representation;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.handlers.collection.PostCollectionHandler;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.RequestHelper;
import org.restheart.utils.ResponseHelper;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

public class XMLService extends ApplicationLogicHandler {

	public XMLService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
	}


	@SuppressWarnings("unchecked")
	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception {
		
		 if (context.getMethod() == METHOD.POST)
			{
			 	InputStream input = exchange.getInputStream();
				BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
				String mongoInput = "";
				readLoop : while(true)
						{
							String inputTemp = inputReader.readLine();
							if (inputTemp != null) 
							{
								mongoInput = mongoInput + inputTemp;
							}
							else 
							{
								break readLoop;
							}
						}
				System.out.println(mongoInput);
				Object id = new ObjectId();
				String referenceID = payloadInsert(id, mongoInput, context, exchange);
				
				DBObject inputObject = (DBObject) JSON.parse(mongoInput);
				
				System.out.println(	auditInsert(id.toString(), inputObject, context, exchange));
			//	System.out.println(referenceID);
				
			//AttachmentKey<AttachmentList<ArrayList>> key = AttachmentKey.createList(ArrayList.class);
		//	System.out.println(exchange.getAttachmentList(key).get(0));
			
				
				System.out.println(id.toString());
			}
		 else 
			 
			{
			 ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
			 exchange.endExchange();
			}
		 
		}

	private static String getData(HttpServerExchange exchange,RequestContext context, String documentID) 
	{
		try{
			Mongo mongo = getMongoConnection(exchange, context);
			DB db = mongo.getDB(context.getDBName());
			DBCollection collection = db.getCollection(context.getCollectionName());
			String json = null;
			String objectID = exchange.getQueryString();
			if (objectID.equals(""))
			{
	            return "No Query Parameter Given";
			}
			String id = "payloadID";
			DBObject searchQuery = new BasicDBObject(id, objectID);
			DBObject HOOLA = collection.findOne(searchQuery);
			
			
			if (HOOLA != null)
			{
				json = (HOOLA.get("payload")).toString();
				System.out.println("Read JSON" + json);
				json= json.replace("&quot", "\"");
		        int code = HttpStatus.SC_ACCEPTED;
	            exchange.setResponseCode(code);
	            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, Representation.HAL_JSON_MEDIA_TYPE);
	           exchange.getResponseSender().send(json);
	           System.out.println("HELLO!");
	            return "Finished";
			}
			
			else {
				return "Document Was Not Found";
			}
			
			}
		catch(Exception e) 
			{
				e.printStackTrace();
				return "Error Happened";
			}
	}
		
	
	private static String auditInsert (String referenceID , DBObject inputObject, RequestContext context,HttpServerExchange exchange) throws Exception
	{
         inputObject.put("referenceID", referenceID);
	     context.setContent(inputObject);
	     PostCollectionHandler handler = new PostCollectionHandler();
	     handler.handleRequest(exchange, context);
	     return "done";

	   
	}
	
	private static String payloadInsert(Object id, String inputxml, RequestContext context, HttpServerExchange exchange) throws Exception
	{
		
		
		inputxml = inputxml.replace("\"", "&quot;");
        String intermediateJSON = "{\"_id\" : \""+ id.toString()+   "\", \"payload\" : \""+ inputxml + "\"}";
	    DBObject inputObject = (DBObject) JSON.parse(intermediateJSON);
	    MongoClient client = getMongoConnection(exchange, context);
	    DB db = client.getDB("ES");
	    DBCollection collection = db.getCollection("payloadCollection");
	    collection.insert(inputObject);
	    
	     return "done";
		
	}
	
	private static MongoClient getMongoConnection(HttpServerExchange exchange,
			RequestContext context) {
		MongoClient db = MongoDBClientSingleton.getInstance().getClient();   
		return db;
			}

}
