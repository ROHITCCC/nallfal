package com.ultimo;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.util.Map;
import org.bson.types.ObjectId;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.hal.Representation;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
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

public class PayloadService extends ApplicationLogicHandler{

	private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart");
	
	public PayloadService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception 
	{
	
		if (context.getMethod() == METHOD.GET)
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
			DB db = client.getDB(context.getDBName());
			DBCollection collection = db.getCollection(context.getCollectionName());
			String objectID = exchange.getQueryString().replace("id=", "");
			LOGGER.trace("Requested Object ID"+objectID);
			ObjectId queryObjectId = new ObjectId(objectID);
			DBObject resultDocument = collection.findOne(new BasicDBObject("_id",queryObjectId));
			LOGGER.debug("Query Executed");
			String result = (resultDocument.get("payload").toString()).replace("&quot;", "\"");
			LOGGER.trace("Query Result"+result);
			int code = HttpStatus.SC_ACCEPTED;
	        exchange.setResponseCode(code);
	        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, Representation.HAL_JSON_MEDIA_TYPE);
	        exchange.getResponseSender().send(result);
			LOGGER.debug("Result Sent to UI");

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

}
