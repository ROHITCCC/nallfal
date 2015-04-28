package com.ultimo;
import io.undertow.server.HttpServerExchange;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;

import org.json.*;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;

import com.mongodb.*;
import com.mongodb.util.JSON;

public class XMLConversionServiceOneField extends ApplicationLogicHandler
{

	public XMLConversionServiceOneField(PipedHttpHandler next,
			Map<String, Object> args) {
		super(next, args);
		// TODO Auto-generated constructor stub
	}


	public static String readXML(String file) throws IOException 
	{
		FileReader inputXMLFile = new FileReader(file);
		BufferedReader br = new BufferedReader(inputXMLFile);
		String line;
		StringBuilder sb = new StringBuilder();
		while((line=br.readLine())!= null){
		    sb.append(line.trim());
		}
		br.close();
		return XMLConversion(sb.toString());
		
	}
	
	
	public static String  XMLConversion(String inputXML) throws UnknownHostException
	{
		inputXML = inputXML.replace("\"", "&quot");
        String intermediateJSON = "{\"payload\" : \""+ inputXML + "\"}";
        System.out.println(intermediateJSON);
	    JSONObject jsonObj2 = new JSONObject(intermediateJSON);
	    System.out.println(jsonObj2.toString());
        //MongoDBDocumentWrite(jsonObj2.toString());
		return jsonObj2.toString();
	}
	
	public static void MongoDBDocumentWrite(String inputJSON) throws UnknownHostException
	 
	{		
		Mongo mongo = new Mongo("172.16.120.70", 27017);
		DB db = mongo.getDB("test");
		DBCollection collection = db.getCollection("payloadTest");
		DBObject dbObject = (DBObject) JSON.parse(inputJSON);
		collection.insert(dbObject);
	}			

	public static String MongoDBDocumentRead( String host, int port, String dbName,String collectionName)
	{
		try{
		Mongo mongo = new Mongo(host, port);
		DB db = mongo.getDB(dbName);
		DBCollection collection = db.getCollection(collectionName);
		DBCursor cursorDoc = collection.find();
		String json = null;
		JSONArray jsonArray = new JSONArray();
		json = cursorDoc.next().get("payload").toString();
		System.out.println(json);
		json= json.replace("&quot", "\"");
		return json;
		
		}
		catch(Exception e) 
		{
			e.printStackTrace();
			return "Error Happened";
		}
	}
	
	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception 
	{
				String inputXMLFile = exchange.toString();
			System.out.println(inputXMLFile.toString());
		String fileOutput = readXML("C:\\Users\\Vinay\\Desktop\\JavaInput.xml");
		
		 if (context.getMethod() == METHOD.GET) {
	            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_OK, fileOutput);
	        } else {
	            exchange.setResponseCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
	            exchange.endExchange();
	        }
		

	}

}
