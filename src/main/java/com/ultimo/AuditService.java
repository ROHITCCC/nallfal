package com.ultimo;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.bson.types.ObjectId;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.hal.Representation;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.handlers.collection.PostCollectionHandler;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

public class AuditService extends ApplicationLogicHandler {

	public AuditService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
	}


	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception 
	{	
		 if (context.getMethod() == METHOD.POST)
			{
			 InputStream input = exchange.getInputStream();
				
				BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
				String payload = "";
				String delimiter = "";
				String audit = "";
				int i = 0;
				readLoop : while(true)
						{
							
							String inputTemp = inputReader.readLine();
							if (inputTemp!= null)
							{
								if (i == 0) 
								{
									delimiter = (inputTemp.split(";"))[1];
								}
								else 
								{
									payload = (payload + inputTemp).replace("\n", "");
								}
								i++;
							}
							else 
							{
								break readLoop;

							}
						}
				
				 delimiter = "--"+(delimiter.split("=")[1]);

				 String[] message = payload.split(delimiter);
				 int m = 0;

				 	for (String mess: message)
				 	{
				 		if (mess.contains("application/json") & m == 0)
				 		{
				 			audit = mess.split("application/json")[1];
				 			System.out.println(audit);
				 		}
				 		else if (mess.contains("application/xml"))
				 		{
				 			payload = mess.split("application/xml")[1];
				 			System.out.println(payload);
				 		}
				 		else if (mess.contains("text/plain"))
				 		{
				 			payload = mess.split("text/plain")[1];
				 			System.out.println(payload);	
				 		}
				 	
				 	}
				 	ObjectId id = new ObjectId();
					String status =  payloadInsert(id, payload, context, exchange);
					if (status.equalsIgnoreCase("done"))
					{
						DBObject inputDBObject = (DBObject) JSON.parse(audit);
						System.out.println(audit);
						auditInsert(id, inputDBObject, context, exchange);
					}

				 	
			}
		 else if (context.getMethod() == METHOD.GET)
		 	{
			 	getData(exchange,context);
		 	}
		 else 
			 
			{
			 ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
			 exchange.endExchange();
			}
		 
		}	
	
	private static String auditInsert (ObjectId referenceID , DBObject inputObject, RequestContext context,HttpServerExchange exchange) throws Exception
	{
	         inputObject.removeField("dataLocation");
		     inputObject.put("dataLocation", referenceID.toString());
	         System.out.println(inputObject.containsField("timestamp"));
	         String timestamp =  inputObject.get("timestamp").toString();
	         System.out.println(timestamp);
	 	     Date gtDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(timestamp);
	 	     System.out.println(gtDate.toString());
	         inputObject.removeField("timestamp");
	         inputObject.put("timestamp",gtDate);
		     context.setContent(inputObject);
		     PostCollectionHandler handler = new PostCollectionHandler();
		     handler.handleRequest(exchange, context);
	     return "done";

	   
	}
	
	private static String payloadInsert(ObjectId id, String inputxml, RequestContext context, HttpServerExchange exchange) throws Exception
	{
		System.out.println(id.toString());
		inputxml = inputxml.replace("\"", "&quot;");
        String intermediateJSON = "{\"payload\" : \""+ inputxml + "\"}";
        System.out.println(intermediateJSON);
	    DBObject inputObject = (DBObject) JSON.parse(intermediateJSON);
	    inputObject.put("_id", id);
	    MongoClient client = getMongoConnection(exchange, context);
	    DB db = client.getDB(context.getDBName());
	    DBCollection collection = db.getCollection("payloadCollection");
	    collection.insert(inputObject);
	    
	     return "done";		
	}
	
	private static String getData(HttpServerExchange exchange,RequestContext context) 
	{

		MongoClient client = getMongoConnection(exchange,context);
		DB db = client.getDB(context.getDBName());
		DBCollection collection = db.getCollection("payloadCollection");
		String objectID = exchange.getQueryString().replace("id=", "");
		System.out.println(objectID);
		ObjectId queryObjectId = new ObjectId(objectID);
		DBObject resultDocument = collection.findOne(new BasicDBObject("_id",queryObjectId));
		String payload = (resultDocument.get("payload").toString()).replace("&quot;", "\"");
		System.out.println(payload);
		int code = HttpStatus.SC_ACCEPTED;
        exchange.setResponseCode(code);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, Representation.HAL_JSON_MEDIA_TYPE);
        exchange.getResponseSender().send(payload);
        return "Finished";
		
	}
	
	private static MongoClient getMongoConnection(HttpServerExchange exchange,
			RequestContext context) {
		MongoClient client = MongoDBClientSingleton.getInstance().getClient();   
		return client;
			}

}
