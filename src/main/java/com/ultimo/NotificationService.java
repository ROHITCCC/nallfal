package com.ultimo;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import io.undertow.server.HttpServerExchange;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

public class NotificationService extends ApplicationLogicHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");

	public NotificationService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,
			RequestContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	public static void sendEmail(String content, String template, String hostname,int port,String toEmailID,String fromEmailID,String username,String password){
		try{
			JSONArray jsonArray = new JSONArray(content);
			File file = new File("C:/Users/Smita/EmailTest/"+template);
			
			//checks if template exists
			if(!file.exists()){
				LOGGER.error("the given template is not found");
				return;
			}
			LOGGER.info("The given template exists");
			Document doc = Jsoup.parse(file,null); //get the template
			
			//iterate through each elemnt in JSON array and add as rows on table
			for(int i=0; i<jsonArray.length();i++){
				//get the individual element of the objects from the array as a DBObject
				DBObject obj = (DBObject) JSON.parse(jsonArray.get(i).toString());
				
				if(!(obj.containsField("_id"))){
					LOGGER.error("object must contain the field id");
					return;
				}
				//get the fields embedded in id and create a new object with all fields in same level
				DBObject idObj = (DBObject) JSON.parse(obj.get("_id").toString());
				obj.removeField("_id");
				for(String key : idObj.keySet()){
					obj.put(key, idObj.get(key)); // puts the key value pair in id to the object level
				}
				
				//add the fields as rows
				LOGGER.info("inserting element "+(i+1));
				LOGGER.trace("inseting: "+obj.toString());
				doc = addRow(doc,obj);
				if(doc==null){
					LOGGER.error("inserting the rows had an error with element "+(i+1));
					return;
				}
				LOGGER.info("element "+ (i+1)+" sucessfully added as a table row");
			}
			LOGGER.trace(doc.toString());
			
			//send the email
			HtmlEmail email = new HtmlEmail();
			email.setHtmlMsg(doc.toString());
			email.setHostName(hostname);
			email.addTo(toEmailID);
			email.setFrom(fromEmailID);
			email.setAuthentication(username,password);
			email.setSmtpPort(port);
			email.send();
			LOGGER.info("Email was sent sucessfully");
		}
		catch(IOException e){
			LOGGER.error("the given file was not valid");
			e.printStackTrace();
		}
		catch(JSONParseException e){
			LOGGER.error("The given JSON array was not valid");
			e.printStackTrace();
		}
		catch(EmailException e){
			LOGGER.error("The email couldn't be sent");
			e.printStackTrace();
		}
		catch(Exception e){
			LOGGER.error("unspecified error");
			e.printStackTrace();
		}
	}
	
	public static Document addRow(Document doc, DBObject obj) throws Exception{
		doc.select("tbody").first().append("<tr> </tr>"); //adds a new row
		Element element = doc.select("tr").last(); // get all the new row
		Elements headerElements = doc.select("th"); //get all the headers to check if they match
		
		//converts the elements object into array of elements
		Element [] headerArray = new Element[headerElements.size()]; 
		headerArray = headerElements.toArray(headerArray);
		LOGGER.trace(headerArray.toString());
		
		//converts set of keys into an array of Stings
		String [] keys = new String[obj.keySet().size()];
		keys=obj.keySet().toArray(keys);
		LOGGER.trace(keys.toString());
		
		LOGGER.info("checking compatibility of # of columns and number of fields");
		//checks the compatiablity of the row and number of keys to be added
		if(keys.length!=headerArray.length){
			LOGGER.error("the number of coulmns: "+headerArray.length+ " doesnt match the number of fields: "+keys.length);
			return null;
		}
		
		//adds the row if the JSON key matches the HTMLheaders
		for(int i=keys.length-1, j=0; i>=0; i--,j++){
			//The json keys are in reverse order
			if(headerArray[j].text().equalsIgnoreCase(keys[i])){ //checks if the JSON key matches the HTML Header(in reverse order)
				LOGGER.info("the JSON key matches the HTMLheaders");
				element.append("<td>"+obj.get(keys[i])+"</td>"); //adds the columns to the new row
			}
			else{
				LOGGER.error("The JSON key: " +keys[i]+" doesn't match the HTML header: "+headerArray[j].text());
				return null;
			}
		}
		return doc;
	}

}
