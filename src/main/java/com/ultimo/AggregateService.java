package com.ultimo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.undertow.server.HttpServerExchange;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
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

public class AggregateService extends ApplicationLogicHandler {

	public AggregateService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception {
		String dbname = context.getDBName();
		String collectionName = context.getCollectionName();

		MongoClient db = new MongoClient( "172.16.120.70" , 27017 );
		System.out.println(exchange.getHostName() + " " + exchange.getHostPort());
        DB database = db.getDB(dbname);
        DBCollection collection = database.getCollection(collectionName);
        
    //============================================================================================================    
    //  Get Payload  
        try{
		InputStream input = exchange.getInputStream();
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
		String payload = inputReader.readLine();
		System.out.println(payload);
	//===========================================================================================================	
	// Begin Formatting Query Pipeline	
		DBObject dbInputObject;
        List<DBObject> query = new ArrayList<DBObject>();
        
        
		JSONArray inputArray = new JSONArray(payload);
		for (int a = 0; a < inputArray.length(); a++)
		{
			JSONObject inputObject = (JSONObject) inputArray.get(a);
		if (inputObject.has("$match"))
        {
        	System.out.println(inputObject.get("$match").toString());
        	dbInputObject = new BasicDBObject("$match",JSON.parse(inputObject.get("$match").toString()));
        	query.add(dbInputObject);
        	
        }
		else if (inputObject.has("$group"))
		 {
        	System.out.println(inputObject.get("$group").toString());
        	dbInputObject = new BasicDBObject("$group",JSON.parse(inputObject.get("$group").toString()));
        	query.add(dbInputObject);
        	
		 }
		else if (inputObject.has("$sort"))
		 {
        	System.out.println(inputObject.get("$sort").toString());
        	dbInputObject = new BasicDBObject("$sort",JSON.parse(inputObject.get("$sort").toString()));
        	query.add(dbInputObject);
        	
		 }
		
        else 
        {
        	System.out.println("in Else");
        }
		}		
		System.out.println("Done");
        
        
     //==========================================================================================================
     // Send Query to DB
        
        AggregationOutput output = collection.aggregate( query );
        JSONArray resultsArray = new JSONArray(output.results().toString());
        int i = 0;
        while(i < resultsArray.length())
        {
        System.out.println(resultsArray.get(i));
        	i++;
        }
        db.close();
        
	}
        catch(JSONException e) 
        {
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Incorrectly Formatted JSON Object. Please check Payload Format");
            e.printStackTrace();

        }
        catch(MongoClientException e) 
        {
            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_FOUND, "Could Not Connect to MongoDB");
            e.printStackTrace();

        }       

	}

}