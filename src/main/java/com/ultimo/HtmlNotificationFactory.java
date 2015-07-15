package com.ultimo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlNotificationFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");

	//use getJob method to get object of type NotificationJob 
	public NotificationTemplate getNotificationClass(String template){
		LOGGER.info("Getting the right job for template: "+template);
		template=template.replaceAll("\\.html", "");	
		try{
			NotificationTemplate notificationClass= (NotificationTemplate)Class.forName("com.ultimo."+template).newInstance();
			LOGGER.info("found :"+notificationClass.getClass().toString());
			return notificationClass;
      	}
		catch (ClassCastException e){
			LOGGER.error("the given class: "+template+" does not implement NotificationJob");
			e.printStackTrace();
		} 
		catch (Exception e){
			LOGGER.error("unspecified error");
			e.printStackTrace();
		}
		return null;
	}
}
