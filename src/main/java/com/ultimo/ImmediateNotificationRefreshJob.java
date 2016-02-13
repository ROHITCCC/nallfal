package com.ultimo;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@DisallowConcurrentExecution
public class ImmediateNotificationRefreshJob implements Job{

	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		LOGGER.info("Excecuting ImmidateNotificationRefreshJob");
		
		try{
		ErrorSpotSinglton.init();
		LOGGER.info("Initiaization of Immidate Notification setting is done.") ;
		LOGGER.debug("Immidate Notification setting: " + ErrorSpotSinglton.getNotificationsMap().toString()) ;
		}
		catch (Exception e){
			LOGGER.error("Error initilizing Immediate Notification from database. ");
			e.printStackTrace();
		}
//		MongoClient client = MongoDBClientSingleton.getInstance().getClient();
//		String dbname = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
//		String collectionName = MongoDBClientSingleton.getErrorSpotConfig("u-setting-collection");
//        DB database = client.getDB(dbname);
//        DBCollection collection = database.getCollection(collectionName);
//        LOGGER.debug("Connected to mongo db: "+ dbname + ", collection "+  collectionName +  " to get \"setting\" document");
//       
//        BasicDBObject whereQuery = new BasicDBObject();
//		//overwrite setting if it exists. If it doesn't exist, adds a new one
//		whereQuery.put("setting", new BasicDBObject("$ne", null));
//		//LOGGER.trace(whereQuery.toString());
//		LOGGER.debug("Searching for \"setting\" document with given qualifications: "+whereQuery.toString());
//		DBCursor cursor = collection.find(whereQuery);
//        
//		
//		if(cursor.size()==1){
//			//if there is already a document with the given fields and values as whereQuery, 
//			//overwrite the document.
//			LOGGER.debug("\"setting\" document exists");
//			DBObject doc = cursor.next();
//			
//			LOGGER.debug("\"setting\" document: " + doc.toString());
//			
//			DBObject settingObject = (DBObject)doc.get("setting");
//			DBObject immediateObject = (DBObject)((DBObject)settingObject.get("notification"))
//																	.get("immediate");
//			
//			DBObject jobRefreshRateObject = (DBObject)immediateObject.get("jobRefreshRate");
//			
//			if(jobRefreshRateObject != null && jobRefreshRateObject.get("duration") != null ){
//				
//				
//			}else{
//				LOGGER.error("\"jobRefreshRate\" is not found or not correct format under  \"setting\" -> \"notification\" -> \"immediate\". Setting Object = " + doc.toString());
//			}
//			
//			
//			
//			
//		}else if(cursor.size() > 1){
//			LOGGER.info(" Multiple Setting document  exists. Only one \"setting\" document is expected. Found " + cursor.size() + " documents");
//			return;
//		}
//		else{
//			
//			LOGGER.info(" Setting document does not exists. Check database for existence of document with \"setting\" object");
//			return;
//		}
//   
		
	}
}
