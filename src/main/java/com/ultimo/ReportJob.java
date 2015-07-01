package com.ultimo;

import org.json.JSONObject;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.restheart.db.MongoDBClientSingleton;

import com.mongodb.MongoClient;

public class ReportJob implements Job{
	
	
	public static int queryMongo(String report){
		
		MongoClient client = MongoDBClientSingleton.getInstance().getClient();
		
		System.out.println(client.getDatabaseNames().size());
		
		return client.getDatabaseNames().size();
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		JobDataMap jobData = context.getJobDetail().getJobDataMap();
		queryMongo(jobData.getString("report"));
		
	}
	
}
