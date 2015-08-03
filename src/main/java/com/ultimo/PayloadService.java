package com.ultimo;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import org.json.JSONException;
import org.json.JSONML;
import org.json.JSONObject;
import org.restheart.security.handlers.IAuthToken;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;

import java.util.Map;

import org.bson.types.ObjectId;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.hal.Representation;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
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


public class PayloadService extends ApplicationLogicHandler implements IAuthToken{

	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");
	
	public PayloadService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception 
	{
		 if (context.getMethod() == METHOD.OPTIONS) 
	        {
			 	ErrorSpotSinglton.optionsMethod(exchange);
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

	
	private static void getData(HttpServerExchange exchange,RequestContext context) 
	{
		try
		{
			MongoClient client = getMongoConnection(exchange,context);
			DB db = client.getDB(MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database"));
			DBCollection collection = db.getCollection(MongoDBClientSingleton.getErrorSpotConfig("u-payload-collection"));
			String objectID = exchange.getQueryString().replace("id=", "");
			LOGGER.trace("Requested Object ID"+objectID);
			ObjectId queryObjectId = new ObjectId(objectID);
			DBObject resultDocument = collection.findOne(new BasicDBObject("_id",queryObjectId));
			LOGGER.debug("Query Executed");
			if ((resultDocument == null) || (resultDocument.toString().isEmpty()))
			{
		        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NO_CONTENT, "No Document Was Returned");

			}
			else
			{
				String result = jsonToPayload(resultDocument);
				LOGGER.trace("Query Result"+result);
				int code = HttpStatus.SC_ACCEPTED;
		        exchange.setResponseCode(code);
		        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, Representation.HAL_JSON_MEDIA_TYPE);
		        exchange.getResponseSender().send(result);
				LOGGER.debug("Result Sent to UI");
			}
			

		}
		
		
	    catch(MongoClientException e)
	    {
	    	LOGGER.error("MongoDB Client Error. Ensure that DB and Collection exist");
	    	LOGGER.error(e.getMessage());
	    	e.printStackTrace();
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "MongoDB Client Exception. Please check MongoDB Status");
	
	    }
		catch(MongoException e)
	    {
	    	LOGGER.error("General MongoDB Error. Please check MongoDB Connection and Permissions");
	    	LOGGER.error(e.getMessage());
	    	e.printStackTrace();

	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "General MongoDB Error. Please check MongoDB Connection and Permissions");
	
	    }
	
	    catch(Exception e) 
	    {
	    	LOGGER.error("Unspecified Application Error" );
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Unspecified Application Error");
	        e.printStackTrace();

	    }
		
	}
	private static MongoClient getMongoConnection(HttpServerExchange exchange, RequestContext context) 
	{
		MongoClient client = MongoDBClientSingleton.getInstance().getClient();   
		return client;
	}

	protected static DBObject payloadtoJSON (String input, String contentType,HttpServerExchange exchange, RequestContext context )
	{
		DBObject output = null;

		try{
			if (contentType.equalsIgnoreCase("application/xml"))
			{
				JSONObject obj =  JSONML.toJSONObject(input);
				output = (DBObject) JSON.parse(obj.toString());
				output.put("errorSpotContentType","application/xml");
			}
			else if (contentType.equalsIgnoreCase("application/json"))
			{
				output = (DBObject) JSON.parse(input);
				output.put("errorSpotContentType","application/json");
	
			}
			else if (contentType.equalsIgnoreCase("text/plain"))
			{
		        String intermediateJSON = "{\"payload\" : \""+ input.replace("\"", "&quot;") + "\"}";
		        output = (DBObject) JSON.parse(intermediateJSON);
				output.put("errorSpotContentType","text/plain");
	
				
			}
		
		}
		catch(JSONParseException e)
		{
			LOGGER.error("Incorrectly Formated JSON Object. Please check JSON Object Format");
	        throw new PayloadConversionException();
		}
		catch(JSONException e)
		{
			LOGGER.error("Unable to Format Document");
	        throw new PayloadConversionException();
		}
		
		
		
		return output;
		
	}

	protected static String jsonToPayload(DBObject inputObject)
	{
		
		String contentType = inputObject.get("errorSpotContentType").toString();
		String output = "";
		if (contentType.equalsIgnoreCase("application/json"))
		{
			inputObject.removeField("errorSpotContentType");
			inputObject.removeField("_id");
			output = inputObject.toString();
		}
		else if (contentType.equalsIgnoreCase("application/xml"))
		{
			inputObject.removeField("errorSpotContentType");
			inputObject.removeField("_id");
			output = inputObject.toString();
			JSONObject obj = new JSONObject(output);
			output = JSONML.toString(obj);
		}
		else if (contentType.equalsIgnoreCase("text/plain"))
		{
			inputObject.removeField("errorSpotContentType");
			output = inputObject.get("payload").toString().replace("&quot;", "\"");
		}
		return output;
		
	}
	
}
