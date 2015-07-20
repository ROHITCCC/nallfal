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
	ClassLoader cl;

	//use getJob method to get object of type NotificationJob 
	public NotificationTemplate getNotificationClass(String template){
		LOGGER.info("Started HTMLNotificationFactory");
		LOGGER.info("Getting the right class for template: "+template);
		String location="";
		template=template.replaceAll("\\.html", "");
		LOGGER.info("Looking for class: "+template+".class");
		try{
			Class<?> cls=null;
			if(template.equals("ImmediateNotification") || template.equals("ReportNotification")){
				//if it is ImmediateNotification or ReportNotification, load the default classes form the com.ultimo package
				location="com.ultimo.";
				LOGGER.info("a default Notification is specified, therefore looking under com.ultimo");
				cls=Class.forName(location+template);
			}
			else{
				// Create a File object on the root of the directory containing the class file
				location= MongoDBClientSingleton.getErrorSpotConfig("u-template-location");
				LOGGER.info("The class isn't a defual one provided by the package; therefore, looking for class under: "+location);
				LOGGER.info("the template file and the class file must be under the same file");
				File file = new File(location);
			    // Convert File to a URL
			    URL url = file.toURI().toURL();
			    LOGGER.trace("The url of the file: "+url.toString());
			    URL[] urls = new URL[]{url};
			    // Create a new class loader with the directory
			    cl = new URLClassLoader(urls);
			    // Load in the class; MyClass.class should be located in
			    // the directory file:/c:/myclasses/com/mycompany
			    cls = cl.loadClass(template);
			}
			NotificationTemplate notificationClass= (NotificationTemplate)cls.newInstance();
			LOGGER.info("found :"+notificationClass.getClass().toString());
			return notificationClass;
      	}
		catch (MalformedURLException e) {
			LOGGER.error("the given file couldn't be converted to a url");
			LOGGER.error("the error: ",e);
		}
		catch (ClassNotFoundException e) {
			LOGGER.error(template+".class could not be found in location: "+location);
			LOGGER.error("the error: ",e);
		}
		catch (ClassCastException e){
			LOGGER.error("the given class: "+template+" does not implement NotificationJob");
			LOGGER.error("the error: ",e);
		} 
		catch (Exception e){
			LOGGER.error("unspecified error");
			LOGGER.error("the error: ",e);
		}
		return null;
	}
}
