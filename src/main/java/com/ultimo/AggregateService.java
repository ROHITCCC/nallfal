package com.ultimo;
import static org.restheart.hal.Representation.HAL_JSON_MEDIA_TYPE;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.db.PropsFixer;
import org.restheart.hal.Representation;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.handlers.collection.CollectionRepresentationFactory;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientException;
import com.mongodb.util.JSON;



/*
 * 	Watch out for the $match and the other important changes that could happen with the new version of MongoDB when it is released.  
 */
public class AggregateService extends ApplicationLogicHandler {
	
	MongoClient db;

	public AggregateService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
	
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception {
	
		String dbname = context.getDBName();
		String collectionName = context.getCollectionName();
		db = MongoDBClientSingleton.getInstance().getClient();
        DB database = db.getDB(dbname);
        DBCollection collection = database.getCollection(collectionName);
        
    //============================================================================================================    
    //  Get Payload  
        try{ 
		InputStream input = exchange.getInputStream();
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
		String payload = inputReader.readLine();
	//===========================================================================================================	
	// Begin Formatting Query Pipeline	
		DBObject dbSortObject = null;
		DBObject dbMatchObject = null;
		DBObject dbGroupObject = null;
		DBObject dbInputObject = null;
        List<DBObject> query = new ArrayList<DBObject>();
        System.out.println(payload);        
		JSONArray inputArray = new JSONArray(payload);
		for (int a = 0; a < inputArray.length(); a++)
		{
			JSONObject inputObject = (JSONObject) inputArray.get(a);
			System.out.println("Input object : " + inputObject.toString());	
		if (inputObject.has("$match"))
        {
        	dbInputObject = new BasicDBObject("$match",JSON.parse(inputObject.get("$match").toString()));
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
		
		
		System.out.println("Done");
        
     //==========================================================================================================
     // Send Query to DB

        AggregationOutput output = collection.aggregate( query );
       
        JSONArray resultsArray = new JSONArray(output.results().toString());
        List<DBObject> outputList = new ArrayList<DBObject>();
        
        System.out.println(resultsArray.toString());
        int i = 0;
        while(i < resultsArray.length())
        {
        System.out.println(resultsArray.get(i));
        DBObject outputObject = (DBObject) JSON.parse(resultsArray.get(i).toString());
        outputList.add(outputObject);
        	i++;
        }
       
        CollectionRepresentationFactory data =  new CollectionRepresentationFactory();
        Representation response = data.getRepresentation(exchange, context, outputList, resultsArray.length());
        String a = response.toString();
        int code = HttpStatus.SC_ACCEPTED;
        Throwable t = null;
            exchange.setResponseCode(code);
           
            String httpStatuText = HttpStatus.getStatusText(code);

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, Representation.HAL_JSON_MEDIA_TYPE);
            exchange.getResponseSender().send(response.toString());
            exchange.endExchange();
          
        
	}
        catch(JSONException e) 
        {
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted JSON Object. Please check Payload Format");
            e.printStackTrace();
            db.close();

        }
        catch(MongoClientException e) 
        {
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_FOUND, "Could Not Connect to MongoDB");
            e.printStackTrace();

        }       
        catch(Exception e) 
        {
        	e.printStackTrace();
        }

	}
	
	
	
	/* public DBObject getCollectionProps(String dbName, String collName, boolean fixMissingProperties) {
	        DBCollection propsColl = getCollection(dbName, "_properties");

	        DBObject properties = propsColl.findOne(new BasicDBObject("_id", "_properties.".concat(collName)));

	        if (properties != null) {
	            properties.put("_id", collName);

	            Object etag = properties.get("_etag");

	            if (etag != null && etag instanceof ObjectId) {
	                properties.put("_lastupdated_on", Instant.ofEpochSecond(((ObjectId) etag).getTimestamp()).toString());
	            }
	        } else if (fixMissingProperties) {
	            new PropsFixer().addCollectionProps(dbName, collName);
	            return getCollectionProps(dbName, collName, false);
	        }

	        return properties;
	    }*/

}