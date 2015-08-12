package com.ultimo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.hal.Representation;
import org.restheart.handlers.IllegalQueryParamenterException;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.handlers.collection.CollectionRepresentationFactory;
import org.restheart.handlers.document.DocumentRepresentationFactory;
import org.restheart.security.handlers.IAuthToken;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientException;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.WriteConcernException;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

public class SettingService extends ApplicationLogicHandler implements IAuthToken{

	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");
	
	MongoClient client;
	
	public SettingService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
		
		 if (args == null) {
	            throw new IllegalArgumentException("args cannot be null");
	        }
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,
			RequestContext context) throws Exception {
		try{
			//connect to server-------------------------------------------------------------------------
			String dbname = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
			String collectionName = MongoDBClientSingleton.getErrorSpotConfig("u-setting-collection");
			client = MongoDBClientSingleton.getInstance().getClient();
			DB database = client.getDB(dbname);
			DBCollection collection = database.getCollection(collectionName);
			
			if (context.getMethod() == METHOD.OPTIONS) {
				ErrorSpotSinglton.optionsMethod(exchange);
	        } 
	        else if (context.getMethod() == METHOD.GET){
	        	LOGGER.info("Request has been recieved to GET a document from  the database: "+dbname+" and collection: "+collectionName);
				Map<String,Deque<String>> queryParams= exchange.getQueryParameters();
				LOGGER.trace("the queries in the map:"+ queryParams.toString());
				
				//return all the document if there is no query---------------------------------------------
				if(queryParams.keySet().size()==0){
					List<DBObject> resultList= new ArrayList<>(); // to hold documents
					DBCursor cursor = collection.find(); 
					if(!cursor.hasNext()){
						//return messege if no document are in the given collection
						exchange.getResponseSender().send("no document to display in given collection");
						LOGGER.info("no document to display in given collection");
						return;
					}
					else{
						LOGGER.info("displaying all documents in the given collecction");
						while (cursor.hasNext()) {
							resultList.add(cursor.next());
						}
						displayCollection(exchange,context,resultList);
						return;
					}
				}
				
				//validations-------------------------------------------------------------------------------
				if(!(queryParams.containsKey("object")) && !(queryParams.containsKey("id"))){
					//checks to see if object is passed as a parameter in the url.error if not
					LOGGER.error("object or id not detected in the url. Only an object or id needs to be specified after the url, preceded with: \"?object=\" or \"?id=\"");
					ResponseHelper.endExchangeWithMessage(exchange, 404, "An object or id needs to be specified, preceded with: \"object=\" or \"id=\"");
					return;
				}
				if(queryParams.containsKey("object") && queryParams.get("object").size()!=1 || queryParams.containsKey("id") && queryParams.get("id").size()!=1){
					//check to see if more than one object is passed as a perameter. error if so
					LOGGER.error("only one object or one id can be querried, but now more than one object or id is being querrired, or no objet or id is being querried. ");
					ResponseHelper.endExchangeWithMessage(exchange, 404, "only one object or id can be querried");
					return;
				}
				//handle id--------------------------------------------------------------------------------
				if((queryParams.containsKey("id"))){
					LOGGER.info("you are querying for a document based on given Id: "+queryParams.get("id").getFirst());
					handleId(exchange,context,collection,queryParams.get("id").getFirst());
					return;
				}
				
				//handle object-----------------------------------------------------------------------------
				//gets the request value of object= and splits it at a period, and passes it to array filter condition
				String filterConditions[]=queryParams.get("object").getFirst().split("\\.");
				LOGGER.info("you are querying for a "+filterConditions[0]+" object");
				//uses switch case for the first elements of the filter condition
				switch (filterConditions[0]){
				case "setting" : handleSetting(exchange,context,collection,filterConditions); break;
				case "report" : handleReport(exchange,context,collection,queryParams); break;
				default : 
					LOGGER.error("no switch case to handle given object: "+filterConditions[0]);
					ResponseHelper.endExchangeWithMessage(exchange, 404, "given object is invalid");
					return;
				}
			}
	        else if(context.getMethod()==METHOD.POST){
	        	LOGGER.info("Request has been recieved to POST a document into  the database: "+dbname+" and collection: "+collectionName);
				//insert payload/document into the mongodb--------------------------------------------------
	        	DBObject document = convertPayloadToDocument(exchange);
				// this is used for validations for duplicate insertions of setting and reports
				BasicDBObject whereQuery = new BasicDBObject();
				if(document.get("setting")!=null){
					LOGGER.info("document is a setting");
					//overwrite setting if it exists. If it doesn't exist, adds a new one
					whereQuery.put("setting", new BasicDBObject("$ne", null));
					//LOGGER.trace(whereQuery.toString());
					LOGGER.trace("Searching for exisiting setting document");
					LOGGER.trace("Searching for setting document with given qualifications:"+whereQuery.toString());
					DBCursor cursor = collection.find(whereQuery);
					if(cursor.size()!=0){
						//if there is already a document with the given fields and values as whereQuery, 
						//overwrite the document.
						LOGGER.info("a setting document already exists");
						DBObject doc = cursor.next();
						LOGGER.info("replacing exisiting document with the new one");
						collection.findAndRemove(doc);
					}
					collection.insert(document);
					exchange.getResponseSender().send(document.get("_id").toString());
					LOGGER.info("successfully inserted a setting document");
					LOGGER.trace("document: "+document.toString());
				}
				else if (document.get("report")!=null ){
					LOGGER.info("document is determined to be a report");
					//if id is passed in and if a document with a given id already exists, update it
					LOGGER.debug("checking to see if the report has a template field");
					if(!validateReport(document,exchange)){
						return;
					}
					if(document.get("_id")!=null){
						LOGGER.info("a report document is passed with Id: "+document.get("_id").toString());
					}
					if(document.get("_id")!=null && collection.findOne(document.get("_id"))!=null ){
						LOGGER.info("The given document exists in the databse");
						DBObject tempDocument=collection.findOne(document.get("_id"));
						collection.update(tempDocument, document);
						exchange.getResponseSender().send(document.get("_id").toString());
						LOGGER.info("successfully updated the new document. scheduler will update the job");
					}
					else{
						LOGGER.info("The given document does not already exsist in the databse, adding a new one");
						LOGGER.trace(document.toString());
						collection.insert(document);
						exchange.getResponseSender().send(document.get("_id").toString());
						LOGGER.info("successfuly inserted document with the id: "+document.get("_id").toString());
					}
					if(SchedulerService.scheduleReport(new JSONObject(document.toString()))==null){
						LOGGER.error("couldn't schedule the document");
					}
				}
				else {
					LOGGER.error("unsuported object type "+document.toString());
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "Object type is not supported");
					return;
				}
				exchange.getResponseSender().send(document.get("_id").toString());
			}
	        else if(context.getMethod() == METHOD.DELETE){
	        	LOGGER.info("Request has been recieved to DELETE a document from  the database: "+dbname+" and collection: "+collectionName);
				DBObject document = convertPayloadToDocument(exchange);
				if(document.get("_id")!=null){
					LOGGER.trace("attempting to delete document based on document's id: "+document.get("_id").toString());
					WriteResult result=collection.remove(document);
					if(result.getN()==1){
						LOGGER.info("The document is removed");
						SchedulerService.deleteJob(new JSONObject(document.toString()));
						exchange.getResponseSender().send("scheduled job is deleted");
					}
					else{
						LOGGER.error("The given document could not be deleted because it is not in the database");
						ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_FOUND, "No document exists in database to delete");
					}
				}
				else{
					LOGGER.error("An Id needs to be specified for the document to be deleted, and currently no id is given");
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_FOUND, "Report needs to have id");
				}
	        }
	        else 
	        {
	        	ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_METHOD_NOT_ALLOWED, "Method Not Allowed. Post Only ");
	        }
		}
		catch(JSONParseException e) 
        {
        	LOGGER.error("Incorrectly Formated JSON Array. Please check JSON Array Format");
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted JSON Array. Please check JSON Array Format");
            LOGGER.error("the error: ",e);
        }
        catch(MongoCommandException e) 
        {
        	LOGGER.error("Bad MongoDB Request. Request Errored Out");
        	LOGGER.error(e.getMessage());
        	LOGGER.error("the error: ",e);
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "Bad MongoDB Request. Please rephrase your command.");
        }       
        
        catch(MongoTimeoutException e)
        {
        	LOGGER.error("MongoDB Connection Timed Out. Please check MongoDB Status and try again ");
        	LOGGER.error(e.getMessage());
        	LOGGER.error("the error: ",e);
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "MongoDB Connection TimedOut");
        }
        catch(MongoClientException e)
        {
        	LOGGER.error("MongoDB Client Error. Ensure that DB and Collection exist");
        	LOGGER.error(e.getMessage());
        	LOGGER.error("the error: ",e);
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "MongoDB Client Exception. Please check MongoDB Status");
        }
		catch(WriteConcernException e)
        {
        	LOGGER.error("The remove or update failed");
        	LOGGER.error(e.getMessage());
        	LOGGER.error("the error: ",e);
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "The remove or update failed");
        }
		catch(SchedulerException e)
        {
        	LOGGER.error("could not get the scheduler to schedule the job");
        	LOGGER.error(e.getMessage());
        	LOGGER.error("the error: ",e);
        }
        catch(MongoException e)
        {
        	LOGGER.error("General MongoDB Exception");
        	LOGGER.error(e.getMessage());
        	LOGGER.error("the error: ",e);
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "General MongoDB Error");
        }
        catch(Exception e) 
        {
        	LOGGER.error("Unspecified Application Error" );
        	LOGGER.error("the error: ",e);
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unspecified Application Error");
        }
	}
	
	public void handleSetting(HttpServerExchange exchange, RequestContext context, DBCollection collection, String[] qList ) throws IllegalQueryParamenterException{
		BasicDBObject whereQuery = new BasicDBObject();
		//this gets only the setting
		whereQuery.put("setting", new BasicDBObject("$ne", null));
		DBCursor cursor = collection.find(whereQuery);
		LOGGER.info("retrieving the setting document");
		//makes sure only one document is being queried
		if(cursor.size()!=1){
			LOGGER.error("there can only be one Setting queried");
			ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_CONFLICT, "Setting object is more than one or doesnt exist");
			return;
		}
		DBObject document = (DBObject) cursor.next();
		LOGGER.trace("the setting document:"+document);
		String id = document.get("_id").toString(); // this is used later for HAL representation
		boolean hasQuery=true; //checks to see if the given query is in Setting
		for(String query: qList){
			//parse through each query from qList(the array that was split by the period)
			LOGGER.trace("searching for: "+query+" within "+document);
    		if(document.get(query)==null){
    			LOGGER.info("document doesn't contain: "+ query);
    			hasQuery=false;
    			break;
    		}
    		else{
    			LOGGER.info("document contains: "+ query);
    			//document now is the query within the previous document DBObject. narrows scope of document
    			document=(DBObject)document.get(query);
    		}
    	}
    	if(hasQuery){
    		//if the match is found, the contents of the search only NOT THE ENTIRE DOCUMENT
    		List<DBObject> resultList=new ArrayList<>();
    		BasicDBObject result = new BasicDBObject();
    		result.put(qList[qList.length-1],document);
    		result.put("_id", id); // id added for HAL representation
    		resultList.add(result);
    		displayCollection(exchange,context,resultList);
    	}
    	else{
    		LOGGER.error("could not find given field in Setting");
    		ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST,"Could not find given field in Setting" );
    	}
	}
	
	public void handleReport(HttpServerExchange exchange, RequestContext context, DBCollection collection, Map<String,Deque<String>> queries ) throws IllegalQueryParamenterException{
		queries.remove("object"); //removes the object key and value (Since document doesn't contain such field)
		BasicDBObject whereQuery = new BasicDBObject(); //used to create a DBObject to search for in collection
		Map<String,String> queryMap=new HashMap<String,String>(); //adds all the field to the DBObject
		for(String key: queries.keySet()){
			//converts the deque<String> param to a string by getting first element
			String value=queries.get(key).getFirst();
			key="report."+key; //this is because the queries are all within report
			queryMap.put(key,value);
		}
		whereQuery.putAll(queryMap);
		//check if where query is empty and if so, make sure it only displays reports
		if(whereQuery.isEmpty()){
			whereQuery.put("report", new BasicDBObject("$ne", null));
		}
		LOGGER.info("searching for report documents with the given fields and values: "+whereQuery.toString());
		DBCursor cursor = collection.find(whereQuery);
		List<DBObject> resultList= new ArrayList<>();
	    while (cursor.hasNext()) {
	    	resultList.add(cursor.next());
	    }
	    if(resultList.size()==0){
	    	//error if no reports are found
	    	LOGGER.error("no reports were found");
	    	ResponseHelper.endExchangeWithMessage(exchange, 404, "no match found");
	    	return;
	    }
	    else{
	    	//prints all the returned documents that matched the given conditions
	    	LOGGER.info("found "+resultList.size()+" report documents that contain the passed key-value pairs");
	    	displayCollection(exchange, context,resultList);
	    }
	}
	
	public void handleId(HttpServerExchange exchange, RequestContext context, DBCollection collection, String Id) throws IllegalQueryParamenterException{
		if(collection.findOne(new ObjectId(Id))!=null){
			List<DBObject> resultList= new ArrayList<>();
		    resultList.add(collection.findOne(new ObjectId(Id)));
		    LOGGER.info("document found with id: "+Id);
	    	displayCollection(exchange, context,resultList);
	    	return;
		}
		else{
			LOGGER.error("no documents were found with that given Id: "+ Id);
	    	ResponseHelper.endExchangeWithMessage(exchange, 404, "no match found");
	    	return;
		}
	}
	
	public void displayCollection(HttpServerExchange exchange, RequestContext context, List<DBObject> outputList) throws IllegalQueryParamenterException{
		//displays content in HAL representation on the webpage
		LOGGER.info("displaying the documents as part of collection in HAL format");
		CollectionRepresentationFactory data =  new CollectionRepresentationFactory();			        
		Representation response = data.getRepresentation(exchange, context, outputList, outputList.size());
		LOGGER.trace("Results Transformed into RestHeart Represenation");
		int code = HttpStatus.SC_ACCEPTED;
		exchange.setResponseCode(code);
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, Representation.HAL_JSON_MEDIA_TYPE);
		exchange.getResponseSender().send(response.toString());
		exchange.endExchange();
		LOGGER.debug("Response has been Sent and Exchange has been Closed");
		return;
	}
	
	public boolean validateReport(DBObject document,HttpServerExchange exchange){
		String template=((DBObject)document.get("report")).get("template").toString();
		if(template==null){ 
			LOGGER.error("The report document does not have a template field");
			ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_FOUND, "The report document is missing a template field");
			return false;
		}
		LOGGER.debug("the document does have a template field: "+template);
		boolean templateExists=NotificationService.validateTemplate(template);
		if(templateExists){
			return true;
		}
		else{
			LOGGER.error("the template: "+template+" could not be found");
			ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_FOUND, "The passed report document has an invalid template field");
			return false;
		}
	}
	
	public static DBObject convertPayloadToDocument(HttpServerExchange exchange) throws IOException,JSONParseException{
		InputStream input = exchange.getInputStream();
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
		String line = null;
		String payload = "";
		while((line = inputReader.readLine())!=null){
			payload += line;
		}
		LOGGER.trace("this is the passed document: " +payload);
		DBObject document = (DBObject) JSON.parse(payload);
		LOGGER.info("converted the payload to a DBObject");
		return document;
	}
}

		