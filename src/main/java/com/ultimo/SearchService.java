package com.ultimo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.bson.types.ObjectId;
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
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class SearchService extends ApplicationLogicHandler implements IAuthToken 
{

	MongoClient db;
	//private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");

	public SearchService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception {

		if (context.getMethod() == METHOD.OPTIONS) 
		{
			exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Methods"),"GET");
			exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Headers"),"Accept, Accept-Encoding, Authorization, Content-Length, Content-Type, Host, Origin, X-Requested-With, User-Agent, No-Auth-Challenge, "+ AUTH_TOKEN_HEADER+ ", "	+ AUTH_TOKEN_VALID_HEADER+ ", "	+ AUTH_TOKEN_LOCATION_HEADER);
			exchange.setResponseCode(HttpStatus.SC_OK);
			exchange.endExchange();
		} 
		else if (context.getMethod() == METHOD.GET)
		{
			List<DBObject> output = new ArrayList<DBObject>();
			if (exchange.getQueryParameters().get("searchtype").getFirst().equalsIgnoreCase("advanced") & exchange.getQueryParameters().get("searchdb").getFirst().equalsIgnoreCase("payload"))
			{
				output = advancedSearchPayload(exchange, context);
			}
			else if (exchange.getQueryParameters().get("searchtype").getFirst().equalsIgnoreCase("advanced") & exchange.getQueryParameters().get("searchdb").getFirst().equalsIgnoreCase("audit"))
			{
				output = advancedSearchAudit(exchange, context);
			}
			else if (exchange.getQueryParameters().get("searchtype").getFirst().equalsIgnoreCase("basic") & exchange.getQueryParameters().get("searchdb").getFirst().equalsIgnoreCase("audit"))
			{
				output = basicSearchAudit(exchange, context);
			}
			else if (exchange.getQueryParameters().get("searchtype").getFirst().equalsIgnoreCase("basic") & exchange.getQueryParameters().get("searchdb").getFirst().equalsIgnoreCase("payload"))
			{
				output = basicSearchPayload(exchange, context);
			}

			
			CollectionRepresentationFactory data = new CollectionRepresentationFactory();
			Representation response = data.getRepresentation(exchange, context, output, output.size());
			int code = HttpStatus.SC_ACCEPTED;
			exchange.setResponseCode(code);
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,Representation.HAL_JSON_MEDIA_TYPE);
			exchange.getResponseSender().send(response.toString());
			exchange.endExchange();

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
	public static List<DBObject> advancedSearchAudit(HttpServerExchange exchange, RequestContext context)
	{
		String payloadCollectionName = "";
		String auditCollectionName = "";
		String databaseName = "";
		String searchKeyword = "";
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
		int page = Integer.parseInt(exchange.getQueryParameters().get("page").getFirst().toString());
		int pagesize = Integer.parseInt(exchange.getQueryParameters().get("pagesize").getFirst().toString());

		List<DBObject> resultList = dao.getCollectionData(auditCollection, page,pagesize, null, filter, null);
		
		
		return resultList;
		
	}
	
	public static List<DBObject> advancedSearchPayload(HttpServerExchange exchange,RequestContext context)
	{
		String payloadCollectionName = "";
		String auditCollectionName = "";
		String databaseName = "";
		List<Map<String, Object>> configList = MongoDBClientSingleton.getErrorSpotConfigs();
		databaseName = configList.get(0).get("where").toString();
		payloadCollectionName = configList.get(1).get("where").toString();
		auditCollectionName = configList.get(2).get("where").toString();
		int page = Integer.parseInt(exchange.getQueryParameters().get("page").getFirst().toString());
		int pagesize = Integer.parseInt(exchange.getQueryParameters().get("pagesize").getFirst().toString());
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
		 
		
				return outputList;

		
	}

	public static List<DBObject> basicSearchAudit(HttpServerExchange exchange, RequestContext context)
	{
		
		String payloadCollectionName = "";
		String auditCollectionName = "";
		String databaseName = "";
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
		int page = Integer.parseInt(exchange.getQueryParameters().get("page").getFirst().toString());
		int pagesize = Integer.parseInt(exchange.getQueryParameters().get("pagesize").getFirst().toString());

		List<DBObject> resultList = dao.getCollectionData(auditCollection, page,pagesize, null, filterQuery, null);
		
		return resultList;
		
	}

	public static List<DBObject> basicSearchPayload(HttpServerExchange exchange, RequestContext context)
	{
		String payloadCollectionName = "";
		String auditCollectionName = "";
		String databaseName = "";
		List<Map<String, Object>> configList = MongoDBClientSingleton.getErrorSpotConfigs();
		databaseName = configList.get(0).get("where").toString();
		payloadCollectionName = configList.get(1).get("where").toString();
		auditCollectionName = configList.get(2).get("where").toString();
		int page = Integer.parseInt(exchange.getQueryParameters().get("page").getFirst().toString());
		int pagesize = Integer.parseInt(exchange.getQueryParameters().get("pagesize").getFirst().toString());
		System.out.println(databaseName);
		System.out.println(payloadCollectionName);
		System.out.println(auditCollectionName);
		MongoClient db = MongoDBClientSingleton.getInstance().getClient();
		DbsDAO dao = new DbsDAO();
		DB database = db.getDB(databaseName);
		DBCollection auditCollection = database.getCollection(auditCollectionName);
		DBCollection payloadCollection = database.getCollection(payloadCollectionName);
		List<String> payloadFilterResult = new ArrayList<String>();
		DBCursor cursor = payloadCollection.find();
		while(cursor.hasNext())
		{
			payloadFilterResult.add(cursor.next().get("_id").toString());
			
		}
		
		BasicDBList resultPayloadIDList = new BasicDBList();
		resultPayloadIDList.addAll(payloadFilterResult);
		DBObject inClause = new BasicDBObject("$in",resultPayloadIDList);
		DBObject payloadQuery = new BasicDBObject("dataLocation" , inClause);
		Deque<String> input = new ArrayDeque<String>();
		input.add(payloadQuery.toString());
		List<DBObject> outputList = dao.getCollectionData(auditCollection, page, pagesize, null, input, null);
		
		
		
		

		return outputList;
		
	}
}

