package com.ultimo;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.restheart.db.MongoDBClientSingleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlNotificationFactory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");

	//use getJob method to get object of type NotificationJob 
	public NotificationTemplate getNotificationClass(String template){
		LOGGER.info("Getting the right job for template: "+template);
		template=template.replaceAll("\\.html", "");	
		try{
			// Create a File object on the root of the directory containing the class file
			File file = new File(MongoDBClientSingleton.getErrorSpotConfig("u-template-location"));
			    // Convert File to a URL
			    URL url = file.toURL();          // file:/c:/myclasses/
			    URL[] urls = new URL[]{url};
                for(URL url1 : urls){
                	System.out.println(url1.toString());
                }
			    // Create a new class loader with the directory
			    ClassLoader cl = new URLClassLoader(urls);

			    // Load in the class; MyClass.class should be located in
			    // the directory file:/c:/myclasses/com/mycompany
			    Class cls = cl.loadClass(template);
			
			NotificationTemplate notificationClass= (NotificationTemplate)cls.newInstance();
			LOGGER.info("found :"+notificationClass.getClass().toString());
			return notificationClass;
      	}
		catch (MalformedURLException e) {
		
		}
		catch (ClassNotFoundException e) {
			
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
