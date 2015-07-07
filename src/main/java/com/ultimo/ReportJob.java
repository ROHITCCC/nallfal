package com.ultimo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.restheart.db.MongoDBClientSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

public class ReportJob implements Job{
	
	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");
	
	
	public static List<DBObject> queryMongo(Date previousRun, String report, String jobName){
		
		MongoClient client = MongoDBClientSingleton.getInstance().getClient();

		BasicDBObject payload=(BasicDBObject)JSON.parse(report);
		payload = (BasicDBObject) payload.get("report");
		
		//since email and frequency are not needed for aggrigate query, remove frequency and email from the payloa.
		BasicDBObject frequency = (BasicDBObject)payload.removeField("frequency");
		payload.removeField("email");
		
		//BasicDBObject aggregateParam = ((BasicDBObject)payload.get("report"));
		
	    TimeZone tz = TimeZone.getTimeZone("UTC");
	    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	    df.setTimeZone(tz);
	    Date currentTime = new Date();
	    String currentJobExecutionTime = df.format(currentTime);
	    
	    
	    String previousJobExecutionTime;
	    
	    if(previousRun == null){
	    	int duationInSeconds = SettingService.calculateDurationInseconds(Integer.parseInt(frequency.getString("duration")), frequency.getString("unit"));
	    	long reportStartTime = currentTime.getTime()/1000 - duationInSeconds;
	    	
	    	previousJobExecutionTime = df.format(new Date(reportStartTime * 1000));
	    	
	    } else{
	    	previousJobExecutionTime = df.format(previousRun);
	    }
	    

	    if(payload.containsField("interface1") && payload.getString("interface1").length() == 0){
	    	payload.remove("interface1");
	    }
	    if(payload.containsField("errorType") && payload.getString("errorType").length() == 0){
	    	payload.remove("errorType");
	    }
	    
	    String matchParam = payload.toString();
	    
	    
	    
	    
	    
	    
	    payload.replace("application", "$application");
	    if(payload.containsField("interface1")){
	    	payload.replace("interface1", "$interface1");
	    }
	    else{
	    	 
	    	 payload.append("interface1", "$interface1");
	    }
	   
	    payload.remove("errorType");
	    
	    String groupParam = payload.toString();
	    
		
		String query = "[{'$match':{'$and':[{'timestamp':{'$gte':{'$date':"+
        "'"+ previousJobExecutionTime +"'},'$lt':{'$date':'"+currentJobExecutionTime+"'}}},"+
        matchParam + "]}},{'$group':{'_id':" + groupParam + ",'count':{'$sum':1}}}]";
		
		
		
		LOGGER.info("Executing Job ("+ jobName + ") query " + query);
		
		//needs to get he colletion from config file
		String dbname = "ES";
		String collectionName = "ErrorSpotActual";
		
        DB database = client.getDB(dbname);
        DBCollection collection = database.getCollection(collectionName);
        List<DBObject> result = AggregateService.executeMongoAggregate(query, collection);

		return result;
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		JobDataMap jobData = context.getJobDetail().getJobDataMap();
		String jobName = context.getJobDetail().getKey().getName();
		Date previousJobExecutionTime = context.getPreviousFireTime();
		
		LOGGER.info("Report job with JobName " + jobName + " is started at " + context.getFireTime().toString()+ ". Report parameter is " + jobData.getString("report"));
		
		List<DBObject> result = queryMongo(previousJobExecutionTime, jobData.getString("report"), jobName);
		
		
		LOGGER.info("Executed report (" + jobName + ") output " + result.toString());
		
	}
	
}
