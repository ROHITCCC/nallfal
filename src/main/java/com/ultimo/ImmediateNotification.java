package com.ultimo;

import java.io.File;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.util.JSON;

public class ImmediateNotification implements NotificationTemplate{

	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");

	@Override
	public Document createEmail(String content, String location, String template) {
		
		try{

			BasicDBObject audit = (BasicDBObject)JSON.parse(content);
			File file = new File(location+"/"+template);
			LOGGER.debug("Looking for template in " + location);
			
			//Retrieves template
			Document doc = Jsoup.parse(file,null); 
			LOGGER.debug("Template has been retrieved.");
			
			boolean row = false;
			//Store the fields and values together in rows of the template
			for(String key : audit.keySet()){
				if (!row) {
					doc.select("table").first().append("<tr><th class=\"def\">" + key + "</th><td class=\"def\">" + audit.get(key) + "</td></tr>");
				}
				else{
					doc.select("table").first().append("<tr><th class=\"alt\">" + key + "</th><td class=\"alt\">" + audit.get(key) + "</td></tr>");
				}
				row = !row;
				LOGGER.info("Stored field \"" + key + "\" with value \"" + audit.get(key) + "\" as a row in the table.");
			}
			
			LOGGER.trace(doc.toString());
			return doc;
		}
		catch(IOException e){
			LOGGER.error("The given file is not valid");
			e.printStackTrace();
		}
		catch(Exception e){
			LOGGER.error("Unspecified error");
			e.printStackTrace();
		}
		return null;
		
	}

}
