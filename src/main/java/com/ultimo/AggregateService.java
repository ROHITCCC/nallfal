package com.ultimo;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.hal.Representation;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.handlers.collection.CollectionRepresentationFactory;
import org.restheart.security.handlers.IAuthToken;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientException;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.util.JSON;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;



/*
 * 	Watch out for the $match and the other important changes that could happen with the new version of MongoDB when it is released.  
 */
public class AggregateService extends ApplicationLogicHandler implements IAuthToken {
	
	MongoClient db;
	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");

	public AggregateService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
	
	}
	
	
	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception {
	
        if (context.getMethod() == METHOD.OPTIONS) 
        {
        	ErrorSpotSinglton.optionsMethod(exchange);
        } 
        else if (context.getMethod() == METHOD.POST)
        {	

		
				String dbname = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
				String collectionName = MongoDBClientSingleton.getErrorSpotConfig("u-audit-collection");
				db = MongoDBClientSingleton.getInstance().getClient();
		        DB database = db.getDB(dbname);
		        DBCollection collection = database.getCollection(collectionName);
		    //============================================================================================================    
		    //  Get Payload  
		        try{ 
					InputStream input = exchange.getInputStream();
					BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
					String payload = "";
					readLoop : while(true)
							{
								String inputTemp = inputReader.readLine();
								if (inputTemp != null) 
								{
									payload = payload + inputTemp;
								}
								else 
								{
									break readLoop;
								}
							}
					
					LOGGER.debug("AggregateService executing");
					LOGGER.trace(payload);

					
			//===========================================================================================================	
//			// Begin Formatting Query Pipeline	
//					DBObject dbInputObject = null;
//			        List<DBObject> query = new ArrayList<DBObject>();
//					JSONArray inputArray = new JSONArray(payload);
//					for (int a = 0; a < inputArray.length(); a++)
//					{
//						JSONObject inputObject = (JSONObject) inputArray.get(a);
//					if (inputObject.has("$project"))
//				    {
//						dbInputObject = new BasicDBObject("$project",JSON.parse(inputObject.get("$project").toString()));
//				        query.add(dbInputObject); 	
//				    }
//					if (inputObject.has("$match"))
//			        {
//			        	dbInputObject = new BasicDBObject("$match",JSON.parse(inputObject.get("$match").toString()));
//			        	System.out.println("Match" + dbInputObject.get("$match").toString());
//
//			        	query.add(dbInputObject); 	
//			        }
//					if (inputObject.has("$group"))
//					 {
//			        	dbInputObject = new BasicDBObject("$group",JSON.parse(inputObject.get("$group").toString()));
//			        	query.add(dbInputObject); 	
//					 }
//					if (inputObject.has("$sort"))
//					 {
//			        	dbInputObject = new BasicDBObject("$sort",JSON.parse(inputObject.get("$sort").toString()));
//			        	query.add(dbInputObject);
//					 }
//					}
//					LOGGER.trace(query.toString());
//					LOGGER.debug("Query List Made");
//							        
//			     //==========================================================================================================
//			     // Send Query to DB
//			
//			        AggregationOutput output = collection.aggregate( query );	
//			        LOGGER.debug("Query Executed");
//			       /// System.out.println(output.)
//			     //==========================================================================================================
//			     // Process Results
//			        JSONArray resultsArray = new JSONArray(output.results().toString());
//			        List<DBObject> outputList = new ArrayList<DBObject>();
//			        int i = 0;
//			        while(i < resultsArray.length())
//			        {
//			        DBObject outputObject = (DBObject) JSON.parse(resultsArray.get(i).toString());
//			        outputList.add(outputObject);
//			        	i++;
//			        }
//			        if (resultsArray.length() == 0)
//			        {
//			        	LOGGER.debug("No Results Were Found.");
//			        }
//			        for(DBObject b : outputList)
//			        {
//			        	//System.out.println(b);
//			        	LOGGER.trace(b.toString());
//			        }
//			        
					//call method for aggregate
					List<DBObject> outputList = executeMongoAggregate(payload, collection);
					
			        
			        CollectionRepresentationFactory data =  new CollectionRepresentationFactory();			        
			        Representation response = data.getRepresentation(exchange, context, outputList, outputList.size());
			        LOGGER.debug("Results Transformed into RestHeart Represenation");

			        int code = HttpStatus.SC_ACCEPTED;
			      //==========================================================================================================
				  // Send Response Back
			        
			        exchange.setResponseCode(code);
			        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, Representation.HAL_JSON_MEDIA_TYPE);
			        exchange.getResponseSender().send(response.toString());
			        exchange.endExchange();
				}
		        catch(JSONException e) 
		        {
		        	LOGGER.error("Incorrectly Formated JSON Array. Please check JSON Array Format");
		        	
		            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted JSON Array. Please check JSON Array Format");
		            
		            e.printStackTrace();
		            
		
		        }
		        catch(MongoCommandException e) 
		        {
		        	LOGGER.error("Bad MongoDB Request. Request Errored Out");
		        	LOGGER.error(e.getMessage());

		            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "Bad MongoDB Request. Please rephrase your command.");
		            e.printStackTrace();
		
		        }       
		        
		        catch(MongoTimeoutException e)
		        {
		        	LOGGER.error("MongoDB Connection Timed Out. Please check MongoDB Status and try again ");
		        	LOGGER.error(e.getMessage());

		            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "MongoDB Connection TimedOut");
		            e.printStackTrace();

		        }
		        catch(MongoClientException e)
		        {
		        	LOGGER.error("MongoDB Client Error. Ensure that DB and Collection exist");
		        	LOGGER.error(e.getMessage());

		            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "MongoDB Client Exception. Please check MongoDB Status");
		            e.printStackTrace();

		        }
		        catch(MongoException e)
		        {
		        	LOGGER.error("General MongoDB Exception");
		        	LOGGER.error(e.getMessage());

		            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "General MongoDB Error");
		            e.printStackTrace();

		        }

		        catch(Exception e) 
		        {
		        	LOGGER.error("Unspecified Application Error" );
		        	e.printStackTrace();


		            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unspecified Application Error");

		        }
		       
       
        }
        else 
    	{
        	ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_METHOD_NOT_ALLOWED, "Method Not Allowed. Post Only ");
    	}
	
	
	
}
	//cal mongo aggregate 
	public static List<DBObject> executeMongoAggregate(String payload, DBCollection collection ){
		// Begin Formatting Query Pipeline	
		DBObject dbInputObject = null;
        List<DBObject> query = new ArrayList<DBObject>();
		JSONArray inputArray = new JSONArray(payload);
		for (int a = 0; a < inputArray.length(); a++)
		{
			JSONObject inputObject = (JSONObject) inputArray.get(a);
		if (inputObject.has("$project"))
	    {
			dbInputObject = new BasicDBObject("$project",JSON.parse(inputObject.get("$project").toString()));
	        query.add(dbInputObject); 	
	    }
		if (inputObject.has("$match"))
        {
        	dbInputObject = new BasicDBObject("$match",JSON.parse(inputObject.get("$match").toString()));
        	//System.out.println("Match" + dbInputObject.get("$match").toString());
        	query.add(dbInputObject); 	
        }
		if (inputObject.has("$group"))
		 {
        	dbInputObject = new BasicDBObject("$group",JSON.parse(inputObject.get("$group").toString()));
        	query.add(dbInputObject); 	
		 }
		if (inputObject.has("$sort"))
		 {
        	dbInputObject = new BasicDBObject("$sort",JSON.parse(inputObject.get("$sort").toString()));
        	query.add(dbInputObject);
		 }
		}
		LOGGER.trace(query.toString());
				        
     //==========================================================================================================
     // Send Query to DB
        LOGGER.debug("Query Executed: " + query.toString());
        AggregationOutput output = collection.aggregate( query );	
       /// System.out.println(output.)
     //==========================================================================================================
     // Process Results
        JSONArray resultsArray = new JSONArray(output.results().toString());
        List<DBObject> outputList = new ArrayList<DBObject>();
        int i = 0;
        while(i < resultsArray.length())
        {
        DBObject outputObject = (DBObject) JSON.parse(resultsArray.get(i).toString());
        outputList.add(outputObject);
        	i++;
        }
        if (resultsArray.length() == 0)
        {
        	LOGGER.debug("No Results Were Found.");
        }
        for(DBObject b : outputList)
        {
        	LOGGER.trace(b.toString());
        }
        
        return outputList;
	}
}