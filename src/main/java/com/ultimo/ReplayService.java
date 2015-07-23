package com.ultimo;
 
import static org.restheart.security.handlers.IAuthToken.AUTH_TOKEN_HEADER;
import static org.restheart.security.handlers.IAuthToken.AUTH_TOKEN_LOCATION_HEADER;
import static org.restheart.security.handlers.IAuthToken.AUTH_TOKEN_VALID_HEADER;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
 
















import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;






import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.ftp.FTPClient;
import org.reficio.ws.builder.SoapBuilder;
import org.reficio.ws.builder.SoapOperation;
import org.reficio.ws.builder.core.Wsdl;
import org.reficio.ws.client.core.SoapClient;
import org.restheart.hal.Representation;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.security.handlers.IAuthToken;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;
 













import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientException;
import com.mongodb.util.JSON;

/*
 * 	Watch  out for the $match and the other important changes that could happen with the new version of MongoDB when it is released.  
 */

public class ReplayService extends ApplicationLogicHandler implements IAuthToken{
	
	MongoClient db;

	public ReplayService(PipedHttpHandler next, Map<String, Object> args) {
        super(next, args);
 
  }
	
	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");
	

	public void handleRequest(HttpServerExchange exchange,RequestContext context) throws Exception {
        
        if (context.getMethod() == METHOD.OPTIONS) {
        	ErrorSpotSinglton.optionsMethod(exchange);
        }
        else if (context.getMethod() == METHOD.POST){   
        	
        	
        	InputStream input = exchange.getInputStream();
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));
            //gets payload
            String line = null;
            String payload = "";
            while((line = inputReader.readLine())!=null){
            	payload += line;
            }
            String[] inputString = new String[5];
            //separates payload into variables split on ~,
            inputString = payload.split("~,");
            
            for(int x = 0; x < inputString.length;x++){
            	inputString[x] = inputString[x].substring(inputString[x].indexOf("=") + 1);
            }
    		
            String[] codeAndMessage = null;
            
	    		switch(inputString[0].toUpperCase()){
	    		
	    		case "REST":codeAndMessage = handleREST(inputString);break;
	    		case "FILE":codeAndMessage = handleFILE(inputString);break;
	    		case "WS":codeAndMessage = handleWS(inputString);break;
	    		case "FTP":codeAndMessage = handleFTP(inputString);break;
	    		
	    		default:break;
	    		
	    		}
	    		
	    	//value of -1 is safe since it is only returned from getResponseCode() if response is not valid http
		   	int code = -1;
		   	String message = null;
		   	String response = null;
		    		
		    if (codeAndMessage[0] != null)
		    {
		    	code = Integer.parseInt(codeAndMessage[0]);
		    }
		    
		    if (codeAndMessage[1] != null)
		    {
		    	message = codeAndMessage[1];
		    }
		    
		    if (codeAndMessage[2] != null)
		    {
		    	response = codeAndMessage[2];
		    }
		    
		    if ((code != -1)&&(message != null))
		    {
		    	ResponseHelper.endExchangeWithMessage(exchange, code, message);
		    }
		    
		    if ((codeAndMessage.length == 4)&&(codeAndMessage[3].equalsIgnoreCase("true")))
		    {
		    	exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, Representation.HAL_JSON_MEDIA_TYPE);
		    }
		    
		    if (response != null)
		    {
		    	exchange.getResponseSender().send(response);
		    }
		    	
    	}

     }
	
		public static String[] handleREST(String[] inputString) throws Exception{
			//Uses Java Rest API
			LOGGER.info("Starting REST Service");
			
			String[] returnArray = null;

			String restEnd = inputString[1].replaceAll("\\s","");
			String restMethod = inputString[2].replaceAll("\\s","");
			String restContentType = inputString[3].replaceAll("\\s","");
			String restPayload = inputString[4];		
			String[] restHead = new String[5];
			String result = "";
			String errorReason = "";
			
			Boolean method = false;
			Boolean content = false;
			Boolean header = false;
			Boolean endpoint = false;
			
			
			restHead = inputString[5].split(",");
			for(int x = 0; x < restHead.length; x++){
				restHead[x] = restHead[x].split("=")[1];
				restHead[x] = restHead[x].replaceAll("\"","").replace("[", "").replace("]", "");
			}
			
			
			
        	/////
        	/////Method Type Checking
        	/////
			
        	switch(restMethod){
        		case "GET":method = true; break;
        		case "POST":method = true; break;
        		case "PUT":method = true; break;
        		case "PATCH":method = true; break;
        		case "DELETE":method = true; break;
        		case "HEAD":method = true; break;
        		case "OPTIONS":method = true; break;
        		
        		default:method=false;break;
        	};
        	if(method){
        		LOGGER.trace("method = "+restMethod); 
        	}
        	else{
        		errorReason += "Method ";
        		LOGGER.error("Invalid Method"); 
        	}
        	
        	
        	///////
        	///////Content Type Checking, only checking if field was written in
        	///////
        	
    		if(restContentType.length()>0){
    			LOGGER.trace("content = "+restContentType);
    			content=true;
    		}
    		else{
    			errorReason += "Content Type ";
    			LOGGER.error("Invalid Content type");
    		}
    		
    		
    		///////
    		///////Payload Checking
    		///////
    		
    		boolean payloadCheck = payloadChecker(restPayload);  //checks for equal numbers of [] and{}
    		
			if(payloadCheck)LOGGER.trace("Payload format correct");
			else {
				errorReason += "Payload ";
				LOGGER.error("Payload not formatted correctly");
				LOGGER.error("payload = " + restPayload);  
			}
			
        	
        	//////
			//////Endpoint Checking 
        	//////
        	
			
        	if(restEnd.substring(0, 7).equals("http://")){
        		endpoint = true;
        		LOGGER.trace("Endpoint ok");
        	}
        	else{
        		errorReason += "Endpoint ";
        		LOGGER.error("Invalid Endpoint Structure");
        	}
        	
        	
        	///////
        	///////Header Check
        	//////
        	
        	for(int x = 0; x < restHead.length; x += 2){
	        	switch(restHead[x]){
	        		case "Accept": header=true;break;
	        		case "Accept-Charset": header=true;break;
	        		case "Accept-Encoding": header=true;break;
	        		case "Accept-Language": header=true;break;
	        		case "Authorization": header=true;break;
	        		case "Cookie": header=true;break;
	        		case "Content-Length": header=true;break;
	        		case "Content-MD5": header=true;break;
	        		case "Date": header=true;break;
	        		case "WWW-Authenticate": header=true;break;
	        		default: header=false;break;
	        	}
	        	if(header)LOGGER.trace("Header ok");
	        	else{
	        		errorReason += "Header ";
	        		LOGGER.error("Invalid Header Type: " + restHead[x]);
	        	}
        	}
        	
			//REST Call
        	
        	
        		
        	URL url = new URL(restEnd);
        	String line;

            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            
            //must pass all validations in order to execute REST call 
            
            if(header && method && payloadCheck && content && endpoint){
            	
	            httpCon.setDoInput(true);
	            httpCon.setDoOutput(true);
	            httpCon.setRequestMethod(restMethod);
	            for(int x = 0; x < restHead.length; x += 2){
	            	httpCon.setRequestProperty(restHead[x], restHead[x+1]);
	            }
	            httpCon.setRequestProperty("Content-Type", restContentType);
	            OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream(), "UTF-8");
	            out.write(restPayload);
	            LOGGER.info("Payload has been written");
	            
	            out.close();
	            
	            if (httpCon.getResponseCode() == HttpURLConnection.HTTP_OK) {
				     
	                LOGGER.trace(httpCon.getResponseCode() + ": " + httpCon.getResponseMessage());
	                LOGGER.info("REST Successful");
	 
	            } 
	            else {
	            	LOGGER.error(httpCon.getResponseCode() + ": " + httpCon.getResponseMessage());
					returnArray = new String[4];
	            	returnArray[0] = Integer.toString(httpCon.getResponseCode());
	            	returnArray[1] = httpCon.getResponseMessage();
	            }
	            
	
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
				while((line = bufferedReader.readLine()) != null){
	                result += line;
	            }
	             
	            bufferedReader.close();
	
				if (returnArray == null)
				{
					returnArray = new String[4];
				}
				returnArray[3] = "true";
	            returnArray[2] = result;
        	}
        	else{
        		LOGGER.error("Error on " + errorReason);
				returnArray = new String[3];
        		returnArray[0] = Integer.toString(409);
            	returnArray[1] = "Error with: " + errorReason;
        	}
		
            return returnArray;
            
        }
		
        
		public static String[] handleFILE(String[] inputString) throws Exception{
			//Host must manually be mounted before file handling can take place
			LOGGER.info("Starting FILE Service");
			
			String[] returnArray = null;
			
			String fileLocation = inputString[1];
			String filePayload = inputString[2];
			Boolean payloadCheck = payloadChecker(filePayload);
			
			LOGGER.trace("Payload = "+filePayload);
			LOGGER.trace("Location = " + fileLocation);

			File file = new File(fileLocation);
			
			if(payloadCheck){
				
				LOGGER.info("Payload format correct");
			
				if (file.exists()) {
					LOGGER.error("Duplicate file name found at: " + fileLocation);
					returnArray = new String[3];
					returnArray[0] = Integer.toString(409);
	            	returnArray[1] = "Duplicate File Found at: " + fileLocation;
				}
				else{
					file.createNewFile();
					
					FileWriter fWriter = new FileWriter(file.getAbsoluteFile(), false); //set to true to allow file appending
					BufferedWriter bWriter = new BufferedWriter(fWriter);
					bWriter.write(filePayload);											//enters payload into file
					LOGGER.info("Payload has been written");
					bWriter.close();
					fWriter.close();
					LOGGER.info("Creating new file at: " + fileLocation);
					returnArray = new String[3];
					returnArray[2] = "File Successfully Created";
				}
			}
			else {
				LOGGER.error("Payload not formatted correctly");
				returnArray = new String[3];
				returnArray[0] = Integer.toString(406);
            	returnArray[1] = "Payload not formatted correctly";
			}
			
			return returnArray;
			
		}
		
		
		
		public static String[] handleWS(String[] inputString) throws Exception{
			
			//Uses soap-ws from github
			LOGGER.info("Starting Web Service");
			
			String[] returnArray = null;
			
			try{
			String wsdlInput = inputString[1];
			String wsOperation = inputString[2];
			String wsSoapAction = inputString[3];
			String wsBinding = inputString[4];
			String wsPayload = inputString[5];
			String xmlns = "";
			String wsBody = "";
			Wsdl wsdl = null;
			
			//checks if wsdl is raw text or in a file online
			if(wsdlInput.substring(0, 7).equals("http://")){
		    	wsdl = Wsdl.parse(wsdlInput);
		    }
			else{

				String path = System.currentTimeMillis() + ".wsdl";
				if(new File(path).exists()){
					for(int x = 0; x < 10; x++){
						if(new File(path).exists()){
							LOGGER.error("duplicate file found: " + path);
							path = (System.currentTimeMillis() - x) + ".wsdl";
						}
					}
				}
				//makes new wsdl file to read
				File file = new File(path);
				file.createNewFile();
				
				FileWriter fWriter = new FileWriter(file.getAbsoluteFile(), false); 
				BufferedWriter bWriter = new BufferedWriter(fWriter);
				bWriter.write(wsdlInput);
				LOGGER.info("Input has been written");
				bWriter.close();
				fWriter.close();
				
		        URL url1 = (new java.io.File(path)).toURI().toURL();

				wsdl = Wsdl.parse(url1);
				
				file.delete();
			}
		    
		    
			String[] getEndpoint = new String[wsdl.binding().localPart(wsBinding).find().getServiceUrls().size()];
			String[] getPayload = new String[StringUtils.countOccurrencesOf(wsPayload, ">")];
			getPayload = wsPayload.split("(?=[>])");
			
		    
			for(int x = 0; x < getEndpoint.length; x++){
				getEndpoint[x] = wsdl.binding().localPart(wsBinding).find().getServiceUrls().get(x);
			}

		    SoapBuilder builder = wsdl.binding()
		        .localPart(wsBinding)
		        .find();
		    
		    //checks if optional soap action field is filled in or not
		    if(wsSoapAction.length()<1){
		    	String soapActionTemp = builder.operation().name(wsOperation).find().toString();
		    	int eqCount = StringUtils.countOccurrencesOf(soapActionTemp, "=");
		    	String[] soapActionArr = new String[eqCount];
		    	soapActionArr = soapActionTemp.split("]");

		    	for(int x = 0; x < eqCount; x++){
		    		if(soapActionArr[x].trim().substring(0, 10).equals("soapAction")){
		    			wsSoapAction = soapActionArr[x].trim().substring(12);
		    		}
		    	}
		    }
		    
		    SoapOperation operation = builder.operation()
		        .soapAction(wsSoapAction)
		        .find();

		    
		    xmlns = getPayload[1].substring(getPayload[1].indexOf("xmlns"));
		    
		    if(getPayload[1].contains("schema")){
		    	for(int x = 2; x < (getPayload.length-2);x++){
		    		wsBody += getPayload[x];
		    	}
		    }
		    else{
		    	for(int x = 1; x < (getPayload.length-1);x++){
		    		wsBody += getPayload[x];
		    	}
		    }
		    
		    char firstL = wsBody.charAt(0);
		    wsBody = wsBody.substring(1) + firstL;
		    
		    String request = builder.buildInputMessage(operation);
		    
		    String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "+ xmlns + ">" +
							   "<soapenv:Header/>" +
							  " <soapenv:Body> " +
							      wsBody +
							   "</soapenv:Body>"+
							"</soapenv:Envelope>";
		    
		    //LOGGER.trace(request);
		    LOGGER.trace(payload);
		    SoapClient client = SoapClient.builder().
		    endpointUri(getEndpoint[0])
		        .build();
		    String response = client.post(wsSoapAction, payload);
		    
		    LOGGER.trace(response);
		    
		    LOGGER.info("Web Service Complete");
            returnArray = new String[3];
            returnArray[2] = response;
			}
			catch(Exception e){
				e.printStackTrace();
				returnArray = new String[3];
				returnArray[0] = Integer.toString(406);
            	returnArray[1] = "";
			}
			
			return returnArray;
			
		}
		
		public static String[] handleFTP(String[] inputString) throws Exception{
			LOGGER.info("Starting FTP service");
			LOGGER.trace(""+inputString.length);
			FTPClient ftp= new FTPClient();
			
			String[] returnArray = null;
			
			// break input string to its contents
			String hostname= inputString[1];
			String username= inputString[2];
			String password= inputString[3];
			String location= inputString[4];
			String file="";
			String fileType="";
			String ftpPayload="";
			
			//filename parameter passed
			if(inputString.length==9){
				LOGGER.info("file passed");
				file= inputString[5];
				fileType=inputString[6];
				ftpPayload=inputString[7];
			}
			//filename parameter not passed in
			else{
				LOGGER.info("no file parameter passed");
				fileType=inputString[5];
				ftpPayload=inputString[6];
			}
			
			//if file is not passed in or is blank
			if(file.equals("")){
				LOGGER.info("no file detected, so creating default with timestamp");
				Calendar calender = Calendar.getInstance();
				Date date=calender.getTime();
				Timestamp ts= new Timestamp(date.getTime());
				String st= ts.toString();
				st=st.replaceAll(":", "_");
				st=st.replaceAll("-", "_");
				st=st.replace('.', '_');
				st=st.replaceAll(" ", "_");
				file=st;
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
				returnArray = new String[3];
				returnArray[0] = Integer.toString(500);
            	returnArray[1] = "File name contains incorrect character: only aphanumaeric and underscore allowed";
				return returnArray;
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
				returnArray = new String[3];
				returnArray[0] = Integer.toString(500);
            	returnArray[1] = "Host is invalid";
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
					returnArray = new String[3];
					returnArray[0] = Integer.toString(500);
	            	returnArray[1] = "Username or password is incorrect";
				}
			}
			
			//add payload to file on the server
			if(loggedIn){
				boolean payloadCheck= payloadChecker(ftpPayload);
				if(payloadCheck){
					LOGGER.info("Payload format correct");
					//convert payload string to inputStream
					InputStream ftpPayloadInput = new ByteArrayInputStream(ftpPayload.getBytes(Charset.forName("UTF-8")));
					try{
						LOGGER.info("storing payload into file");
						LOGGER.warn("if file already exists in directory, new information will overwrite the exisiting");
						boolean stored = ftp.storeFile(filename,ftpPayloadInput);
						if(stored){
							LOGGER.info("successfully stored payload into file");
							returnArray = new String[3];
							returnArray[2] = "Payload successfully stored on file on server";
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
								returnArray = new String[3];
								returnArray[2] = "Payload successfully store on file on server";
							}
							else{
								LOGGER.error("storing file failed: incorrect filepath");
								returnArray = new String[3];
								returnArray[0] = Integer.toString(500);
				            	returnArray[1] = "Incorrect file path";
							}
						}
					}
					catch (IOException e){
						LOGGER.error("login failed: incorrect filepath");
						returnArray = new String[3];
						returnArray[0] = Integer.toString(500);
		            	returnArray[1] = "Incorrect file path";
					}
				}
				else{
					LOGGER.error("Payload not formatted correctly");
					returnArray = new String[3];
					returnArray[0] = Integer.toString(500);
	            	returnArray[1] = "Payload not formatted correctly";
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
			
			return returnArray;
			
		}
		
		public static boolean isValidIP(String ipAddr){
	        
	        Pattern ptn = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
	        Matcher mtch = ptn.matcher(ipAddr);
	        return mtch.find();
	    }
		
		public static boolean payloadChecker(String payload){
			
			int leftBrace = 0; 
			int rightBrace = 0;
			int leftBracket = 0;
			int rightBracket = 0;

				
			for(int x = 0; x < payload.length(); x++){
				if(payload.charAt(x) == '{'){
					leftBrace++;
				}
				if(payload.charAt(x) == '}'){
					rightBrace++;
				}
				if(payload.charAt(x) == '['){
					leftBracket++;
				}
				if(payload.charAt(x) == ']'){
					rightBracket++;
				}
			}
			if(leftBrace == rightBrace && leftBracket == rightBracket && payload.length() > 0)return true;
			else return false;
		}
	}













