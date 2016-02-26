package com.ultimo;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONML;
import org.json.JSONObject;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.handlers.collection.PostCollectionHandler;
import org.restheart.security.handlers.IAuthToken;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientException;
import com.mongodb.MongoException;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

import java.io.ByteArrayInputStream;

import org.apache.commons.fileupload.MultipartStream;

public class InsertService extends ApplicationLogicHandler implements IAuthToken {

	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");
	
	public InsertService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
 
	}

	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception 
	{	

		 if (context.getMethod() == METHOD.OPTIONS) 
	        {
			 	ErrorSpotSinglton.optionsMethod(exchange);
	        } 
		 else if (context.getMethod() == METHOD.POST)
		 {
			try
			{
				
				/*
				 * Declare the Variables that will be used in this Insert Service. 
				 */
				String payload = "";
				String delimiter = "";
				String audit = "";
				String auditHeaders="";
				String payloadHeaders="";
				String payloadContentType = "";
				/*
				 * Read the Payload of the Incoming Request, and find the delimiter.
				 */
				InputStream input = exchange.getInputStream();
				BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
				int i = 0;
				readLoop : while(true)
						{
							
							String inputTemp = inputReader.readLine();
							if (inputTemp!= null)
							{
								//if (i == 0)
								if(!(inputTemp.trim().isEmpty()) && delimiter.isEmpty())
								{
									//delimiter = (inputTemp.split(";"))[1];
									delimiter = inputTemp.substring(2);
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
				LOGGER.debug("InsertService executing...");
				LOGGER.trace("Payload received: " + payload);
				
				//delimiter = delimiter.split("=")[1]; 
				
				//=============================================================================================================================================
				/*
				 * Read and Parse Multipart/Mixed Message.     
				 */
				    byte[] boundary = delimiter.getBytes();
				    byte[] contents = payload.getBytes();
			        ByteArrayInputStream content = new ByteArrayInputStream(contents);
			        MultipartStream multipartStream = new MultipartStream(content, boundary,1000, null);
			        
			        boolean nextPart = multipartStream.skipPreamble();
			        int m = 0;
			        while (nextPart)
			        {
			        	
			        	//Logic to determine Audit or payload from stream
			        	String partHeaders = multipartStream.readHeaders();
			        	String[] partHeadersArray = partHeaders.split("\n");
			        	String partContentType = "";
			        	String partType = "";
				       
			        	
			        	for (String headerPart : partHeadersArray)
				        {
				        	
				        	
				        		if (headerPart.toLowerCase().contains("content-type"))
					        	{
					        		String[] contentType = headerPart.split(":");
					        		partContentType = contentType[1].trim().replace(";", "");
					        	}
				        		
				        		//look for content-dispostion to get the type of data sent in this part (either payload or audit)
				        		if (headerPart.toLowerCase().contains("content-disposition"))
					        	{
					        		String[] contentDispostion = headerPart.split(";");
					        		for(String disposition: contentDispostion){
					        			if(disposition.toLowerCase().replaceAll("\\s+","").contains("type=\"payload\"") || disposition.toLowerCase().replaceAll("\\s+","").contains("type=\"audit\""))
					        			partType = disposition.substring(disposition.indexOf("=") + 1).replace("\"", "").trim();
					        		}
					        		
					        	}	
				        	
				        }
			        	
			        	//If content-type is missing in part then return 400 and exit.
			        	if (partContentType.length() == 0){
					    	LOGGER.error("Incorrect Payload: Content-Type is missing in one of MIME messages");
					        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "Incorrect Payload: Content-Type is missing in one of MIME messages");
					        
					        return;

			        	}
			        	
			        	//If part type (audit or payload) could not be determined from content-dispostion then check  
			        	//the data for required fields for audit and determine the type
			        	ByteArrayOutputStream body = new ByteArrayOutputStream();
	
					    multipartStream.readBodyData(body);
					    String temp = new String(body.toByteArray());
					    
			        	try{
				        	if((partType.length() == 0 
				        			|| !(partType.contentEquals("audit") || partType.contentEquals("payload"))) 
				        			&& partContentType.contentEquals("application/json")){
					            
					            
					            temp = temp.replace("\r\n", "");
					       
					            DBObject tempDBObject = (DBObject) JSON.parse(temp);
					            
					           // DBObject tempDBObject = (DBObject)PayloadService.payloadtoJSON(temp, "application/json", exchange, context).get("payload");
					            
					      
					            if (tempDBObject.containsField("envid") && tempDBObject.containsField("application") &&
					            	tempDBObject.containsField("interface1") &&tempDBObject.containsField("timestamp")&&
					            	tempDBObject.containsField("exceptionFlag")){
					            	
					            	partType = "audit";
					            	
					            } else {
					            	partType = "payload";
					            	
					            }
					            
	 			        	} 
			        	
			        	}
			        	 /*
					        * If there is an error in conversion, then the payload will be saved as a text/plain payload. Conversion will happen in the PayloadService
					        */
					       catch(PayloadConversionException e)
					       {
					    	   partType = "payload";	
					       }
			        	
			        	
			        	if (partType.contentEquals("audit"))
			        	{ 	
					            //ByteArrayOutputStream body = new ByteArrayOutputStream();
					           //auditHeaders = multipartStream.readHeaders();
			        			auditHeaders = partHeaders;
					            //multipartStream.readBodyData(body);
					            // audit = new String(body.toByteArray());
			        			audit = temp;
						        LOGGER.debug("Audit extracted from multipart message: boundary info: " + delimiter);
	
		       
			        	}
			        	if (partType.contentEquals("payload"))
			        	{
				                //payloadHeaders = multipartStream.readHeaders();
			        			payloadHeaders = partHeaders;
					            //ByteArrayOutputStream body = new ByteArrayOutputStream();

					            //multipartStream.readBodyData(body);
					            //payload = new String(body.toByteArray());
				                payload = temp;
						        LOGGER.debug("Payload extracted from multipart message: boundary info: " + delimiter);

	
			        	}
			        	
			        	
			           nextPart = multipartStream.readBoundary();
			           m++;
			        }
			        
			        
			        audit = audit.replace("\r\n", "");
			        payload = payload.replace("\r\n", "");
			        String[] pHeaders = payloadHeaders.split("\n");
			        
					//=============================================================================================================================================
					/*
					 * Find out the content type of the Payload Headers.     
					 */

			        for (String headerPart : pHeaders)
			        {
			        	
			        	String [] line = headerPart.split(";");
			        	for (String linePart : line)
			        	{
			        		if (linePart.contains("Content-Type") || linePart.contains("content-type") | linePart.contains("Content-type") | linePart.contains("content-Type") )
				        	{
				        		String[] contentType = linePart.split(":");
				        		payloadContentType = contentType[1].trim().replace(";", "");
				        	}			        	
			        	}
			        }
					//=============================================================================================================================================
					/*
					 * Insert the Payload based upon the Content Type. The Payload will be Converted in PayloadService    
					 */

			       DBObject payloadInput = null;
			       try{
				        if (payloadContentType.equalsIgnoreCase("application/xml"))
				        {
				        	payloadInput = PayloadService.payloadtoJSON(payload, "application/xml", exchange, context);
				        }
				        else if (payloadContentType.equalsIgnoreCase("application/json"))
				        {
				        	payloadInput = PayloadService.payloadtoJSON(payload, "application/json", exchange, context);
				        }
				        else if (payloadContentType.equalsIgnoreCase("text/plain"))
				        {
				        	payloadInput = PayloadService.payloadtoJSON(payload, "text/plain", exchange, context);
				        }
				        else 
				        {
				        	LOGGER.error("Not Acceptable Input");
							 throw new PayloadConversionException();
				        }
			       }
			       
			       /*
			        * If there is an error in conversion, then the payload will be saved as a text/plain payload. Conversion will happen in the PayloadService
			        */
			       catch(PayloadConversionException e)
			       {
			    	   LOGGER.error("Payload was Saved in text/plain format." );
			        	payloadInput = PayloadService.payloadtoJSON(payload, "text/plain", exchange, context);
			       }
			       
			       // Decide a ObjectID fro the payload. 
			       
			        ObjectId id = new ObjectId();
			        
			        //Insert the Payload in to the Payload Collection. 
			        
					String status = payloadInsert(id, payloadInput, context, exchange);
					LOGGER.info("Payload Insert: " + status + ", with payload object ID: " + id);
					
					if (status.equalsIgnoreCase("Success"))
					{
							
						DBObject inputDBObject = (DBObject) JSON.parse(audit);
						
						
				     //If the payload insert is successful, then insert the audit. 
	
						String auditInsertStatus = auditInsert(id, inputDBObject, context, exchange);
						if (auditInsertStatus == "Success")
						{
							LOGGER.info("Audit Inserted Successfully");
						}
						else 
						{
							LOGGER.info("Audit Insert Failed");
						}
					}
					
				}    
			
			catch(JSONParseException e) 
		    {
		    	LOGGER.error("Incorrectly Formated JSON Object. Please check JSON Object Format");
		    	
		        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted JSON Object. Please check JSON Object Format");
		    }
		 }
		 else
			{
				 ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
				 exchange.endExchange();

			}
            
	}
	
	
	protected static String insertService (HttpServerExchange exchange, RequestContext context)
	{
		return null;
		
	}
	
	
	private static String auditInsert (ObjectId referenceID , DBObject inputObject, RequestContext context,HttpServerExchange exchange) throws Exception
	{
		String status = "";
	    try{
		       /*
		        * Steps for Audit Insert
		        *  1) Add Payload ID to Reference ID
		        *  2) Check if Audit has a timestamp or not. 
		        *  3) Change the Format of the Date from String to a Date Object. 
		        *  4) Update the timestamp in the payload object. 
		        *  5) Insert the Update. 
		        */
	     
	    		 //Makes audit's envid field into uppercase
   		 		 String uppercaseEnvid = inputObject.get("envid").toString().toUpperCase();
   		 		 inputObject.removeField("envid");
   		 		 inputObject.put("envid", uppercaseEnvid);
   		 		 
   		 	     //Makes audit's application, transactionDomain, transactionType, severity, and errorType fields into lowercase
   		 		 String lowercaseApplication = inputObject.get("application").toString().toLowerCase();
   		 		 inputObject.removeField("application");
  		 		 inputObject.put("application", lowercaseApplication);
  		 		 
   		 		 String lowercaseTDomain = inputObject.get("transactionDomain").toString().toLowerCase();
   		 		 inputObject.removeField("transactionDomain");
 		 		 inputObject.put("transactionDomain", lowercaseTDomain);
 		 		 
   		 		 String lowercaseTType = inputObject.get("transactionType").toString().toLowerCase();
   		 		 inputObject.removeField("transactionType");
 		 		 inputObject.put("transactionType", lowercaseTType);
 		 		 
   		 	     String lowercaseSeverity = inputObject.get("severity").toString().toLowerCase();
   		 	     inputObject.removeField("severity");
		 		 inputObject.put("severity", lowercaseSeverity);
		 		 
   		         String lowercaseEType = inputObject.get("errorType").toString().toLowerCase();
   		 		 inputObject.removeField("errorType");
   		 		 inputObject.put("errorType", lowercaseEType);
   		 		 
   		 		 LOGGER.debug("Converting audit's envid value to uppercase and converting application, transaction domain, transaction type, severity, and error type values to lowercase");
   		 		
				 inputObject.removeField("dataLocation");
			     inputObject.put("dataLocation", referenceID.toString());
	    		 LOGGER.debug("Updating the audit with new datalocation: " + referenceID.toString());
		         if (!inputObject.containsField("timestamp"))
		         {
		        	 LOGGER.debug("Audit does not contain timestamp");
		         }
		         String timestamp =  inputObject.get("timestamp").toString();
		 	     Date gtDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(timestamp);
		         inputObject.removeField("timestamp");
		         inputObject.put("timestamp",gtDate);
			     context.setContent(inputObject);
			     MongoClient client = getMongoConnection(exchange, context);
				 DB db = client.getDB(MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database"));
				 DBCollection collection = db.getCollection(MongoDBClientSingleton.getErrorSpotConfig("u-audit-collection"));
				 LOGGER.trace("Updated audit to be inserted: " + inputObject);
				 collection.insert(inputObject);
				 DBObject query = new BasicDBObject("dataLocation", referenceID.toString());
				 DBCursor cursor = collection.find(query);
				 String auditID = "";
				 while (cursor.hasNext()) {
					 
					 BasicDBObject obj = (BasicDBObject) cursor.next();
					 auditID =  obj.getString("_id");
					 
				 }
				 if (!auditID.equals(""))
					 LOGGER.debug("Audit successfully inserted with Object ID:" + auditID);

			     status = "Success";
			     
			     /*=======Immediate Notification for audits=======*/
			     //Fetch setting document
			     String auditContent = "";
			     
			     String auditSeverity = "";
			     String auditName = "";
			     String auditInterface = "";
			     String auditEnvid = "";
				 
			     //Obtain fields to be compared from audit if they exist
				 if (inputObject.containsField("envid")) {
			    	 auditEnvid = inputObject.get("envid").toString();
			    	 LOGGER.debug("Audit envid: " + auditEnvid + ", to be compared for Immediate Notification");
			     }
				 
			     if (inputObject.containsField("severity")) {
			    	 auditSeverity = inputObject.get("severity").toString();
			    	 LOGGER.debug("Audit severity: " + auditSeverity + ", to be compared for Immediate Notification");
			     }
			     
			     if (inputObject.containsField("application")) {
			    	 auditName = inputObject.get("application").toString(); 
			    	 LOGGER.debug("Audit application name: " + auditName + ", to be compared for Immediate Notification");
			     }
			     
			     if (inputObject.containsField("interface1")) {
			    	 auditInterface = inputObject.get("interface1").toString(); 
			    	 LOGGER.debug("Audit interface: " + auditInterface + ", to be compared for Immediate Notification");
			     }
			     
			     if (ErrorSpotSinglton.isInitialized()) {
			     
			    	 JSONObject config = null;
			    	 config = ErrorSpotSinglton.getExpiredNotificationDetail(auditEnvid, auditName, auditInterface, auditSeverity);
			     
			    	 if (config != null)
			    	 {
			    		 String toEmailId = config.getString("email");			     
			    		 String template = config.getString("template");
			     
			    		 //Call NotificationService
			    		 auditContent = inputObject.toString();
			    		 String subject = "Audit Notification: Conditions: Application = " + auditName + ", Interface = " + auditInterface + ", Severity = " + auditSeverity;
			    		 LOGGER.debug("NotificationService called with template: " + template + ", email: " + toEmailId + ", subject: " + subject);
			    		 NotificationService.sendEmail(auditContent, template, toEmailId, subject);
			    		 LOGGER.debug("Notification sent to " + toEmailId); 
			    	 }
			    	 else
			    	 {
			    		 LOGGER.debug("No notification sent.");
			    	 } 
			
			     }
	    	}
	    catch(	java.text.ParseException e) 
		   {
		    	LOGGER.error("Date Not Correctly Formatted. Date Format is: YYYY-MM-DDTHH:MM:SS");
		    	e.printStackTrace();
		        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted Date. Accepted Date Format is: YYYY-MM-DDTHH:MM:SS");
			    MongoClient client = getMongoConnection(exchange, context);
			    DB db = client.getDB(MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database"));
			    DBCollection collection = db.getCollection(MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection"));
			    DBObject removalObject = new BasicDBObject("_id", referenceID);
			    collection.remove(removalObject);
			     status = "Failed";


		
		   }
	    catch(IllegalArgumentException e) 
		   {
		    	LOGGER.error("Date Not Correctly Formatted. Date Format is: YYYY-MM-DDTHH:MM:SS");
		    	e.printStackTrace();
		        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted JSON Array. Please check JSON Array Format");
			    MongoClient client = getMongoConnection(exchange, context);
			    DB db = client.getDB(MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database"));
			    DBCollection collection = db.getCollection(MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection"));
			    DBObject removalObject = new BasicDBObject("_id", referenceID);
			    collection.remove(removalObject);
			     status = "Failed";

		
		   }
		 catch(JSONParseException e) 
			   {
			    	LOGGER.error("Incorrectly Formated JSON Array. Please check JSON Array Format");
			    	e.printStackTrace();
			        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted JSON Array. Please check JSON Array Format");
				    MongoClient client = getMongoConnection(exchange, context);
				    DB db = client.getDB(MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database"));
				    DBCollection collection = db.getCollection(MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection"));
				    DBObject removalObject = new BasicDBObject("_id", referenceID);
				    collection.remove(removalObject);
				     status = "Failed";

			
			   }
			  
	    catch(MongoClientException e)
	    {
	    	LOGGER.error("MongoDB Client Error. Ensure that DB and Collection exist");
	    	LOGGER.error(e.getMessage());
	    	e.printStackTrace();
	    	status = "Failed";
	    	MongoClient client = getMongoConnection(exchange, context);
		    DB db = client.getDB(MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database"));
		    DBCollection collection = db.getCollection(MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection"));
		    DBObject removalObject = new BasicDBObject("_id", referenceID);
		    collection.remove(removalObject);
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "MongoDB Client Exception. Please check MongoDB Status");
		    status = "Failed";

	    }
		catch(MongoException e)
	    {
	    	LOGGER.error("General MongoDB Error. Please check MongoDB Connection and Permissions");
	    	LOGGER.error(e.getMessage());
	    	e.printStackTrace();
	    	status = "Failed";
	    	MongoClient client = getMongoConnection(exchange, context);
		    DB db = client.getDB(MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database"));
		    DBCollection collection = db.getCollection(MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection"));
		    DBObject removalObject = new BasicDBObject("_id", referenceID);
		    collection.remove(removalObject);
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "General MongoDB Error. Please check MongoDB Connection and Permissions");
		     status = "Failed";

	    }
	
	    catch(Exception e) 
	    {
	    	LOGGER.error("Unspecified Application Error" );
	    	e.printStackTrace();
	    	MongoClient client = getMongoConnection(exchange, context);
		    DB db = client.getDB(MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database"));
		    DBCollection collection = db.getCollection(MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection"));
		    DBObject removalObject = new BasicDBObject("_id", referenceID);
		    collection.remove(removalObject);
	    	status = "Failed";
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unspecified Application Error");
	        status = "Failed";

	    }
			     return status;		
			
	       
 	}
	
	private static String payloadInsert(ObjectId id, DBObject inputObject, RequestContext context, HttpServerExchange exchange) throws Exception
	{
		/*
		 *  Get MongoDB connection and insert the converted payload document. 
		 */
		String status = "";
		try {
			inputObject.put("_id", id);
		    MongoClient client = getMongoConnection(exchange, context);
		    DB db = client.getDB(MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database"));
		    DBCollection collection = db.getCollection(MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection"));
		    collection.insert(inputObject);
		    status = "Success";
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
