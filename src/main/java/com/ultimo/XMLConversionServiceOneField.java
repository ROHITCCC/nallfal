package com.ultimo;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Map;

import org.json.*;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.hal.Representation;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.handlers.document.PutDocumentHandler;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

public class XMLConversionServiceOneField extends ApplicationLogicHandler
{

	public XMLConversionServiceOneField(PipedHttpHandler next,
			Map<String, Object> args) {
		super(next, args);
	}	
	
	public static String SendXMLToDB(HttpServerExchange exchange,RequestContext context,MongoClient mongo, String inputXML) throws UnknownHostException
	{
		try
		{
		inputXML = inputXML.replace("\"", "&quot");
		String referenceID = exchange.getQueryString();
        String intermediateJSON = "{\"referenceID\" : \"" +referenceID +"\",\"payload\" : \""+ inputXML + "\"}";
	    JSONObject jsonObj2 = new JSONObject(intermediateJSON);
	    
	    //DB db = mongo.getDB(context.getDBName());
		//DBCollection collection = db.getCollection(context.getCollectionName());
		DBObject dbObject = (DBObject) JSON.parse(jsonObj2.toString());
		context.setContent(dbObject);
		PutDocumentHandler documentHandler = new PutDocumentHandler();
		documentHandler.handleRequest(exchange, context);

		//WriteResult result = collection.insert(dbObject);
		
		
		//JSONObject inter = new JSONObject(result.toString());
		//String resultOutput = "Inserted Document. Number of Documents Inserted: " + inter.get("ok");

		return "Okay";

		}
		
		catch(Exception e) 
		{
			return "Errors Occured";
		}
	    
	}

	public static String MongoDBDocumentRead(HttpServerExchange exchange, RequestContext context)
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
		String id = "referenceID";
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
	
	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception 
	{
		
		 if (context.getMethod() == METHOD.POST)
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
				
				String results = SendXMLToDB(exchange, context,dbClient,mongoInput);
				System.out.println(results);

			}
		 else if (context.getMethod() == METHOD.GET)
			{
				
				String readResult = MongoDBDocumentRead(exchange, context);
				switch(readResult)
				{
					case "No Query Parameter Given" : ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, readResult);
					break;
					case "Unspecified Errors" : ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, readResult);
					break;
					case "Document Was Not Found" : ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_FOUND, readResult);
					break; 
				}
			}
		 
		 else {
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
