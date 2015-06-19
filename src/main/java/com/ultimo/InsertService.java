package com.ultimo;

import io.undertow.server.HttpServerExchange;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.bson.types.ObjectId;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.handlers.collection.PostCollectionHandler;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientException;
import com.mongodb.MongoException;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;
import java.io.ByteArrayInputStream;
import org.apache.commons.fileupload.MultipartStream;

public class InsertService extends ApplicationLogicHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart");
	
	public InsertService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
 
	}

	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception 
	{	
		
		try
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
								payload = payload + inputTemp + "\r\n";
							}
							else 
							{
								payload = (payload + inputTemp + "\r\n");
							}
							i++;
						}
						else 
						{
							break readLoop;
	
						}
					}
			    delimiter = delimiter.split("=")[1];
			    byte[] boundary = delimiter.getBytes();
			    byte[] contents = payload.getBytes();
		        ByteArrayInputStream content = new ByteArrayInputStream(contents);
		        MultipartStream multipartStream = new MultipartStream(content, boundary,1000, null);	        
		        boolean nextPart = multipartStream.skipPreamble();
		        int m = 0;
		        while (nextPart) {
		        	
		        	if (m==0)
		        	{ 	
				            ByteArrayOutputStream body = new ByteArrayOutputStream();
				            multipartStream.readBodyData(body);
				             audit = new String(body.toByteArray());
					        LOGGER.info("Audit Recieved");

	       
		        	}
		        	else if(m==1)
		        	{
				            ByteArrayOutputStream body = new ByteArrayOutputStream();
				            multipartStream.readBodyData(body);
				            payload = new String(body.toByteArray());
					        LOGGER.info("Payload Recieved");

		        	}
	           nextPart = multipartStream.readBoundary();
	           m++;
		        }
		        LOGGER.debug("PayloadRecieved");
		        LOGGER.trace("Payload: " + payload);
		        payload = payload.replace("\r\n", "");
		        LOGGER.trace("Payload: " + payload);
		        ObjectId id = new ObjectId();
		        LOGGER.trace("Payload Object ID: " + id.toString());
		        
				String status =  payloadInsert(id, payload, context, exchange);
				LOGGER.debug("Payload Insert: " + status);
				
				if (status.equalsIgnoreCase("Success"))
				{
					LOGGER.trace("Audit to be inserted" + audit);

					DBObject inputDBObject = (DBObject) JSON.parse(audit);
					String auditInsertStatus = auditInsert(id, inputDBObject, context, exchange);
					if (auditInsertStatus == "Success")
					{
						LOGGER.debug("Audit Inserted Successfully");
					}
					else 
					{
						LOGGER.debug("Audit Insert Failed");
					}
				}
			}
		
		catch(JSONParseException e) 
	    {
	    	LOGGER.error("Incorrectly Formated JSON Array. Please check JSON Array Format");
	    	
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted JSON Array. Please check JSON Array Format");
	        
	
	    }
		
	
	    catch(Exception e) 
	    {
	    	LOGGER.error("Unspecified Application Error" );
	
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unspecified Application Error");
	
	    }
            
	}

	
	private static String auditInsert (ObjectId referenceID , DBObject inputObject, RequestContext context,HttpServerExchange exchange) throws Exception
	{
		String status = "";
	    try{
	    	 
	     
				 inputObject.removeField("dataLocation");
			     inputObject.put("dataLocation", referenceID.toString());
		         System.out.println(inputObject.containsField("timestamp"));
		         if (!inputObject.containsField("timestamp"))
		         {
		        	 LOGGER.debug("Audit does not contain timestamp");
		         }
		         String timestamp =  inputObject.get("timestamp").toString();
		         System.out.println(timestamp);
		 	     Date gtDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(timestamp);
		 	     System.out.println(gtDate.toString());
		         inputObject.removeField("timestamp");
		         inputObject.put("timestamp",gtDate);
			     context.setContent(inputObject);
			     PostCollectionHandler handler = new PostCollectionHandler();
			     handler.handleRequest(exchange, context);
			     status = "Success";
	    	}
	    catch(	java.text.ParseException e) 
		   {
		    	LOGGER.error("Date Not Correctly Formatted. Date Format is: YYYY-MM-DDTHH:MM:SS");
		    	
		        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted Date. Accepted Date Format is: YYYY-MM-DDTHH:MM:SS");
			    MongoClient client = getMongoConnection(exchange, context);
			    DB db = client.getDB(context.getDBName());
			    DBCollection collection = db.getCollection("samplePayloads");
			    DBObject removalObject = new BasicDBObject("_id", referenceID);
			    collection.remove(removalObject);
			     status = "Failed";


		
		   }
	    catch(IllegalArgumentException e) 
		   {
		    	LOGGER.error("Date Not Correctly Formatted. Date Format is: YYYY-MM-DDTHH:MM:SS");
		    	
		        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted JSON Array. Please check JSON Array Format");
			    MongoClient client = getMongoConnection(exchange, context);
			    DB db = client.getDB(context.getDBName());
			    DBCollection collection = db.getCollection("samplePayloads");
			    DBObject removalObject = new BasicDBObject("_id", referenceID);
			    collection.remove(removalObject);
			     status = "Failed";

		
		   }
		 catch(JSONParseException e) 
			   {
			    	LOGGER.error("Incorrectly Formated JSON Array. Please check JSON Array Format");
			    	
			        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted JSON Array. Please check JSON Array Format");
				    MongoClient client = getMongoConnection(exchange, context);
				    DB db = client.getDB(context.getDBName());
				    DBCollection collection = db.getCollection("samplePayloads");
				    DBObject removalObject = new BasicDBObject("_id", referenceID);
				    collection.remove(removalObject);
				     status = "Failed";

			
			   }
			  
	    catch(MongoClientException e)
	    {
	    	LOGGER.error("MongoDB Client Error. Ensure that DB and Collection exist");
	    	LOGGER.error(e.getMessage());
	    	status = "Failed";
	    	MongoClient client = getMongoConnection(exchange, context);
		    DB db = client.getDB(context.getDBName());
		    DBCollection collection = db.getCollection("samplePayloads");
		    DBObject removalObject = new BasicDBObject("_id", referenceID);
		    collection.remove(removalObject);
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "MongoDB Client Exception. Please check MongoDB Status");
		    status = "Failed";

	    }
		catch(MongoException e)
	    {
	    	LOGGER.error("General MongoDB Error. Please check MongoDB Connection and Permissions");
	    	LOGGER.error(e.getMessage());
	    	status = "Failed";
	    	MongoClient client = getMongoConnection(exchange, context);
		    DB db = client.getDB(context.getDBName());
		    DBCollection collection = db.getCollection("samplePayloads");
		    DBObject removalObject = new BasicDBObject("_id", referenceID);
		    collection.remove(removalObject);
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "General MongoDB Error. Please check MongoDB Connection and Permissions");
		     status = "Failed";

	    }
	
	    catch(Exception e) 
	    {
	    	LOGGER.error("Unspecified Application Error" );
	    	MongoClient client = getMongoConnection(exchange, context);
		    DB db = client.getDB(context.getDBName());
		    DBCollection collection = db.getCollection("samplePayloads");
		    DBObject removalObject = new BasicDBObject("_id", referenceID);
		    collection.remove(removalObject);
	    	status = "Failed";
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unspecified Application Error");
	        status = "Failed";

	    }
			     return status;		
			
	       
 	}
	
	private static String payloadInsert(ObjectId id, String input, RequestContext context, HttpServerExchange exchange) throws Exception
	{
		String status = "";
		try {
			input = input.replace("\"", "&quot;");
	        String intermediateJSON = "{\"payload\" : \""+ input + "\"}";
		    DBObject inputObject = (DBObject) JSON.parse(intermediateJSON);
		    inputObject.put("_id", id);
		    MongoClient client = getMongoConnection(exchange, context);
		    DB db = client.getDB(context.getDBName());
		    DBCollection collection = db.getCollection("payloadCollection");
		    collection.insert(inputObject);
		    status = "Success";
		}
		catch(JSONParseException e) 
	    {
	    	LOGGER.error("Incorrectly Formated JSON Array. Please check JSON Array Format");
	    	
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted JSON Array. Please check JSON Array Format");
	        
	
	    }
	    catch(MongoClientException e)
	    {
	    	LOGGER.error("MongoDB Client Error. Ensure that DB and Collection exist");
	    	LOGGER.error(e.getMessage());
	    	status = "Failed";

	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "MongoDB Client Exception. Please check MongoDB Status");
	
	    }
		catch(MongoException e)
	    {
	    	LOGGER.error("General MongoDB Error. Please check MongoDB Connection and Permissions");
	    	LOGGER.error(e.getMessage());
	    	status = "Failed";

	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "General MongoDB Error. Please check MongoDB Connection and Permissions");
	
	    }
	
	    catch(Exception e) 
	    {
	    	LOGGER.error("Unspecified Application Error" );
	
	    	status = "Failed";
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unspecified Application Error");

	    }
	     return status;		
	}
	
	private static MongoClient getMongoConnection(HttpServerExchange exchange,
			RequestContext context) {
		MongoClient client = MongoDBClientSingleton.getInstance().getClient();   
		return client;
			}

}
