package com.ultimo;

import io.undertow.server.HttpServerExchange;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.net.ftp.FTPClient;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restheart.db.MongoDBClientSingleton;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.security.handlers.IAuthToken;
import org.restheart.utils.ResponseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;


public class ReplayService extends ApplicationLogicHandler implements IAuthToken 
{

	public ReplayService(PipedHttpHandler next, Map<String, Object> args) 
	{
		super(next, args);
		
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger("org.restheart");

	@Override
	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception
	{
		String delimiter = "";
		String payload = "";
		JSONObject request = null;
		InputStream input = exchange.getInputStream();
		BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
		int i = 0;
		readLoop : while(true)
				{
					
					String inputTemp = inputReader.readLine();
					if (inputTemp!= null)
					{
						if (i == 0)
						{
							delimiter = (inputTemp.split(";"))[1];
							payload = payload + inputTemp + "\r\n";
						}
						else 
						{
							payload = (payload + inputTemp + "\r\n");
						}
						i++;
					}
					else 
					{
						break readLoop;
					}
				}
		delimiter = delimiter.split("=")[1]; 
		//============================================
	    byte[] boundary = delimiter.getBytes();
	    byte[] contents = payload.getBytes();
        ByteArrayInputStream content = new ByteArrayInputStream(contents);
        MultipartStream multipartStream = new MultipartStream(content, boundary,1000, null);	        
        int m = 0;
		boolean nextPart = multipartStream.skipPreamble();
		while (nextPart)
		{
			if (m==0)
			{
				ByteArrayOutputStream body = new ByteArrayOutputStream();
				multipartStream.readHeaders();
		        multipartStream.readBodyData(body);
		        String jsonRequest = new String(body.toByteArray());
		        request = new JSONObject(jsonRequest);
		        nextPart = multipartStream.skipPreamble();
		        m++;
			}
			else if (m==1)
			{
				ByteArrayOutputStream body = new ByteArrayOutputStream();
				multipartStream.readHeaders();
		        multipartStream.readBodyData(body);
		        payload = new String(body.toByteArray());
		        nextPart = multipartStream.skipPreamble();
		        m++;
			}
		}
		String result = handleReplays(request, payload);
		if (result.equals("Success"))
		{
			ResponseHelper.endExchangeWithMessage(exchange, 200, result);
		}
		else 
		{
			ResponseHelper.endExchangeWithMessage(exchange, 500, result);
		}
	}
	public static String handleReplays(JSONObject input, String payload) throws Exception
	{
		 String result = "";
		 String handleAble = "";
			if (input.getString("type").equalsIgnoreCase("REST"))
			{
				result = handleRest(input, payload);
			}
			else if (input.getString("type").equalsIgnoreCase("file"))
			{
				result = handleFile(input, payload);
			}
			else if (input.getString("type").equalsIgnoreCase("FTP"))
			{
				result = handleFTP(input, payload);
			}
			System.out.println(result);
			
			updateAudit(input,result);
			return result;
	}
	
	public static String handleRest(JSONObject connectionDetails , String payload)
	{
		try 
		{
			String endpoint = connectionDetails.getString("endpoint");
			String restMethod = connectionDetails.getString("method");
			String contentType = connectionDetails.getString("content-type");
			JSONArray headers = new JSONArray();
			if (connectionDetails.has("restHeaders"))
			{
			 headers = connectionDetails.getJSONArray("restHeaders");
			}
			LOGGER.trace("Replay Request Endpoint: " + endpoint);
			LOGGER.trace("Replay Request Method: " + restMethod);
			LOGGER.trace("Replay Request Content Type: " + contentType);
			LOGGER.trace("Replay Request Headers: " + headers);
			LOGGER.trace("Replay Request Payload: " + payload);
			
	    	URL url;
	    	url = new URL(endpoint);
		    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
	        httpCon.setDoInput(true);
	        httpCon.setDoOutput(true);
	        httpCon.setRequestMethod(restMethod);
	        httpCon.setRequestProperty("Content-Type", contentType);
	        System.out.println("VINAYYYYYY" + headers.toString());  
	        if (!headers.toString().equals("[]"));
	        {
		        for (int iterator = 0; iterator < headers.length();iterator++)
		        {
		         	JSONObject s = headers.getJSONObject(iterator);
		          	httpCon.setRequestProperty(s.getString("type"), s.getString("value"));
		        }
	        }
	        OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream(), "UTF-8");
	        out.write(payload);
	        out.close();
	        String line = "";
	        String result = "";
	        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
			while((line = bufferedReader.readLine()) != null){
                result += line;
            }
             System.out.println(result);
            bufferedReader.close();

            if (httpCon.getResponseCode() == HttpURLConnection.HTTP_OK) {
			     
                LOGGER.trace(httpCon.getResponseCode() + ": " + httpCon.getResponseMessage());
                LOGGER.info("REST Successful");
 
            } 
            else {
            	LOGGER.error(httpCon.getResponseCode() + ": " + httpCon.getResponseMessage());
				
            }
		} 
		catch (MalformedURLException e) 
		{
			LOGGER.error("URL is not structured properly. Please resubmit request with a proper URL");
			e.printStackTrace();
			return "URL is not structured properly. Please resubmit request with a proper URL";
		} 
		catch (IOException e) 
		{
			LOGGER.error("Connection Failed. " + e.getMessage());
			e.printStackTrace();
			return "Connection Failed. " + e.getMessage() + "\n" + "Please verify all connection details";
		}
		catch(JSONException e)
		{
			LOGGER.error("URL is not structured properly. Please resubmit request with a proper URL");

			
			if (e.getMessage().contains("not found"))
			{
				LOGGER.error(e.getMessage().split("\\[")[1].split("\\]")[0] + " was not found in the request. Please enter a valid value and resubmit the request");
				return e.getMessage().split("\\[")[1].split("\\]")[0] + " was not found in the request. Please enter a valid value and resubmit the request"; 
			}
			e.printStackTrace();
			
			return e.getMessage();
		}
		
		return "Success";
		
	}

	public static String handleFile(JSONObject connectionDetails, String payload)
	{
		String filePath = "";
		if (!connectionDetails.has("fileType"))
		{
			return "No File Type Present";
		}
		else if (!connectionDetails.has("fileLocation"))
		{
			return "No File Location Present";
		}
		
			String fileType = connectionDetails.getString("fileType");
			String fileLocation = connectionDetails.getString("fileLocation");
			if (!fileLocation.endsWith("/"))
			{
				fileLocation = fileLocation + "/";
			}
			String fileName =""; 
			if (connectionDetails.has("fileName"))
			{
				fileName = connectionDetails.getString("fileName");
	
				filePath  = fileLocation + fileName + fileType;
			}
			else 
			{
				String id = connectionDetails.getString("auditID");
				Calendar cal = Calendar.getInstance();
				DateFormat dateFormat = new SimpleDateFormat("MM_dd_yyyy_HH_mm_ss_ms_");
				String sysDate = dateFormat.format(cal.getTime());
				filePath = fileLocation +  sysDate + id + fileType  ;
			}
			
			File file = new File(filePath);
			System.out.println(file.toString());
			String output = "Success";
			try 
			{
				if (file.exists()) 
				{
					LOGGER.warn("Duplicate file name found at: " + fileLocation + ". + Contents in File are overwritten");
				}
				else
				{
					file.createNewFile();
				}
					FileWriter fWriter = new FileWriter(file.getAbsoluteFile(), false);
					BufferedWriter bWriter = new BufferedWriter(fWriter);
					bWriter.write(payload);											
					LOGGER.info("Payload has been written");
					bWriter.close();
					fWriter.close();
					LOGGER.info("Writing to file : " + fileLocation);
					return output;
			}
			catch (IOException e) 
			{
				 LOGGER.error(e.getMessage());
					e.printStackTrace();
					return e.getMessage();
			}
	}
	
	public static String handleFTP(JSONObject input, String payload) throws Exception{
		LOGGER.info("Starting FTP service");
		FTPClient ftp= new FTPClient();
		
		
		// break input string to its contents
		String hostname= input.getString("host");
		String username= input.getString("username");
		String password= input.getString("password");
		String location= input.getString("location");
		if (!location.endsWith("/"))
		{
			location = location + "/";
		}
		String auditID = input.getString("auditID");
		String output = "";
		String file="";
		String fileType=input.getString("fileType");
		String ftpPayload=payload;
		
		//filename parameter passed
		if(input.has("fileName")){
			LOGGER.info("file passed");
			file= input.getString("fileName");
		}
		//filename parameter not passed in
		else{
			LOGGER.info("no file parameter passed");
		}
		
		//if file is not passed in or is blank
		if(file.equals("")){
			LOGGER.info("no file detected, so creating default with timestamp");
			Calendar calender = Calendar.getInstance();
			Date date=calender.getTime();
			String dt = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_").format(date);
			file=dt +auditID;
		}
		
		//file validation
		boolean valid=true;
		for(int i=0;i<file.length();i++){
			char ch=file.charAt(i);
			if(!Character.isLetterOrDigit(ch) && ch!='_'){
				valid=false;
			}
		}
		if(valid){
			LOGGER.info("filename has valid characters");
		}
		else{
			LOGGER.error("incorrect file name");
        	return "File name contains incorrect character: only aphanumaeric and underscore allowed";
		}
		
		String filename=location+file+fileType;
		
		//logging trace messages
		LOGGER.trace("host: "+hostname);
		LOGGER.trace("username: "+username);
		LOGGER.trace("password: "+password);
		LOGGER.trace("filename: "+filename);
		LOGGER.trace("payload: "+ftpPayload);
		
		//step checks
		boolean connected=false;
		boolean loggedIn=false;
		
		//connect to server
		try {
			LOGGER.info("connecting to server: "+hostname);
			ftp.connect(hostname, 21);
			LOGGER.info("successfully connected to server: "+hostname);
			connected=true;
		} catch (IOException e) {
			LOGGER.error("incorrect host: "+hostname);
			output = "Host is invalid";
		}
		
		//login to server with given credentials
		if(connected){
			try {
				LOGGER.info("logging in");
				LOGGER.trace("logging in with username: "+username+" and password: "+password);
				ftp.login(username, password);
				loggedIn=true;
				LOGGER.info("login successful");
			} catch (IOException e) {
				LOGGER.error("login failed");
				output = "Username or password is incorrect";
			}
		}
		
		//add payload to file on the server
		if(loggedIn){
			
				LOGGER.info("Payload format correct");
				//convert payload string to inputStream
				InputStream ftpPayloadInput = new ByteArrayInputStream(ftpPayload.getBytes(Charset.forName("UTF-8")));
				try{
					LOGGER.info("storing payload into file");
					LOGGER.warn("if file already exists in directory, new information will overwrite the exisiting");
					boolean stored = ftp.storeFile(filename,ftpPayloadInput);
					if(stored){
						LOGGER.info("successfully stored payload into file");
						output =  "Success";
					}
					else{
						//if you are trying to input to a nonexsisting direcotry
						LOGGER.info("creating new directories");
						String [] directories = filename.split("/");
						for(int i=0;i<(directories.length-1);i++){
							boolean dirExists = ftp.changeWorkingDirectory(directories[i]);
							if(!dirExists){
								LOGGER.trace("creating new directory: "+directories[i]);
								ftp.makeDirectory(directories[i]);
								ftp.changeWorkingDirectory(directories[i]);
							}
						}
						boolean storedNewDir=ftp.storeFile(directories[directories.length-1],ftpPayloadInput);
						if(storedNewDir){
							LOGGER.trace("payload stored in direcotry: "+directories[directories.length-2]);
							LOGGER.info("successfully stored payload into file");
							output =  "Success";
						}
						else{
							LOGGER.error("storing file failed: incorrect filepath");
							output =  "Incorrect file path";
						}
					}
				}
				catch (IOException e){
					LOGGER.error("login failed: incorrect filepath");
					output =  "Incorrect file path";
				}
			
		}
		
		//logout of server
		if(loggedIn){
			LOGGER.trace("attempting to log out of server: "+hostname);
			ftp.logout();
			LOGGER.info("logout successful");
		}
		
		//disconnect from server
		if(connected){
			LOGGER.trace("attempting to disconnect form server: "+hostname);
			ftp.disconnect();
			LOGGER.info("disconnected successfully");
		}
		
		return output;
		
	}

	public static void updateAudit(JSONObject input, String status) throws ParseException
	{
		
			MongoClient client = MongoDBClientSingleton.getInstance().getClient();
			DB database = client.getDB(MongoDBClientSingleton.getErrorSpotConfig("u-mongodb-database"));
			DBCollection auditCollection = database.getCollection(MongoDBClientSingleton.getErrorSpotConfig("u-audit-collection"));
			ObjectId auditID = new ObjectId(input.getString("auditID"));
			DBObject audit = auditCollection.findOne(auditID);
			System.out.println(audit.toString());
			BasicDBList replayInformation;
			
			if (audit.containsField("replayInfo"))
			{
				replayInformation =  (BasicDBList) audit.get("replayInfo");	
				BasicDBObject replayInfo = new BasicDBObject("replayedBy",input.getString("replayedBy"));
				replayInfo.put("status",status);
				Calendar cal = Calendar.getInstance();
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				String sysDate = dateFormat.format(cal.getTime());
		        Date gtDate = dateFormat.parse(sysDate); 
				replayInfo.put("replayTimestamp",gtDate);
				System.out.println(replayInfo.toString());
				replayInformation.add(replayInfo);
			}
			else
			{
				replayInformation =  new BasicDBList();
				BasicDBObject replayInfo = new BasicDBObject("replayedBy",input.getString("replayedBy"));
				replayInfo.put("status",status);
				Calendar cal = Calendar.getInstance();
				DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				String sysDate = dateFormat.format(cal.getTime());
		        Date gtDate = dateFormat.parse(sysDate); 
				replayInfo.put("replayTimestamp",gtDate);
				System.out.println(replayInfo.toString());
				replayInformation.add(replayInfo);
				System.out.println("YESSS" + replayInformation);
			}
			BasicDBObject query = new BasicDBObject("_id", auditID );
			BasicDBObject updateCriteria = new BasicDBObject("replayInfo",replayInformation);
			BasicDBObject setCritera = new BasicDBObject("$set", updateCriteria);
			auditCollection.update(query, setCritera);
			
	}


}