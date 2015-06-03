package com.ultimo;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Map;

import org.bson.types.ObjectId;
import org.json.*;
import org.restheart.db.DocumentDAO;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.hal.Representation;
import org.restheart.handlers.IllegalQueryParamenterException;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.handlers.document.DocumentRepresentationFactory;
import org.restheart.handlers.document.PutDocumentHandler;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;



 // Not Finished. We have issues with the XML Quotes Configuration. 
public class XMLConversionService extends ApplicationLogicHandler 

{
	public XMLConversionService(PipedHttpHandler next, Map<String, Object> args) 
	{
		super(next, args);
	}	
	
	public static String XMLConversion(HttpServerExchange exchange,RequestContext context, String inputXML, MongoClient mongo, String dbName, String collectionName) throws Exception
	{
         JSONObject jsonObj = XML.toJSONObject(inputXML); 
         String intermediateJSON = jsonObj.toString();
         intermediateJSON = "{\"payload\" : "+ intermediateJSON + "}";
	     JSONObject jsonObj2 = new JSONObject(intermediateJSON);
        return MongoDBDocumentWrite(exchange, context, jsonObj2.toString(),mongo, dbName, collectionName);
	}
	
	public static String MongoDBDocumentWrite(HttpServerExchange exchange,RequestContext context,  String inputJSON, MongoClient mongo, String dbName, String collectionName) throws Exception
 
	{		
		DB db = mongo.getDB(dbName);
		DBCollection collection = db.getCollection(collectionName);
		DBObject dbObject = (DBObject) JSON.parse(inputJSON);
		context.setContent(dbObject);
		PutDocumentHandler documentHandler = new PutDocumentHandler();
		//PutDocumentHandler documentHandler = new PutDocumentHandler(new DocumentDAO());
		documentHandler.handleRequest(exchange, context);
		//documentHandler.
		//WriteResult result = collection.insert(dbObject);
		//JSONObject inter = new JSONObject(result.toString());
		//System.out.println(dbObject.toString());
		String resultOutput = "Inserted Document. Number of Documents Inserted: "; //+ inter.get("ok");
		return resultOutput;

		
	}	
	
	public static String MongoDBDocumentRead( String dbName,String collectionName,HttpServerExchange exchange,RequestContext context) throws IllegalQueryParamenterException
	{
		try
		{
			MongoClient mongo = MongoDBClientSingleton.getInstance().getClient();
			DB db = mongo.getDB(dbName);
			DBCollection collection = db.getCollection(collectionName);

			String objectID = exchange.getQueryString();
			if (objectID.equals(""))
			{
	            return "No Query Parameter Given";
			}
			String id = "_id";
			DBObject searchID = new BasicDBObject(id, new ObjectId(objectID));
			DBObject HOOLA = collection.findOne(searchID);
			JSONObject output = new JSONObject(HOOLA.toString());
			String xmlInput = XML.toString(output);
			DBObject outputObject = (DBObject) JSON.parse("{\"payload\": \"" + xmlInput.replace("$oid", "oid") + "\"}");
			System.out.println();
			
			if (HOOLA != null)
			{
		        int code = HttpStatus.SC_ACCEPTED;
	            exchange.setResponseCode(code);
	            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, Representation.HAL_JSON_MEDIA_TYPE);
	           // exchange.getResponseSender().send(rep.toString());
	            return "Finished";
			}
			else 
			{
	            return "Document Was Not Found";
			}
			
		}
		catch(Exception e) 
		{
			e.printStackTrace();
			return "Unspecified Errors";
		}
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception 
	{
		
		if (context.getMethod() == METHOD.GET)
		{
		
			String readResult = MongoDBDocumentRead( context.getDBName(), context.getCollectionName(), exchange, context);
			switch(readResult)
			{
				case "No Query Parameter Given" : ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, readResult);
				break;
				case "Finished" : exchange.endExchange();
				break;
				case "Unspecified Errors" : ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_METHOD_FAILURE, readResult);
				break;
				case "Document Was Not Found" : ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_FOUND, readResult);
				break; 
			}
		}
		
		else if (context.getMethod() == METHOD.POST)
		{
			InputStream input = exchange.getInputStream();
			BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
			String mongoInput = "";
			MongoClient dbClient = getMongoConnection(exchange, context);
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
			System.out.println(context.getDocumentId());
			System.out.println(context.getContent().get("_id"));
			ObjectId id = new ObjectId();
			String results = XMLConversion(exchange,context, mongoInput,dbClient,context.getDBName(), context.getCollectionName());	
			//ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_ACCEPTED, results);
		}
		else 
		{
			 exchange.setResponseCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
	         exchange.endExchange();
		}

	}

	public static MongoClient getMongoConnection (HttpServerExchange exchange,RequestContext context) 
	{		
		MongoClient db = MongoDBClientSingleton.getInstance().getClient();   
		return db;
	}

}
