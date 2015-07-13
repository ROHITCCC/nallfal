package com.ultimo;

import java.io.BufferedReader;
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
			String dbname = context.getDBName();
			String collectionName = context.getCollectionName();
			client = MongoDBClientSingleton.getInstance().getClient();
			DB database = client.getDB(dbname);
			DBCollection collection = database.getCollection(collectionName);
			LOGGER.info("successfully connected to the database");
			LOGGER.trace("db: "+dbname);
			LOGGER.trace("collection: "+collectionName);
			LOGGER.info("successfully got the db and collection names");
			
			if (context.getMethod() == METHOD.OPTIONS) {
				Collection<String> allowedMethods= new ArrayList<>();
				allowedMethods.add("GET");
				allowedMethods.add("POST");
	            exchange.getResponseHeaders().putAll(HttpString.tryFromString("Access-Control-Allow-Methods"), allowedMethods);
	            exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Methods"), "POST");
	            exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Headers"), "Accept, Accept-Encoding, Authorization, Content-Length, Content-Type, Host, Origin, X-Requested-With, User-Agent, No-Auth-Challenge, " + AUTH_TOKEN_HEADER + ", " + AUTH_TOKEN_VALID_HEADER + ", " + AUTH_TOKEN_LOCATION_HEADER);
	            exchange.setResponseCode(HttpStatus.SC_OK);
	            exchange.endExchange();
	        } 
	        else if (context.getMethod() == METHOD.GET){
	        	LOGGER.info("Starting the GET service");
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
						while (cursor.hasNext()) {
							resultList.add(cursor.next());
						}
						displayCollection(exchange,context,resultList);
						return;
					}
				}
				
				//validations-------------------------------------------------------------------------------
				if(!(queryParams.containsKey("object"))){
					//checks to see if object is passed as a parameter in the url.error if not
					LOGGER.error("object not detected");
					ResponseHelper.endExchangeWithMessage(exchange, 404, "An object needs to be specified, preceded with: \"object=\"");
					return;
				}
				if(queryParams.get("object").size()!=1){
					//check to see if more than one object is passed as a perameter. error if so
					LOGGER.error("more than one object is being querrired");
					ResponseHelper.endExchangeWithMessage(exchange, 404, "only one object can be querried");
					return;
				}
				
				//handle object-----------------------------------------------------------------------------
				LOGGER.info("validations passed");
				//gets the request value of object= and splits it at a period, and passes it to array filter condition
				String filterConditions[]=queryParams.get("object").getFirst().split("\\.");
				LOGGER.trace(" object ="+ filterConditions[0]);
				//uses switch case for the first elemt of the filter condition
				switch (filterConditions[0]){
				case "setting" : handleSetting(exchange,context,collection,filterConditions); break;
				case "report" : handleReport(exchange,context,collection,queryParams); break;
				default : 
					LOGGER.error("no switch case to handle given object");
					ResponseHelper.endExchangeWithMessage(exchange, 404, "given object is invalid");
					return;
				}
			}
	        else if(context.getMethod()==METHOD.POST){
	        	LOGGER.info("Starting the POST service");
				//get payload from the inputStream on the webpage-------------------------------------------
				InputStream input = exchange.getInputStream();
				BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
				String line = null;
				String payload = "";
				while((line = inputReader.readLine())!=null){
					payload += line;
				}
				
				//insert payload/document into the mongodb--------------------------------------------------
				DBObject document = (DBObject) JSON.parse(payload);
				// this is used for validations for duplicate insertions of setting and reports
				BasicDBObject whereQuery = new BasicDBObject(); 
				if(document.get("setting")!=null){
					LOGGER.info("document is a setting");
					//overwrite setting if it exists. If it doesn't exist, adds a new one
					whereQuery.put("setting", new BasicDBObject("$ne", null));
					//LOGGER.trace(whereQuery.toString());
				}
				else if(document.get("report")!=null){
					LOGGER.info("document is a report");
					//JSONObject jsonDoc = (JSONObject)document;
					//reportSubDoc =document.get("report");
					String tempDocument = document.toString();
					whereQuery=(BasicDBObject)JSON.parse(tempDocument);
					((BasicDBObject)whereQuery.get("report")).removeField("email");
					((BasicDBObject)whereQuery.get("report")).removeField("frequency");
					
				}
				LOGGER.info(whereQuery.toString());
				DBCursor cursor = collection.find(whereQuery);
				if(cursor.size()!=0){
					//if there is already a document with the given fields and values as whereQuery, 
					//overwrite the document.
					DBObject doc = cursor.next();
					LOGGER.info("replacing exisiting document with the new one");
					collection.findAndRemove(doc);
				}
				
				//if the document is a report then validate the passes template
				if(document.get("report")!=null){
					//get the template string
					String template=((DBObject)document.get("report")).get("template").toString();
					if(template==null){ 
						LOGGER.error("The report document does not have a template field");
						ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_FOUND, "The report document is missing a template field");
						return;
					}
					boolean templateExists=NotificationService.validateTemplate(template);
					if(templateExists){
						LOGGER.info("the given report has a valid template field");
					}
					else{
						LOGGER.error("the template: "+template+" could not be found");
						ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_FOUND, "The passed report document has an invalid template field");
						return;
					}
				}
				collection.insert(document);
				
				//schedule a notification if the document is a report
				if(document.get("report")!=null){
					//schedule the report in quartz
					Date sceduleTime = scheduleReport(new JSONObject(document.toString()));
					LOGGER.info("successfully scheduled report");
				}
				
				LOGGER.info("Successfully inserted report into the database, report " + document.toString());
				exchange.getResponseSender().send("Payload successfully stored as document on Mongo Database");
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
	
	public void handleSetting(HttpServerExchange exchange, RequestContext context, DBCollection collection, String[] qList ) throws IllegalQueryParamenterException{
		LOGGER.info("Starting setting service");
		BasicDBObject whereQuery = new BasicDBObject();
		//this gets only the setting
		whereQuery.put("setting", new BasicDBObject("$ne", null));
		DBCursor cursor = collection.find(whereQuery);
		LOGGER.info("cursor size: "+ cursor.size());
		//makes sure only one document is being queried
		if(cursor.size()!=1){
			LOGGER.error("there can only be one Setting queried");
			ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_CONFLICT, "Setting object is more than one or doesnt exist");
			return;
		}
		DBObject document = (DBObject) cursor.next();
		String id = document.get("_id").toString(); // this is used later for HAL representation
		boolean hasQuery=true; //checks to see if the given query is in Setting
		for(String query: qList){
			//parse through each query from qList(the array that was split by the period)
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
    		LOGGER.info("printing the sub-document");
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
		LOGGER.info("Starting Report Service");
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
		LOGGER.trace(whereQuery.toString());
		//check if where query is empty and if so, make sure it only displays reports
		if(whereQuery.isEmpty()){
			whereQuery.put("report", new BasicDBObject("$ne", null));
		}
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
	    	LOGGER.info("displaying found reports");
	    	displayCollection(exchange, context,resultList);
	    }
	}
	
	public void displayCollection(HttpServerExchange exchange, RequestContext context, List<DBObject> outputList) throws IllegalQueryParamenterException{
		//displays content in HAL representation on the webpage
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
	
	private static Date scheduleReport(JSONObject report) throws SchedulerException, java.text.ParseException{
		
		
		
		String applicationName = report.getJSONObject("report").getString("application");
		
		String jobKeyName;
		
		//Interface and error type are optional fields
		try{
			String interfaceName = report.getJSONObject("report").getString("interface1");
			
			if(!interfaceName.isEmpty())
				jobKeyName = applicationName + "." + interfaceName;
			else
				jobKeyName = applicationName;
			
		} catch (JSONException e){
			LOGGER.warn(e.getMessage());
			jobKeyName = applicationName;
		}
		
		try{
			
			String errorType = report.getJSONObject("report").getString("errorType");
			
			if(!errorType.isEmpty())
				jobKeyName = jobKeyName + "." + errorType;
			
		} catch (JSONException e){
			LOGGER.warn(e.getMessage());
			
		}
		
		LOGGER.info("Job Key name " + jobKeyName);
		
		
		
		JobKey jobKey = new JobKey(jobKeyName);
		Scheduler scheduler;
		//get scheduler 
		try{
			
			scheduler = new StdSchedulerFactory().getScheduler();
		
		} catch (SchedulerException e){
			LOGGER.error(e.getMessage());
			LOGGER.error(e.getStackTrace().toString());
			throw e;
		}
		
		JobDetail job;
		try{
				job = scheduler.getJobDetail(jobKey);
				
				//remove old job if exists
				scheduler.deleteJob(jobKey);
				
		} catch(SchedulerException e){
			LOGGER.info("Job with JobKey " + jobKeyName + " does not exits. Createing new Job");

		}
		
		//create  job
		job = JobBuilder.newJob(ReportJob.class)
				.withIdentity(jobKey).build();

		
		job.getJobDataMap().put("report", report.toString());
		
		LOGGER.info("created new job");
		
		//Create Trigger
		Trigger trigger = getSechduleTrigger(report, jobKeyName);
		
		LOGGER.info("created new trigger");

		
		//if scheduler is not start it, start the scheduler
		if(!scheduler.isStarted()){
			scheduler.start();
		}
		LOGGER.info("scheduler is started");
		
		Date startDateTime = scheduler.scheduleJob(job, trigger);
		
		LOGGER.info("Report " + jobKeyName + " is scheduled to start at " + startDateTime.toString() + " and will run every " 
		+ report.getJSONObject("report").getJSONObject("frequency").getString("duration") + " " 
				+ report.getJSONObject("report").getJSONObject("frequency").getString("unit"));
		
		return startDateTime;
		
	}

	private static Trigger getSechduleTrigger(JSONObject report, String triggerName) throws JSONException, java.text.ParseException {
		LOGGER.trace("trigger name: "+triggerName);
		//JSONObject report = new JSONObject(payload);
		
		JSONObject frequency; 
		int duration; 
		String unit;
		try{
			frequency = report.getJSONObject("report").getJSONObject("frequency");
			duration = frequency.getInt("duration");
			unit = frequency.getString("unit");
		}
		catch (JSONException e){
			LOGGER.error(e.getMessage());
			LOGGER.error(e.getStackTrace().toString());
			throw e;
		}
		Date triggerStartTime;
		String startDateTime = frequency.getString("starttime");
		

		if (startDateTime != null && !startDateTime.isEmpty()) {
			LOGGER.info("start time is given");

			SimpleDateFormat formatter = new SimpleDateFormat(
					"MM/dd/yyyy'T'hh:mm:ss");
			triggerStartTime = formatter.parse(startDateTime);

		} else {
			triggerStartTime = new Date();
			LOGGER.info("no starttime is given, using current time as default stattime");
		}

		// default schedule is 1 hr
		int seconds = calculateDurationInseconds(duration, unit);

		LOGGER.trace("seconds: "+seconds);

		SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder
				.simpleSchedule().withIntervalInSeconds(seconds)
				.repeatForever();

		Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity(triggerName).withSchedule(scheduleBuilder)
				.startAt(triggerStartTime).build();

		return trigger;
	}
	
	public static int calculateDurationInseconds(int duration, String unit){
		LOGGER.trace("duration: "+duration);
		LOGGER.trace("unit: "+ unit);
		
		//default is 1 hr
		int seconds = 60 * 60;

		switch (unit) {

		case "sec":
			seconds = duration;
			break;
		case "min":
			seconds = 60 * duration;
			break;
		case "hr":
			seconds = 60 * 60 * duration;
			break;
		case "hrs":
			seconds = 60 * 60 * duration;
			break;
		case "day":
			seconds = 24 * 60 * 60 * duration;
			break;
		case "days":
			seconds = 24 * 60 * 60 * duration;
			break;

		default:
			
		}
		LOGGER.info("the time interval is scheduled for "+seconds+" seconds");
		return seconds;
	}
}
		