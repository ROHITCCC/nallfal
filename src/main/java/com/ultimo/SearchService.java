package com.ultimo;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import org.json.JSONException;
import org.json.JSONObject;
import org.mortbay.util.ajax.JSON;
import org.restheart.db.DBCursorPool;
import org.restheart.db.DBCursorPool.EAGER_CURSOR_ALLOCATION_POLICY;
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

public class SearchService extends ApplicationLogicHandler implements
		IAuthToken {

	MongoClient db;
	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");

	public SearchService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,
			RequestContext context) throws Exception {

		if (context.getMethod() == METHOD.OPTIONS) {
			exchange.getResponseHeaders().put(
					HttpString.tryFromString("Access-Control-Allow-Methods"),
					"GET");
			exchange.getResponseHeaders()
					.put(HttpString
							.tryFromString("Access-Control-Allow-Headers"),
							"Accept, Accept-Encoding, Authorization, Content-Length, Content-Type, Host, Origin, X-Requested-With, User-Agent, No-Auth-Challenge, "
									+ AUTH_TOKEN_HEADER
									+ ", "
									+ AUTH_TOKEN_VALID_HEADER
									+ ", "
									+ AUTH_TOKEN_LOCATION_HEADER);
			exchange.setResponseCode(HttpStatus.SC_OK);
			exchange.endExchange();
		} else if (context.getMethod() == METHOD.GET) {

			String dbname = context.getDBName();
			String collectionName = context.getCollectionName();
			db = MongoDBClientSingleton.getInstance().getClient();
			DB database = db.getDB(dbname);
			

			try {
				Map<String, Deque<String>> queryParams = exchange
						.getQueryParameters();

				
				
				 long size = -1;

			     if (context.isCount()) {
			            size = getDatabase().getCollectionSize(collection, exchange.getQueryParameters().get("filter"));
			       }
			        
				List<DBObject> outputList = executeMongoSearch(queryParams);

				CollectionRepresentationFactory data = new CollectionRepresentationFactory();
				Representation response = data.getRepresentation(exchange, context, outputList, size);
				LOGGER.trace("Results Transformed into RestHeart Represenation");

				int code = HttpStatus.SC_ACCEPTED;
				// ==========================================================================================================
				// Send Response Back

				exchange.setResponseCode(code);
				exchange.getResponseHeaders().put(Headers.CONTENT_TYPE,
						Representation.HAL_JSON_MEDIA_TYPE);
				exchange.getResponseSender().send(response.toString());
				exchange.endExchange();
				LOGGER.debug("Response has been Sent and Exchange has been Closed");
			} catch (JSONException e) {
				LOGGER.error("Incorrectly Formated JSON Array. Please check JSON Array Format");

				ResponseHelper
						.endExchangeWithMessage(exchange,
								HttpStatus.SC_NOT_ACCEPTABLE,
								"Incorrectly Formatted JSON Array. Please check JSON Array Format");

				e.printStackTrace();

			} catch (MongoCommandException e) {
				LOGGER.error("Bad MongoDB Request. Request Errored Out");
				LOGGER.error(e.getMessage());

				ResponseHelper.endExchangeWithMessage(exchange,
						HttpStatus.SC_BAD_REQUEST,
						"Bad MongoDB Request. Please rephrase your command.");
				e.printStackTrace();

			}

			catch (MongoTimeoutException e) {
				LOGGER.error("MongoDB Connection Timed Out. Please check MongoDB Status and try again ");
				LOGGER.error(e.getMessage());

				ResponseHelper.endExchangeWithMessage(exchange,
						HttpStatus.SC_INTERNAL_SERVER_ERROR,
						"MongoDB Connection TimedOut");
				e.printStackTrace();

			} catch (MongoClientException e) {
				LOGGER.error("MongoDB Client Error. Ensure that DB and Collection exist");
				LOGGER.error(e.getMessage());

				ResponseHelper
						.endExchangeWithMessage(exchange,
								HttpStatus.SC_INTERNAL_SERVER_ERROR,
								"MongoDB Client Exception. Please check MongoDB Status");
				e.printStackTrace();

			} catch (MongoException e) {
				LOGGER.error("General MongoDB Exception");
				LOGGER.error(e.getMessage());

				ResponseHelper.endExchangeWithMessage(exchange,
						HttpStatus.SC_INTERNAL_SERVER_ERROR,
						"General MongoDB Error");
				e.printStackTrace();

			}

			catch (Exception e) {
				LOGGER.error("Unspecified Application Error");
				e.printStackTrace();

				ResponseHelper.endExchangeWithMessage(exchange,
						HttpStatus.SC_INTERNAL_SERVER_ERROR,
						"Unspecified Application Error");

			}

		} else {
			ResponseHelper.endExchangeWithMessage(exchange,
					HttpStatus.SC_METHOD_NOT_ALLOWED,
					"Method Not Allowed. Post Only ");
		}

	}

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
			
			resultList = dao.getCollectionData(collection, page, pageSize,
					sort, filter, EAGER_CURSOR_ALLOCATION_POLICY.NONE);

			LOGGER.trace("Query result on payload collection " + resultList.toString());
			
		}else
		{
			
		}
		
		

		return resultList;
	}

}
