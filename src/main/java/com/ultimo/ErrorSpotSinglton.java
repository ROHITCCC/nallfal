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

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

import org.restheart.Configuration;
import org.restheart.db.DbsDAO;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.security.handlers.IAuthToken;
import org.restheart.utils.HttpStatus;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ErrorSpotSinglton implements IAuthToken{

    private static boolean initialized = false;

    private static transient JSONObject frequency;
    private static transient JSONArray notifications;
    private static transient Map<String, JSONObject> notificationsMap =  new HashMap<String, JSONObject>(); 
    private static transient Map<String, Date> lastNotificationsTime = new HashMap<String, Date>();;
    

    

    private MongoClient mongoClient;

    private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");

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
    	
        notificationsMap =  new HashMap<String, JSONObject>(); 

        DbsDAO dbs = new DbsDAO();
        String dbName = MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database");
        String collName = MongoDBClientSingleton.getErrorSpotConfig("u-setting-collection");
        DBCollection settingColl = dbs.getCollection(dbName, collName);
        
        BasicDBObject query = new BasicDBObject();
        query.append("setting", new BasicDBObject("$ne", null));
        LOGGER.debug("Searching in database 'u-mongodb-database' and collection 'u-setting-collection' for the settings document...");
        DBCursor cursor = settingColl.find(query);
    	
        if(cursor.size() == 1){
        	
        	LOGGER.debug("Settings document retrieved");
        	DBObject setting = cursor.next();
        	DBObject settingObject = (DBObject)setting.get("setting");
        	DBObject notficationObject = (DBObject)settingObject.get("notification");
        	DBObject immediateObject = (DBObject)notficationObject.get("immediate");
        	LOGGER.trace("Immediate Notification: " + immediateObject.toString());
        	
        	
        	notifications = new JSONArray(((DBObject)immediateObject.get("notification")).toString());
        	frequency = new JSONObject(((DBObject)immediateObject.get("frequency")).toString());
        	
        	//convert JSONArray into map for faster access.
        	boolean isImmediateNotificationExists = false;
        	try{
        	String firstAppName = notifications.optJSONObject(0).optJSONObject("application").getString("name");
        	if(firstAppName.length() > 0)
        		isImmediateNotificationExists = true;
        	
        	} catch (Exception e){
        		e.printStackTrace();
        	}
        	
        	if(notifications.length() >= 1 && isImmediateNotificationExists){
        		
	        	
	        	for (int i = 0; i < notifications.length(); i++) {
	        		
	        		JSONObject currentNotification = notifications.optJSONObject(i);
	        		JSONArray interfaces = currentNotification.optJSONObject("application").optJSONArray("interfaces");
	
	        		
	        		for(int j = 0; j < interfaces.length(); j++){
	        			String application = currentNotification.optJSONObject("application").getString("name");
	        			
	        			String interfaceName = interfaces.getString(j);
	        			String severity = currentNotification.getString("severity");
	        			String envid = currentNotification.getString("envid");
	        			
	        			String key = envid.toUpperCase() + "." +application.toUpperCase() + "." + interfaceName.trim().toUpperCase() + "." + severity.toUpperCase();
	        			
	        			LOGGER.debug("Key field values found in settings document: Application name - " + application + ", Interface name - " + interfaceName + ", Severity - " + severity + ", Envid - " + envid);
	        				        			
	        			notificationsMap.put(key, currentNotification);
	        			
	        		}
	        		
					
				}
	        	
	        	//lastNotificationsTime = new HashMap<String, Date>();
	        	
	        	initialized = true;
	        	LOGGER.debug("ErrorSpotSinglton has been initialized.");
        	}
        }
        

    }
    
    public static boolean isNotificationConfigured(String envid, String application, String interfaceName, String severity){
    	boolean match = false;
    	
    	String key = envid.toUpperCase() + "." +application.toUpperCase() + "." + interfaceName.trim().toUpperCase() + "." + severity.toUpperCase();
    	
    	JSONObject configuredNotification = notificationsMap.get(key);
    	
    	if (configuredNotification != null)
    	{
    		match = true;
    		LOGGER.debug("Notification has been configured.");
    	} else {
    		LOGGER.debug("No match found between the audit fields and settings document fields");
    	}
    	
    	return match;
    }
    
    /**
	 * @return the notificationsMap
	 */
	public static final Map<String, JSONObject> getNotificationsMap() {
		return notificationsMap;
	}

	public static JSONObject getExpiredNotificationDetail(String envid, String application, String interfaceName, String severity){
    	JSONObject configuredNotification = null;
    	boolean notificationExists = isNotificationConfigured(envid, application,interfaceName, severity);
    	
        String key = envid.toUpperCase() + "." +application.toUpperCase() + "." + interfaceName.trim().toUpperCase() + "." + severity.toUpperCase();
        
    	if (notificationExists){
    		Date lastNotiTime = lastNotificationsTime.get(key);
    		Date currentDate = new Date();
    		
    		if(lastNotiTime  !=  null){
    			
    		    long timeDiffrence = Math.abs(currentDate.getTime() - lastNotiTime.getTime());
    		    LOGGER.debug("Duration of last notification has been " + timeDiffrence/1000 + " seconds.");
    		    long notifcationPeriod = SchedulerService.calculateDurationInseconds(frequency.getInt("duration"), frequency.getString("unit"));
    		    LOGGER.debug("The intended duration of the last notification is " + notifcationPeriod + " seconds.");
    		 
        		if(timeDiffrence/1000 > notifcationPeriod){
        			LOGGER.debug("The previous notification has expired.");
        			lastNotificationsTime.put(key, currentDate);
        			configuredNotification = notificationsMap.get(key);
        		}
        		else
        		{
        			LOGGER.debug("Duration of previous notification has not yet expired.");
        		}
    		    
    		} else {
    			LOGGER.debug("There was no previous notification.");
    			lastNotificationsTime.put(key, currentDate);
    			configuredNotification = notificationsMap.get(key);
    		}
    		
    	}
    	return configuredNotification;	
    	
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
        	LOGGER.error("Mongo client not initialized.");
            throw new IllegalStateException("Mongo client not initialized");
        }

        return this.mongoClient;
    }

    /**
     * @return the initialized
     */
    public static boolean isInitialized() {
    	if (initialized)
    	{
    		LOGGER.debug("ErrorSpotSinglton is initialized.");
    	}
    	else
    	{
    		LOGGER.debug("ErrorSpotSinglton is not initialized.");
    	}
        return initialized;
    }

    public static void optionsMethod(HttpServerExchange exchange) {
    	
    	Collection<String> allowedMethods= new ArrayList<>();
		allowedMethods.add("GET");
		allowedMethods.add("POST");
		allowedMethods.add("DELETE");
		allowedMethods.add("PUT");
        exchange.getResponseHeaders().putAll(HttpString.tryFromString("Access-Control-Allow-Methods"), allowedMethods);
		LOGGER.debug("Added methods to the exchange");
        //exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Methods"), "POST");
        exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Headers"), "Accept, Accept-Encoding, Authorization, Env-ID, Content-Length, Content-Type, Host, Origin, X-Requested-With, User-Agent, No-Auth-Challenge, " + AUTH_TOKEN_HEADER + ", " + AUTH_TOKEN_VALID_HEADER + ", " + AUTH_TOKEN_LOCATION_HEADER);
        LOGGER.debug("Added headers to the exchange");
        exchange.setResponseCode(HttpStatus.SC_OK);
        exchange.endExchange();
    	
    }
    
}
