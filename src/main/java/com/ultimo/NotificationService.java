package com.ultimo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import io.undertow.server.HttpServerExchange;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

public class NotificationService extends ApplicationLogicHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");
	
	public static String hostname="";
	public static String port="";
	public static String fromEmailId="";
	public static String username="";
	public static String password="";
	public static String location="";
	
	public static void setEmailConfigs(){
		LOGGER.info("getting the configurations for the email");
		List<Map<String, Object>> configList = MongoDBClientSingleton.getErrorSpotConfigs();
		ListIterator<Map<String, Object>> iterator= configList.listIterator();
		while(iterator.hasNext()){
			Map<String, Object> configMap= iterator.next();
			if(configMap.get("what").toString().equals("u-smtp-server")){
				hostname=configMap.get("where").toString();
			}
			else if(configMap.get("what").toString().equals("u-smtp-port")){
				port = configMap.get("where").toString();
			}
			else if(configMap.get("what").toString().equals("u-default-from-email-address")){
				fromEmailId=configMap.get("where").toString();
			}
			else if(configMap.get("what").toString().equals("u-email-username")){
				username=configMap.get("where").toString();
			}
			else if(configMap.get("what").toString().equals("u-email-password")){
				password=configMap.get("where").toString();
			}
			else if(configMap.get("what").toString().equals("u-template-location")){
				location=configMap.get("where").toString();
			}
		}
		LOGGER.info("got the email erver, port, emailID, username, password, and the location of the template file. hostname: "+hostname+" port: "+port+" emailID: "+fromEmailId+" username: "+username+" password: "+password+" location of template: "+location);
	}

	public NotificationService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,
			RequestContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public static boolean validateTemplate(String template){
		LOGGER.info("checking to see if template: "+template+" exists and has a class associated with it");
		location=MongoDBClientSingleton.getErrorSpotConfig("u-template-location");
		LOGGER.info("getting the location to look for from the config file. checking for the template in location:"+location);
		File file = new File(location+"/"+template);
		
		//checks if template exists
		if(!file.exists()){
			LOGGER.error("the given template: "+template+" is not found in location: "+location);
			return false;
		}
		LOGGER.info("The given template: "+template+" exists");
		try{
			template=template.replaceAll("\\.html", "");
			LOGGER.info("checking if "+template+".class exists in location: "+location);
			File classFile= new File(location+"/"+template+".class");
			if(!(classFile.exists())){
				LOGGER.info("class is not in the given location, checking to see if it exists in the default package: com.ultimo");
				Class.forName("com.ultimo."+template);
			}
			LOGGER.info("the template has  "+template+".class associated with it, and therefore is a valid template");
			return true;
		}
		catch(ClassNotFoundException e){
			LOGGER.error("the given template: "+template+" does not have a class associated with it");
			return false;
		}
	}
	
	public static void sendEmail(String content, String template, String toEmailID, String subject){
		try{
			LOGGER.info("Notification Service Started");
			LOGGER.trace("content recieved: "+content);
			LOGGER.trace("the template that is being used:"+ template);
			LOGGER.info("getting email information form the configuration file");
			LOGGER.trace("sending email to: "+toEmailID);
			setEmailConfigs();
			//get the specific job depending on the template
			HtmlNotificationFactory emailFactory= new HtmlNotificationFactory();
			NotificationTemplate emailNotification = emailFactory.getNotificationClass(template);
			Document doc = emailNotification.createEmail(content, location, template);
			
			//send the email
			HtmlEmail email = new HtmlEmail();
			email.setHtmlMsg(doc.toString());
			email.setHostName(hostname);
			email.addTo(toEmailID);
			email.setFrom(fromEmailId);
			email.setSubject(subject);
			email.setAuthentication(username,password);
			email.setSmtpPort(Integer.parseInt(port));
			email.send();
			LOGGER.info("Email was sent sucessfully");
		}
		catch(EmailException e){
			LOGGER.error("The email couldn't be sent");
			LOGGER.error("the error: ",e);
		}
		catch(NullPointerException e){
			LOGGER.error("the passed report had invalid structure and therefore could not be processed");
			LOGGER.error("the error: ",e);
		}
		catch(Exception e){
			LOGGER.error("unspecified error");
			LOGGER.error("the error: ",e);
		}
	}
	
	

}
