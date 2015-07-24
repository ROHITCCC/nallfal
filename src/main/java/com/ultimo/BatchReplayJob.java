package com.ultimo;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.bson.BSONObject;
import org.json.JSONObject;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.restheart.db.MongoDBClientSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.util.JSON;

@DisallowConcurrentExecution
public class BatchReplayJob implements Job{

	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		LOGGER.info("excecuting BatchReplayJob");
		//connect to appropriate cdb and collection
		MongoClient client = MongoDBClientSingleton.getInstance().getClient();
		String dbname = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
		String collectionName = MongoDBClientSingleton.getErrorSpotConfig("u-batch-replay-collection");
        DB database = client.getDB(dbname);
        DBCollection collection = database.getCollection(collectionName);
        LOGGER.trace("connected to db: "+dbname);
        LOGGER.info("connected to collection: "+collectionName);
        
        //query the documents the do not have status as processed or processing
        LOGGER.info("querring the documents whose status field in not processed or processing");
        BasicDBList queryList = new BasicDBList();
        queryList.add("processed");
        queryList.add("processing");
        BasicDBObject whereQuery = new BasicDBObject("status", new BasicDBObject("$nin", queryList));
        LOGGER.trace("the query's fields: "+whereQuery.toString());
		DBCursor cursor= collection.find(whereQuery).sort((DBObject)JSON.parse("{ \"replaySavedTimestamp\": 1 }"));
		LOGGER.info("found "+cursor.size()+" document in the database that match this criteria");
		
		while(cursor.hasNext()){
			//get the document
			DBObject document = cursor.next();
	        LOGGER.info("processing document: "+document.get("_id").toString());
	        LOGGER.trace("docuemnt: "+document.toString());
	        
	        //change status to processing
			DBObject processingDocument = (DBObject)JSON.parse(document.toString());
			processingDocument.removeField("status");
			processingDocument.put("status", "processing");
			collection.update(document,processingDocument);
			
			//make the batch call with it
			Map<String, String> failedAuditsMap=null;
			try {
				failedAuditsMap = BatchReplayService.BatchHandleRequest(new JSONObject(document.toString()));
			} catch (Exception e) {
				
				//change status to failed
				DBObject failedDocument = (DBObject)JSON.parse(document.toString());
				failedDocument.removeField("status");
				failedDocument.put("status", "failed");
				collection.update(processingDocument,failedDocument);
				LOGGER.error("unspecified error with BatchReplayService's BatchHandleRequest/handleBatch");
				LOGGER.error("the error: ",e);
			}
			LOGGER.info(failedAuditsMap.size()+" audits failed");
			
			//save all the failed Audits in a separate document to replay next time
			if(failedAuditsMap.size()!=0){
				//create the document to have the same fields as the parent but with new field parentId, 
				//and audit ID's change to  the ones that failed and status changed to failed
				LOGGER.info("adding failed audits into seperate document");
				DBObject failedDocument= (DBObject)JSON.parse(document.toString());
	 			failedDocument.removeField("_id");
	 			failedDocument.removeField("status");
	 			failedDocument.removeField("auditID");
	 			failedDocument.put("status","failed");
	 			failedDocument.put("parentID", document.get("_id").toString());
	 			LOGGER.info("failed document's parnet's ID:"+document.get("_id").toString());
	 			
	 			//get the failed ID's in a list to pass as an array
	 			BasicDBList failedAuditList = new BasicDBList();
	 			for(String key: failedAuditsMap.keySet()){
	 				LOGGER.info("adding failed auditID: "+key);
	 				failedAuditList.add(key);
	 			}
	 			BasicDBObject failedObject = new BasicDBObject("auditID",failedAuditList);
	 			failedDocument.putAll((BSONObject)failedObject);
	 			
	 			//insert the fialed docuemnt
	 			collection.insert(failedDocument);
	 			LOGGER.info("added a document with failed auditID's");
	 			LOGGER.trace("the document's id: "+failedDocument.get("id"));
			}
			//change the document to processed and update batchProcessedTimestamp
			DBObject processedDocuemnt = (DBObject)JSON.parse(processingDocument.toString());
			processedDocuemnt.removeField("status");
			processedDocuemnt.put("status", "processed");
			processedDocuemnt.removeField("batchProcessedTimestamp");
			processedDocuemnt.put("batchProcessedTimestamp", new Date());
			collection.update(processingDocument,processedDocuemnt);
			LOGGER.info("the batchProcessedTimestamp: "+context.getFireTime());
			LOGGER.trace("the processed doument: "+processedDocuemnt.toString());
		}
	}
}
