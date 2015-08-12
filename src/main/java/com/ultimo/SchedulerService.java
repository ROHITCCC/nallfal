package com.ultimo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.security.handlers.IAuthToken;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public class SchedulerService extends ApplicationLogicHandler implements IAuthToken{
	
	public SchedulerService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");
	
	public static Scheduler scheduler;
	
	public static void startScheduler() throws SchedulerException{
		startScheduler("");
	}
	
	public static void startScheduler(String propertiesFile) throws SchedulerException{
		try {
			//get scheduler
			StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
			if(propertiesFile.length()>0){
				//check existence of file
				File file = new File(propertiesFile);
				if(file.exists()){
					schedulerFactory.initialize(propertiesFile);
				}
				else{
					LOGGER.warn(propertiesFile+" doesn't exist in current working directory");
				}
			}
			else{
				schedulerFactory.initialize("config/quartz.properties");
			}
			scheduler = schedulerFactory.getScheduler();
			scheduler.start();
			
			//get and schedule existing reports
			LOGGER.info("Scheduler "+scheduler.getSchedulerName()+ " started, getting existing report and scheduling them");
			DBCursor cursor=getReports();
			LOGGER.info("Scheduling "+cursor.size()+" existing reports");
			while(cursor.hasNext()){
				//schedule the existing reports
				scheduleReport(new JSONObject(cursor.next().toString()));
			}
		} 
		catch (SchedulerException e) {
			throw e;
		} 
		//shouldn't ever be in these catch blocks
		catch (JSONException e) {
			LOGGER.error(e.getMessage());
			LOGGER.error("the error: ",e);
		} 
		catch (ParseException e) {
			LOGGER.error(e.getMessage());
			LOGGER.error("the error: ",e);
		}
	}
	
	public static void stopScheduler() throws SchedulerException{
		if(scheduler==null || !scheduler.isStarted()){
			LOGGER.info("the scheduler hasn't been started yet");
			return;
		}
		else{
			LOGGER.info("shutting down the scheduler "+scheduler.getSchedulerName());
			scheduler.shutdown();
			scheduler=null;
		}
	}
	
	public static Date scheduleReport(JSONObject report) throws SchedulerException, java.text.ParseException{		
		LOGGER.info("scheduling passed report");
		LOGGER.trace("report: "+report.toString());
		String jobKeyName = getJobName(report);
		if(scheduler==null || !scheduler.isStarted()){
			LOGGER.error("the scheduler is not started, so the job: "+jobKeyName+" will not be scheduled");
			return null;
		}
		JobKey jobKey = new JobKey(jobKeyName);
		JobDetail job;
		try{
				job = scheduler.getJobDetail(jobKey);
				
				//remove old job if exists
				scheduler.deleteJob(jobKey);
				LOGGER.info("removed old job and creating new job with job key: "+jobKeyName);
				
		} catch(SchedulerException e){
			LOGGER.info("Job with JobKey " + jobKeyName + " does not exits. Createing new Job");

		}
		
		//create  job
		job = JobBuilder.newJob(ReportJob.class)
				.withIdentity(jobKey).build();

		
		job.getJobDataMap().put("report", report.toString());
		
		
		
		JSONObject frequency = report.getJSONObject("report").getJSONObject("frequency");
		//Create Trigger
		Trigger trigger = getScheduleTrigger(frequency, jobKeyName);
		
		LOGGER.debug("created new trigger with same name as jobKeyName: "+jobKeyName);
		Date startDateTime = scheduler.scheduleJob(job, trigger);
		LOGGER.info("Report " + jobKeyName + " is scheduled to start at " + startDateTime.toString() + " and will run every " 
		+ report.getJSONObject("report").getJSONObject("frequency").getString("duration") + " " 
				+ report.getJSONObject("report").getJSONObject("frequency").getString("unit"));
		
		return startDateTime;
		
	}

	private static Trigger getScheduleTrigger(JSONObject frequency, String triggerName) throws JSONException, java.text.ParseException {
		//JSONObject report = new JSONObject(payload);
		
		//JSONObject frequency; 
		int duration; 
		String unit;
		try{
			//frequency = report.getJSONObject("report").getJSONObject("frequency");
			duration = frequency.getInt("duration");
			unit = frequency.getString("unit");
		}
		catch (JSONException e){
			LOGGER.error(e.getMessage());
			LOGGER.error("the error: ",e);
			throw e;
		}
		Date triggerStartTime;
		String startDateTime = frequency.getString("starttime");
		

		if (startDateTime != null && !startDateTime.isEmpty()) {
			LOGGER.info("start time is: "+startDateTime);

			SimpleDateFormat formatter = new SimpleDateFormat(
					"MM/dd/yyyy'T'hh:mm:ss");
			triggerStartTime = formatter.parse(startDateTime);

		} else {
			triggerStartTime = new Date();
			LOGGER.debug("no starttime is given, using current time as default stattime of job");
		}

		// default schedule is 1 hr
		int seconds = calculateDurationInseconds(duration, unit);


		SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder
				.simpleSchedule().withIntervalInSeconds(seconds)
				.repeatForever();

		Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity(triggerName).withSchedule(scheduleBuilder)
				.startAt(triggerStartTime).build();

		return trigger;
	}
	
	public static int calculateDurationInseconds(int duration, String unit){
		
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
		return seconds;
	} 

	public static String getJobName(JSONObject report){
		BasicDBObject reportDoc= (BasicDBObject) JSON.parse(report.toString());
		String jobKeyName = reportDoc.get("_id").toString();
		return jobKeyName;
	}
	
	public static boolean deleteJob(JSONObject report) throws Exception{
		LOGGER.info("deleting job associated with the passed report");
		if(scheduler==null || !scheduler.isStarted()){
			//don't delete if scheduler is not started
			LOGGER.info("The scheduler is not started, so there is no job to delete");
			return false;
		}
		boolean jobDeleted=false;
		try{
			String jobKeyName=getJobName(report);
			LOGGER.info("deleting Job with JobKey: "+jobKeyName);
			JobKey jobKey = new JobKey(jobKeyName);
			jobDeleted=scheduler.deleteJob(jobKey);
				
		} catch(SchedulerException e){
			LOGGER.info("Job with JobKey " + getJobName(report) + " does not exits. so no job deleted");

		}
		return jobDeleted;
	}

	public static DBCursor getReports(){
		MongoClient client = MongoDBClientSingleton.getInstance().getClient();
		String dbname = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
		String collectionName = MongoDBClientSingleton.getErrorSpotConfig("u-setting-collection");
        DB database = client.getDB(dbname);
        DBCollection collection = database.getCollection(collectionName);
        LOGGER.info("querring all the reports in db: "+dbname+" and collection: "+collectionName);  
        //this gets report documents only
        BasicDBObject whereQuery = new BasicDBObject();
		whereQuery.put("report", new BasicDBObject("$ne", null));
		DBCursor cursor = collection.find(whereQuery);
		return cursor;
	}

	@Override
	public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception {
		LOGGER.info("starting the SchedulerService");
		if (context.getMethod() == METHOD.OPTIONS) {
			ErrorSpotSinglton.optionsMethod(exchange);
        }
		else if(context.getMethod() == METHOD.POST){
			//Map<String, Deque<String>> queryParams= exchange.getQueryParameters();
			//LOGGER.trace("Query Parameters: "+queryParams.toString());
			
			InputStream input = exchange.getInputStream();
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
            //gets payload
            String line = null;
            String payload = "";
            while((line = inputReader.readLine())!=null){
            	payload += line;
            }
            LOGGER.trace("the payload: "+payload);
            JSONObject requestInfo=null; 
            try{
				requestInfo= new JSONObject(payload);
			}
			catch (JSONParseException e){
				LOGGER.error("the error: ", e);
				ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "The past payload must be a JSON");
			}
			String requestType= requestInfo.getString("requestType");
			switch (requestType){
			case "startScheduler":
				LOGGER.info("The payload recieved is meant to start the scheduler");
				if(scheduler !=null && scheduler.isStarted()){
					LOGGER.info("the scheduler is already started");
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "The Scheduler is already started");
					return;
				}
				String propertiesFile= "";
				try{
					propertiesFile=requestInfo.getString("propertiesFile");
				}
				catch(JSONException e){
				}
				if(!propertiesFile.equals("")){
					LOGGER.info("starting scheduler with properties file: "+propertiesFile);
					File file =new File(propertiesFile);
					if(!file.exists()){
						LOGGER.error("The given properties file does not exist");
						ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_NOT_FOUND, "The given properties file does not exist");
						return;
					}
					try{
						startScheduler(propertiesFile);
						exchange.getResponseSender().send("Sheduler is started");
					}
					catch(SchedulerException e){
						LOGGER.error("the error", e);
						LOGGER.error("The scheduler couldn't be started");
						ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_SERVICE_UNAVAILABLE, "The scheduler failed to start");
						return;
					}
				}
				else{
					LOGGER.debug("no properties file detected, starting the scheduler with the default properties file");
					try{
						startScheduler();
						exchange.getResponseSender().send("Sheduler is started");
					}
					catch(SchedulerException e){
						LOGGER.error("The scheduler couldn't be started");
						LOGGER.error("the error",e);
						ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_SERVICE_UNAVAILABLE, "The scheduler failed to start");
						return;
					}
				}
				exchange.getResponseSender().send("Started scheduler: "+scheduler.getSchedulerName());
				break;
			
			case "stopScheduler":
				LOGGER.info("payload recieved was meant to stop scheduler");
				if(scheduler==null || scheduler.isShutdown()){
					LOGGER.info("the scheduler is already stopped");
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "The scheduler is already shutdown");
					return;
				}
				try{
					stopScheduler();
					exchange.getResponseSender().send("Stopped scheduler");
				}
				catch(SchedulerException e){
					LOGGER.error("the error",e);
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_SERVICE_UNAVAILABLE, "Could not stop the scheduler");
					return;
				}
				break;
			case "startJob":
				LOGGER.info("payload recieved was meant to start a given job");
				Date jobStartDate=null;
				try{
					jobStartDate=startJob(requestInfo);
				}
				catch(ClassNotFoundException e){
					LOGGER.error("the job class that is passed in the payload does not exist or is invalid");
					LOGGER.error("the error",e);
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_GATEWAY, "the job class that is passed is invalid");
					return;
				}
				catch(ParseException e){
					LOGGER.error("the jobName or jobClass fields don't exist or are invalid");
					LOGGER.error("the error",e);
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_GATEWAY, "the jobName or jobClass fields don't exist or are invalid");
					return;
				}
				catch(JSONException e){
					LOGGER.error("the jobName or jobClass fields don't exist or are invalid");
					LOGGER.error("the error",e);
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_GATEWAY, "the jobName or jobClass fields don't exist or are invalid");
					return;
				}
				catch(SchedulerException e){
					LOGGER.error("the ");
					LOGGER.error("the error",e);
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_GATEWAY, "the jobName or jobClass fields don't exist or are invalid");
					return;
				}
				if(jobStartDate==null){
					LOGGER.error("scheduler is not started");
		        	ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_SERVICE_UNAVAILABLE, "The scheduler is not started");
		        	return;
				}
				exchange.getResponseSender().send("Started Job");
				break;
			case "stopJob":
				LOGGER.info("payload recieved was meant to stop a given job");
				if(scheduler ==null || scheduler.isShutdown()){
					LOGGER.info("the scheduler is shutdown");
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "The Scheduler is not started");
					return;
				}
				try{
					stopJob(requestInfo);
				}
				catch(JSONException e){
					LOGGER.error("the jobName or jobClass fields don't exist or are invalid");
					LOGGER.error("the error",e);
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_GATEWAY, "the jobName or jobClass fields don't exist or are invalid");
					return;
				}
				catch(SchedulerException e){
					LOGGER.error("the error",e);
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "The Scheduler could not stop the job");
					return;
				}
				exchange.getResponseSender().send("Stoped Job");
				break;
			case "getAllJobs":
				LOGGER.info("payload recieved was meant to display all job");
				JSONArray jobArray= null;
				try{
					jobArray = getAllJobs();
				}
				catch (SchedulerException e){
					LOGGER.error("the error",e);
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "The Scheduler could not get the jobs");
					return;
				}
				if(jobArray==null){
					LOGGER.info("the scheduler is shutdown");
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "The Scheduler is not started");
					return;
				}
				exchange.getResponseSender().send(jobArray.toString());
				break;
			case "suspendJob":
				LOGGER.info("payload recieved was meant to pause a job");
				boolean suspended = false;
				try{
					suspended = suspendJob(requestInfo);
				}
				catch(SchedulerException e){
					LOGGER.error("the error",e);
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "The Scheduler could not suspend the job");
					return;
				}
				if(suspended){
					exchange.getResponseSender().send("suspended Job");
				}
				else{
					LOGGER.info("the job could not be suspended");
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "The job could not be suspended");
					return;
				}
				break;
			case "resumeJob":
				LOGGER.info("payload recieved was meant to play a paused job");
				boolean resumed = false;
				try{
					resumed = resumeJob(requestInfo);
				}
				catch(SchedulerException e){
					LOGGER.error("the error",e);
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "The Scheduler could not resume the job");
					return;
				}
				if(resumed){
					exchange.getResponseSender().send("resumed Job");
				}
				else{
					LOGGER.info("the job could not be resumed");
					ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "The job could not be resumed");
					return;
				}
				break;
			case "getSchedulerStatus":
				LOGGER.info("payload recieved was meant to display scheduler status");
				if(scheduler==null || !scheduler.isStarted()){
					LOGGER.info("scheduler is stopped");
					exchange.getResponseSender().send("stopped");
				}
				else{
					LOGGER.info("scheduler is started");
					exchange.getResponseSender().send("started");
				}
				break;
			default:
				LOGGER.info("the requestType is invlaid");
				ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_BAD_REQUEST, "the requestType is invlaid");
				break;
			}
		}
		else 
        {
			LOGGER.error("invaild http option");
        	ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_METHOD_NOT_ALLOWED, "Method Not Allowed. Post Only ");
        }
	}
	
	public static Date startJob(JSONObject requestInfo) throws SchedulerException, ClassNotFoundException, JSONException, ParseException{
		LOGGER.info("starting and scheduleing a new Job");
		LOGGER.trace("job request: "+requestInfo.toString());
		String jobKeyName = requestInfo.getString("jobName");
		LOGGER.trace("jobKey= "+jobKeyName);
		if(scheduler==null || !scheduler.isStarted()){
			LOGGER.error("the scheduler is not started, so the job: "+jobKeyName+" will not be scheduled");
			return null;
		}
		JobKey jobKey = new JobKey(jobKeyName);
		JobDetail job;
		try{
				job = scheduler.getJobDetail(jobKey);
				
				//remove old job if exists
				scheduler.deleteJob(jobKey);
				
		} catch(SchedulerException e){
			LOGGER.info("Job with JobKey " + jobKeyName + " does not exits. Createing new Job");
			
		}
		
		//create  job
		Class jobClass = Class.forName("com.ultimo."+requestInfo.getString("jobClass"));
		job = JobBuilder.newJob(jobClass).withIdentity(jobKey).build();

		
		job.getJobDataMap().put("requestInfo", requestInfo.toString());
		
		LOGGER.info("created new job with job key: "+jobKeyName);
		
		JSONObject frequency = requestInfo.getJSONObject("frequency");
		//Create Trigger
		Trigger trigger = getScheduleTrigger(frequency, jobKeyName);
		
		LOGGER.info("created new trigger");
		Date startDateTime = scheduler.scheduleJob(job, trigger);
		LOGGER.info("Job " + jobKeyName + " is scheduled to start at " + startDateTime.toString() + " and will run every " 
		+ requestInfo.getJSONObject("frequency").getString("duration") + " " 
				+ requestInfo.getJSONObject("frequency").getString("unit"));
		
		return startDateTime;
	}
	public static void stopJob(JSONObject requestInfo) throws JSONException, SchedulerException{
		scheduler.deleteJob(new JobKey(requestInfo.getString("jobName")));
	}
	public static JSONArray getAllJobs() throws SchedulerException{
		LOGGER.info("getting all jobs");
		//return if the scheduler isn't started
		if(scheduler==null || !scheduler.isStarted()){
			LOGGER.info("the scheduler hasn't been started yet");
			return null;
		}
		List<String> groupNames = scheduler.getJobGroupNames();
		JSONArray jobArray = new JSONArray();
		for(String groupName: groupNames){
			LOGGER.trace("job group: "+ groupName);
			for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))){
				
				//get job's trigger
				Trigger trigger = (Trigger) scheduler.getTriggersOfJob(jobKey).get(0);
				Date nextFireTime = trigger.getNextFireTime(); 
				TriggerState status = scheduler.getTriggerState(trigger.getKey());
				
				LOGGER.trace("Job key: "+jobKey.toString() +" Job next fire time: "+nextFireTime.toString()+" Job status: "+status.toString());
				//add the jobKey, nextFireTime, and status to a json object
				JSONObject jobInfo = new JSONObject();
				jobInfo.put("jobKey", jobKey.getName());
				jobInfo.put("nextFireTime", nextFireTime.toString());
				jobInfo.put("status", status.toString());
				
				//check if it is a BatchReplayJob and if so, display frequency
				if(jobKey.getName().equals("BatchReplayJob")){
					long frequency = trigger.getFireTimeAfter(trigger.getNextFireTime()).getTime()-trigger.getNextFireTime().getTime();
					//convert frequncy to seconds from milliseconds
					frequency=frequency/1000;
					jobInfo.put("frequency", frequency);
				}
				
				//add the Json object to the array
				jobArray.put(jobInfo);
			}
		}
		return jobArray;
	}
	public boolean suspendJob(JSONObject requestInfo) throws SchedulerException{
		LOGGER.info("attempting to pause job");
		if(scheduler==null || !scheduler.isStarted()){
			LOGGER.info("the scheduler hasn't been started yet");
			return false;
		}
		if(requestInfo.getString("jobKey")==null){
			LOGGER.error("no jobKey field was given. Aa jobKey must be given");
			return false;
		}
		LOGGER.info("attempting to pause job with job id: "+requestInfo.getString("jobKey"));
		JobKey jobKey=new JobKey(requestInfo.getString("jobKey"));
		if(!scheduler.checkExists(jobKey)){
			LOGGER.error("no job is associated with the passed jobKey: "+requestInfo.getString("jobKey"));
			return false;
		}
		Trigger trigger = (Trigger) scheduler.getTriggersOfJob(jobKey).get(0);
		if(scheduler.getTriggerState(trigger.getKey()).equals(Trigger.TriggerState.PAUSED)){
			LOGGER.error("the Job is already paused");
			return true;
		}
		scheduler.pauseJob(new JobKey(requestInfo.getString("jobKey")));
		LOGGER.info("paused Job: "+jobKey.toString());
		return true;
	}
	public boolean resumeJob(JSONObject requestInfo) throws SchedulerException{
		LOGGER.info("attempting to resume job");
		if(scheduler==null || !scheduler.isStarted()){
			LOGGER.info("the scheduler hasn't been started yet");
			return false;
		}
		if(requestInfo.getString("jobKey")==null){
			LOGGER.error("no jobKey field was given. Aa jobKey must be given");
			return false;
		}
		LOGGER.info("attempting to resume job with job id: "+requestInfo.getString("jobKey"));
		JobKey jobKey=new JobKey(requestInfo.getString("jobKey"));
		if(!scheduler.checkExists(jobKey)){
			LOGGER.error("no job is associated with the passed jobKey: "+requestInfo.getString("jobKey"));
			return false;
		}
		Trigger trigger = (Trigger) scheduler.getTriggersOfJob(jobKey).get(0);
		if(!scheduler.getTriggerState(trigger.getKey()).equals(Trigger.TriggerState.PAUSED)){
			LOGGER.error("the Job is not currently paused for it tto be resumed");
			return true;
		}
		scheduler.resumeJob(new JobKey(requestInfo.getString("jobKey")));
		LOGGER.info("resumed Job: "+jobKey.toString());
		return true;
	}
}
