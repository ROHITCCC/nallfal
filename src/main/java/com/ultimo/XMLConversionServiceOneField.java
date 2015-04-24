package com.ultimo;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import org.json.*;
import com.mongodb.*;
import com.mongodb.util.JSON;

public class XMLConversionServiceOneField 
{

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
		return sb.toString();
		
	}
	
	
	public static void XMLConversion(String inputXML) throws UnknownHostException
	{
		inputXML = inputXML.replace("\"", "&quot");
        String intermediateJSON = "{\"payload\" : \""+ inputXML + "\"}";
        System.out.println(intermediateJSON);
	    JSONObject jsonObj2 = new JSONObject(intermediateJSON);
	    System.out.println(jsonObj2.toString());
        MongoDBDocumentWrite(jsonObj2.toString());
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
	
	public static void main(String[] args) throws IOException 
	{

	/*	String fileOutput = readXML("C:\\Users\\Vinay\\Desktop\\JavaInput.xml");
		System.out.println(fileOutput);
		XMLConversion(fileOutput);
		String outputFromDB = MongoDBDocumentRead("172.16.120.70", 27017, "test", "payloadTest");
		System.out.println("output from DB:");
		System.out.println(outputFromDB);
*/
		
		
	}

}
