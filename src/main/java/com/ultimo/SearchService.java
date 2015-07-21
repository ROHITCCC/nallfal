package com.ultimo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONObject;
import org.restheart.db.DbsDAO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientException;
import com.mongodb.MongoException;

public class SearchService extends ApplicationLogicHandler implements IAuthToken 
{

	MongoClient db;
	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");

	public SearchService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception {

		if (context.getMethod() == METHOD.OPTIONS) 
		{
			ErrorSpotSinglton.optionsMethod(exchange);
		} 
		else if (context.getMethod() == METHOD.GET)
		{
			try
			{
			List<DBObject> output = new ArrayList<DBObject>();
			List<Object> outputList = new ArrayList<Object>();
			if (exchange.getQueryParameters().get("searchtype").getFirst().equalsIgnoreCase("advanced") & exchange.getQueryParameters().get("searchdb").getFirst().equalsIgnoreCase("payload"))
			{
				outputList = advancedSearchPayload(exchange, context);
			}
			else if (exchange.getQueryParameters().get("searchtype").getFirst().equalsIgnoreCase("advanced") & exchange.getQueryParameters().get("searchdb").getFirst().equalsIgnoreCase("audit"))
			{
				outputList = advancedSearchAudit(exchange, context);
			}
			else if (exchange.getQueryParameters().get("searchtype").getFirst().equalsIgnoreCase("basic") & exchange.getQueryParameters().get("searchdb").getFirst().equalsIgnoreCase("audit"))
			{
				outputList = basicSearchAudit(exchange, context);
			}
			else if (exchange.getQueryParameters().get("searchtype").getFirst().equalsIgnoreCase("basic") & exchange.getQueryParameters().get("searchdb").getFirst().equalsIgnoreCase("payload"))
			{
				outputList = basicSearchPayload(exchange, context);
			}
			output = (List<DBObject>) outputList.get(0);
			long size = Long.parseLong(outputList.get(1).toString());
			CollectionRepresentationFactory data = new CollectionRepresentationFactory();
			Representation response = data.getRepresentation(exchange, context, output, size);
			int code = HttpStatus.SC_ACCEPTED;
			exchange.setResponseCode(code);
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,Representation.HAL_JSON_MEDIA_TYPE);
			exchange.getResponseSender().send(response.toString());
			exchange.endExchange();
			}
			catch(NumberFormatException e)
			{
				
				LOGGER.error("Cannot parse output. Search parameters are 'searchtype', 'searchdb', and either 'advanced' or 'basic'.");
				ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_ACCEPTABLE, "Cannot parse output. Search parameters are 'searchtype', 'searchdb', and either 'advanced' or 'basic'.");
				
			}
			
			catch(Exception e) 
		    {
		    	LOGGER.error("Unspecified Application Error" );

		    }
			
		} 
		else 
		{
			ResponseHelper.endExchangeWithMessage(exchange,
					HttpStatus.SC_METHOD_NOT_ALLOWED,
		
					"Method Not Allowed. Post Only ");
		}
	}
	
/*
	public static List<DBObject> executeMongoSearch(
			Map<String, Deque<String>> queryParams) {
		
		DBCollection collection;
		
		DbsDAO dao = new DbsDAO();
		List<DBObject> resultList= new ArrayList<>();
		Deque<String> filter = queryParams.get("filter");
		int pageSize = Integer.parseInt(queryParams.get("pagesize").getFirst());
		String searchCollection = queryParams.get("collection").getFirst();
		
		int page = 1;
		if(queryParams.get("page") != null)
			page = Integer.parseInt(queryParams.get("page").getFirst());
		
		Deque<String> sort = queryParams.get("sort");

		LOGGER.trace("Search filter data = " + filter.getFirst());
		
		if(searchCollection.equalsIgnoreCase("payload")){
			
			resultList = dao.getCollectionData(, page, pageSize,
					sort, filter, EAGER_CURSOR_ALLOCATION_POLICY.NONE);
	
			LOGGER.trace("Query result on payload collection " + resultList.toString());
			
		}else
		{
			
		}

		return resultList;
	}
*/
	public static ArrayList<Object> advancedSearchAudit(HttpServerExchange exchange, RequestContext context)
	{
		try
		{
		String payloadCollectionName = "";
		String auditCollectionName = "";
		String databaseName = "";
		String searchKeyword = "";
		int page = 1;
		int pagesize = 25;
		if (exchange.getQueryParameters().containsKey("page"))
		{
			page = Integer.parseInt(exchange.getQueryParameters().get("page").getFirst().toString());
		}
		if (exchange.getQueryParameters().containsKey("pagesize"))
		{
			pagesize = Integer.parseInt(exchange.getQueryParameters().get("pagesize").getFirst().toString());
		}
		List<Map<String, Object>> configList = MongoDBClientSingleton.getErrorSpotConfigs();
		databaseName = configList.get(0).get("where").toString();
		payloadCollectionName = configList.get(1).get("where").toString();
		auditCollectionName = configList.get(2).get("where").toString();
		System.out.println(databaseName);
		System.out.println(payloadCollectionName);
		System.out.println(auditCollectionName);
		MongoClient db = MongoDBClientSingleton.getInstance().getClient();
		DB database = db.getDB(databaseName);
		DBCollection auditCollection = database.getCollection(auditCollectionName);
		DbsDAO dao = new DbsDAO();

		Deque<String> filterQuery = exchange.getQueryParameters().get("filter");
		String filterString = filterQuery.getFirst();
		JSONObject intermediateQuery = new JSONObject(filterString);
		if (exchange.getQueryParameters().containsKey("searchkeyword"))
		{
			Deque<String> searchKey = exchange.getQueryParameters().get("searchkeyword");
			searchKeyword= searchKey.getFirst();

		}
		Deque<String> filter = new ArrayDeque<String>();
		
		filter.add(intermediateQuery.toString());
		if (exchange.getQueryParameters().containsKey("searchkeyword"))
		{
			filter.add("{\"$text\" : { \"$search\" : \"" + searchKeyword + "\" }}" );
		}

		List<DBObject> resultList = dao.getCollectionData(auditCollection, page,pagesize, null, filter, null);
		long size = dao.getCollectionSize(auditCollection, filter);
		ArrayList<Object> outputArray = new ArrayList<Object>();
		outputArray.add(resultList);
		outputArray.add(new Long(size));
		return outputArray;
		}
		catch(MongoClientException e)
	    {
	    	LOGGER.error("MongoDB Client Error. Ensure that DB and Collection exist");
	    	LOGGER.error(e.getMessage());
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "MongoDB Client Exception. Please check MongoDB Status");

	    }
		catch(MongoException e)
	    {
	    	LOGGER.error("General MongoDB Error. Please check MongoDB Connection and Permissions");
	    	LOGGER.error(e.getMessage());
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "General MongoDB Error. Please check MongoDB Connection and Permissions");

	    }
	
	    catch(Exception e) 
	    {
	    	LOGGER.error("Unspecified Application Error" );

	    }
		
		return null;
		
	}
	
	public static ArrayList<Object> advancedSearchPayload(HttpServerExchange exchange,RequestContext context)
	{
		try
		{
		String payloadCollectionName = "";
		String auditCollectionName = "";
		String databaseName = "";
		List<Map<String, Object>> configList = MongoDBClientSingleton.getErrorSpotConfigs();
		databaseName = configList.get(0).get("where").toString();
		payloadCollectionName = configList.get(1).get("where").toString();
		auditCollectionName = configList.get(2).get("where").toString();
		int page = 1;
		int pagesize = 25;
		if (exchange.getQueryParameters().containsKey("page"))
		{
			page = Integer.parseInt(exchange.getQueryParameters().get("page").getFirst().toString());
		}
		if (exchange.getQueryParameters().containsKey("pagesize"))
		{
			pagesize = Integer.parseInt(exchange.getQueryParameters().get("pagesize").getFirst().toString());
		}
		System.out.println(databaseName);
		System.out.println(payloadCollectionName);
		System.out.println(auditCollectionName);
//===================================================================================================================================================
		
		Deque<String> filterQuery = exchange.getQueryParameters().get("filter");
		String filterString = filterQuery.getFirst();
		JSONObject intermediateQuery = new JSONObject(filterString);
		Deque<String> searchKey = exchange.getQueryParameters().get("searchkeyword");
		String searchKeyword = searchKey.getFirst();
		Deque<String> filter = new ArrayDeque<String>();
		filter.add(intermediateQuery.toString());
		DbsDAO dao = new DbsDAO();
		List<DBObject> resultList = new ArrayList<DBObject>();
		MongoClient db = MongoDBClientSingleton.getInstance().getClient();
		DB database = db.getDB(databaseName);
		DBCollection auditCollection = database.getCollection(auditCollectionName);
		DBCollection payloadCollection = database.getCollection(payloadCollectionName);
		int size = (int) dao.getCollectionSize(auditCollection, filter);
		resultList = dao.getCollectionData(auditCollection, 1, size, null, filter, null);
		BasicDBList dataLocations = new BasicDBList();
		List<ObjectId> payloadIDs = new ArrayList<ObjectId>();
		for (DBObject s : resultList)
		{
			if(!s.get("dataLocation").toString().isEmpty())
			{
				
				payloadIDs.add(new ObjectId(s.get("dataLocation").toString()));
			}
		}
		dataLocations.addAll(payloadIDs);
//===================================================================================================================================================

		DBObject inClause = new BasicDBObject("$in",dataLocations);
		DBObject searchKeywordObject = new BasicDBObject("$search", searchKeyword);
		DBObject payloadQuery = new BasicDBObject("_id" , inClause);
		payloadQuery.put("$text",searchKeywordObject );
		DBCursor payloadResultCursor = payloadCollection.find(payloadQuery);
		List<String> resultPayloadIDs = new ArrayList<String>();
		
		while(payloadResultCursor.hasNext())
		{
			DBObject payload = payloadResultCursor.next();
			if (payload.containsField("_id"))
			{
				String ObjectID = payload.get("_id").toString();
				resultPayloadIDs.add(ObjectID);
			}
		}
		BasicDBList resultPayloadIDList = new BasicDBList();
		resultPayloadIDList.addAll(resultPayloadIDs);
		inClause = null;
		searchKeywordObject = null;
		payloadQuery = null;
		 inClause = new BasicDBObject("$in",resultPayloadIDList);
		 payloadQuery = new BasicDBObject("dataLocation" , inClause);
		 Deque<String> input = new ArrayDeque<String>();
		 input.add(payloadQuery.toString());
		 List<DBObject> outputList = dao.getCollectionData(auditCollection, page, pagesize, null, input, null);
			long size1 = dao.getCollectionSize(auditCollection, input);
			ArrayList<Object> outputArray = new ArrayList<Object>();
			outputArray.add(outputList);
			outputArray.add(new Long(size1));
			return outputArray;
		}
		catch(MongoClientException e)
	    {
	    	LOGGER.error("MongoDB Client Error. Ensure that DB and Collection exist");
	    	LOGGER.error(e.getMessage());
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "MongoDB Client Exception. Please check MongoDB Status");

	    }
		catch(MongoException e)
	    {
	    	LOGGER.error("General MongoDB Error. Please check MongoDB Connection and Permissions");
	    	LOGGER.error(e.getMessage());
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "General MongoDB Error. Please check MongoDB Connection and Permissions");

	    }
	
	    catch(Exception e) 
	    {
	    	LOGGER.error("Unspecified Application Error" );

	    }
		
		return null;
		
	}

	public static ArrayList<Object> basicSearchAudit(HttpServerExchange exchange, RequestContext context)
	{
	
		try
		{
		String payloadCollectionName = "";
		String auditCollectionName = "";
		String databaseName = "";
		int page = 1;
		int pagesize = 25;
		List<Map<String, Object>> configList = MongoDBClientSingleton.getErrorSpotConfigs();
		databaseName = configList.get(0).get("where").toString();
		payloadCollectionName = configList.get(1).get("where").toString();
		auditCollectionName = configList.get(2).get("where").toString();
		System.out.println(databaseName);
		System.out.println(payloadCollectionName);
		System.out.println(auditCollectionName);
		MongoClient db = MongoDBClientSingleton.getInstance().getClient();
		DbsDAO dao = new DbsDAO();
		DB database = db.getDB(databaseName);
		DBCollection auditCollection = database.getCollection(auditCollectionName);
		Deque<String> filterQuery = exchange.getQueryParameters().get("filter");
		if (exchange.getQueryParameters().containsKey("page"))
		{
			page = Integer.parseInt(exchange.getQueryParameters().get("page").getFirst().toString());
		}
		if (exchange.getQueryParameters().containsKey("pagesize"))
		{
			pagesize = Integer.parseInt(exchange.getQueryParameters().get("pagesize").getFirst().toString());
		}

		List<DBObject> resultList = dao.getCollectionData(auditCollection, page,pagesize, null, filterQuery, null);
		long size = dao.getCollectionSize(auditCollection, filterQuery);
		ArrayList<Object> outputArray = new ArrayList<Object>();
		outputArray.add(resultList);
		outputArray.add(new Long(size));
		return outputArray;
		}
		catch(MongoClientException e)
	    {
	    	LOGGER.error("MongoDB Client Error. Ensure that DB and Collection exist");
	    	LOGGER.error(e.getMessage());
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "MongoDB Client Exception. Please check MongoDB Status");

	    }
		catch(MongoException e)
	    {
	    	LOGGER.error("General MongoDB Error. Please check MongoDB Connection and Permissions");
	    	LOGGER.error(e.getMessage());
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "General MongoDB Error. Please check MongoDB Connection and Permissions");

	    }
	
	    catch(Exception e) 
	    {
	    	LOGGER.error("Unspecified Application Error" );

	    }
		
		return null;
		
	}

	public static ArrayList<Object> basicSearchPayload(HttpServerExchange exchange, RequestContext context)
	{
		
		try
		{
		String payloadCollectionName = "";
		String auditCollectionName = "";
		String databaseName = "";
		int page = 1;
		int pagesize = 25;
		List<Map<String, Object>> configList = MongoDBClientSingleton.getErrorSpotConfigs();
		databaseName = configList.get(0).get("where").toString();
		payloadCollectionName = configList.get(1).get("where").toString();
		auditCollectionName = configList.get(2).get("where").toString();
		if (exchange.getQueryParameters().containsKey("page"))
		{
			page = Integer.parseInt(exchange.getQueryParameters().get("page").getFirst().toString());
		}
		if (exchange.getQueryParameters().containsKey("pagesize"))
		{
			pagesize = Integer.parseInt(exchange.getQueryParameters().get("pagesize").getFirst().toString());
		}
		System.out.println(databaseName);
		System.out.println(payloadCollectionName);
		System.out.println(auditCollectionName);
		MongoClient db = MongoDBClientSingleton.getInstance().getClient();
		DbsDAO dao = new DbsDAO();
		DB database = db.getDB(databaseName);
		DBCollection auditCollection = database.getCollection(auditCollectionName);
		DBCollection payloadCollection = database.getCollection(payloadCollectionName);
		Deque<String> filterQuery = exchange.getQueryParameters().get("filter");
		String filterString = filterQuery.getFirst();
		JSONObject intermediateQuery = new JSONObject(filterString);
		JSONArray andClause = intermediateQuery.getJSONArray("$and");
		JSONArray finalandClause = new JSONArray();
		String newQuery = "[";
		String environmentID = "";
		for( int i = 0; i < andClause.length(); i++)
		{
			JSONObject b = andClause.getJSONObject(i);
			System.out.println(b.has("envid"));
			if (b.has("envid"))
			{
				environmentID = b.get("envid").toString();
				System.out.println(environmentID);
				
			}
			else 
			{
				finalandClause.put(andClause.getJSONObject(i));
				newQuery = newQuery + andClause.getJSONObject(i).toString();
			}
		}
		newQuery = newQuery + "]";
		System.out.println(finalandClause.toString());
		intermediateQuery = new JSONObject("{$and:" + finalandClause.toString()+"}");
		System.out.println("intermediate query " + intermediateQuery.toString());
		Deque<String> filter = new ArrayDeque<String>();
		filter.add(intermediateQuery.toString());
		int size = (int) dao.getCollectionSize(payloadCollection, filter);
		List<DBObject >resultList = dao.getCollectionData(payloadCollection, 1, size, null, filter, null);
		List<String> payloadFilterResult = new ArrayList<String>();

		
		for(DBObject s : resultList)
		{
			payloadFilterResult.add(s.get("_id").toString());
		//	System.out.println(s.toString());
			
		}
		
		BasicDBList resultPayloadIDList = new BasicDBList();
		resultPayloadIDList.addAll(payloadFilterResult);
		DBObject inClause = new BasicDBObject("$in",resultPayloadIDList);
		DBObject payloadQuery = new BasicDBObject("dataLocation" , inClause);
		payloadQuery.put("envid", environmentID);
		
		Deque<String> input = new ArrayDeque<String>();
		input.add(payloadQuery.toString());
		System.out.println(payloadQuery.toString());
		long size1 = dao.getCollectionSize(auditCollection, input);
		List<DBObject> outputList = dao.getCollectionData(auditCollection, page, pagesize, null, input, null);
		ArrayList<Object> outputArray = new ArrayList<Object>();
		outputArray.add(outputList);
		outputArray.add(new Long(size1));
		
		return outputArray;
		}
		catch(MongoClientException e)
	    {
	    	LOGGER.error("MongoDB Client Error. Ensure that DB and Collection exist");
	    	LOGGER.error(e.getMessage());
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "MongoDB Client Exception. Please check MongoDB Status");

	    }
		catch(MongoException e)
	    {
	    	LOGGER.error("General MongoDB Error. Please check MongoDB Connection and Permissions");
	    	LOGGER.error(e.getMessage());
	        ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_INTERNAL_SERVER_ERROR, "General MongoDB Error. Please check MongoDB Connection and Permissions");

	    }
	
	    catch(Exception e) 
	    {
	    	LOGGER.error("Unspecified Application Error" );

	    }
		
		return null;
		
	}
}

