package com.ultimo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

import org.restheart.Configuration;
import org.restheart.db.DbsDAO;
import org.restheart.db.MongoDBClientSingleton;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ErrorSpotSinglton {

    private static boolean initialized = false;

    private static transient JSONObject frequency;
    private static transient JSONArray notifications;
    private static transient Map<String, JSONObject> notificationsMap;
    private static transient Map<String, Date> lastNotificationsTime;
    

    

    private MongoClient mongoClient;

    private static final Logger logger = LoggerFactory.getLogger(ErrorSpotSinglton.class);

    private ErrorSpotSinglton() {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

    }

    /**
     *
     * @param conf
     */
    public static void init() {

        DbsDAO dbs = new DbsDAO();
        String dbName = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
        String collName = MongoDBClientSingleton.getErrorSpotConfig("u-setting-collection");
        DBCollection settingColl = dbs.getCollection(dbName, collName);
        
        BasicDBObject query = new BasicDBObject();
        query.append("setting", new BasicDBObject("$ne", null));
        DBCursor cursor = settingColl.find(query);
    	
        if(cursor.size() == 1){
        	
        	DBObject setting = cursor.next();
        	DBObject settingObject = (DBObject)setting.get("setting");
        	DBObject notficationObject = (DBObject)settingObject.get("notification");
        	DBObject immidateObject = (DBObject)notficationObject.get("immidate");
        	
        	System.out.println(immidateObject.toMap().toString());
        	
        	notifications = new JSONArray(((DBObject)immidateObject.get("notification")).toString());
        	frequency = new JSONObject(((DBObject)immidateObject.get("frequency")).toString());
        	
        	//convert JSONArray into map for faster access.
        	for (int i = 0; i < notifications.length(); i++) {
        		
        		JSONObject currentNotification = notifications.optJSONObject(i);
        		JSONArray interfaces = currentNotification.optJSONObject("application").optJSONArray("interfaces");
        		
        		System.out.println(interfaces.toString());
        		System.out.println(currentNotification.toString());
        		
        		for(int j = 0; j < interfaces.length(); j++){
        			
        			String application = currentNotification.optJSONObject("application").getString("name");
        			String interfaceName = interfaces.getString(j);
        			String severity = currentNotification.getString("severity");
        			String envid = currentNotification.getString("envid");
        			
        			String key = envid.toUpperCase() + "." +application.toUpperCase() + "." + interfaceName.trim().toUpperCase() + "." + severity.toUpperCase();
        			
        			
        			notificationsMap = new HashMap<String, JSONObject>();
        			notificationsMap.put(key, currentNotification);
        			
        		}
        		
				
			}
        	
        	lastNotificationsTime = new HashMap<String, Date>();
        	
        	initialized = true;
        	
        }
        

    }
    
    public static boolean isNotificationConfigured(String envid, String application, String interfaceName, String severity){
    	boolean match = false;
    	
    	String key = envid.toUpperCase() + "." +application.toUpperCase() + "." + interfaceName.trim().toUpperCase() + "." + severity.toUpperCase();
    	
    	JSONObject configuredNotification = notificationsMap.get(key);
    	
    	if (configuredNotification != null)
    		match = true;
    	
    	
    	return match;
    }
    
    public static boolean checkNotificationAndTimeExpired(String envid, String application, String interfaceName, String severity){
    	
    	boolean expired = false;
    	
        boolean notificationExists = isNotificationConfigured(envid, application,interfaceName, severity);
    	
        String key = envid.toUpperCase() + "." +application.toUpperCase() + "." + interfaceName.trim().toUpperCase() + "." + severity.toUpperCase();
        
    	if (notificationExists){
    		Date lastNotiTime = lastNotificationsTime.get(key);
    		Date currentDate = new Date();
    		
    		if(lastNotiTime  !=  null){
    			
    		    long timeDiffrence = Math.abs(currentDate.getTime() - lastNotiTime.getTime());
    		    long notifcationPeriod = SettingService.calculateDurationInseconds(frequency.getInt("duration"), frequency.getString("unit"));
    		 
        		if(timeDiffrence/1000 > notifcationPeriod){
        			expired = true;
        			lastNotificationsTime.put(key, currentDate);
        		}
    		    
    		} else {
    			expired = true;
    			lastNotificationsTime.put(key, currentDate);
    		}
    		
    	}
    	return expired;	
    	
    }

    

    /**
     *
     * @return
     */
    public static ErrorSpotSinglton getInstance() {
        return ErrorSpotSingltonHolder.INSTANCE;
    }

    private static class ErrorSpotSingltonHolder {

        private static final ErrorSpotSinglton INSTANCE = new ErrorSpotSinglton();
    }

    /**
     *
     * @return
     */
    public MongoClient getClient() {
        if (this.mongoClient == null) {
            throw new IllegalStateException("mongo client not initialized");
        }

        return this.mongoClient;
    }

    /**
     * @return the initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

}
