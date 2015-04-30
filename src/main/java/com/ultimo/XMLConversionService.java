package com.ultimo;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;

import org.json.*;
import org.restheart.db.MongoDBClientSingleton;

import com.mongodb.*;
import com.mongodb.util.JSON;


public class XMLConversionService 

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
         JSONObject jsonObj = XML.toJSONObject(inputXML); 
         String intermediateJSON = jsonObj.toString();
         intermediateJSON = "{\"payload\" : "+ intermediateJSON + "}";
	     JSONObject jsonObj2 = new JSONObject(intermediateJSON); 
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
			MongoClient mongo = MongoDBClientSingleton.getInstance().getClient();
			DB db = mongo.getDB(dbName);
			DBCollection collection = db.getCollection(collectionName);
			DBCursor cursorDoc = collection.find();
			String json = null;
			json = cursorDoc.next().get("payload").toString();
			System.out.println(json);
			JSONObject output = new JSONObject(json);

			/*	Code to Get and Print a JSON Array. 
			  JSONArray jsonArray = new JSONArray();
				while (cursorDoc.hasNext())
			{
				if (i!=0) 
				{
					json = json + "," + cursorDoc.next().get("payload").toString();

				}
				else
				{ 
					json = cursorDoc.next().toString();
					JSONObject jsonobj = new JSONObject(json);
					jsonobj.toJSONArray(jsonArray);
				}
				i++;
			}
			json = "[" + json + "]";
			System.out.println(json);
			json = json.replace("$oid" , "oid");
			System.out.println(json);
			JSONArray array2 = new JSONArray(json);

			System.out.println(array2.toString(1));
			*/
			
			return XML.toString(output);
			
			}
			catch(Exception e) 
			{
				e.printStackTrace();
				return "Error Happened";
			}
	}
	
	public static void main(String[] args) throws IOException
	{
		//String fileOutput = readXML("C:\\Users\\Vinay\\Desktop\\JavaInput.xml");
		//XMLConversion(fileOutput);
		String outputFromDB = MongoDBDocumentRead("172.16.120.70", 27017, "test", "payloadTest");
		System.out.println("output from DB:");
		System.out.println(outputFromDB);
		
		
	}


}
