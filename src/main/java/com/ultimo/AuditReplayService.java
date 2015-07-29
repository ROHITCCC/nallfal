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
import java.util.Map;
import org.apache.commons.fileupload.MultipartStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.security.handlers.IAuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AuditReplayService extends ApplicationLogicHandler implements IAuthToken {

	public AuditReplayService(PipedHttpHandler next, Map<String, Object> args) 
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
        String result = "";
		if (request.getString("type").equalsIgnoreCase("REST"))
		{
			result = handleRest(request, payload);
		}
		else if (request.getString("type").equalsIgnoreCase("file"))
		{
			result = handleFile(request, payload);
		}
		
		System.out.println(result);
		
	}
	
	public String handleRest(JSONObject connectionDetails , String payload)
	{
		try 
		{
			String endpoint = connectionDetails.getString("endpoint");
			String restMethod = connectionDetails.getString("method");
			String contentType = connectionDetails.getString("content-type");
			JSONArray headers = connectionDetails.getJSONArray("restHeaders");
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
	          
	        for (int iterator = 0; iterator < headers.length();iterator++)
	        {
	         	JSONObject s = headers.getJSONObject(iterator);
	          	httpCon.setRequestProperty(s.getString("type"), s.getString("value"));
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
				LOGGER.error(e.getMessage().split("\\[")[1].split("\\]")[0] + "was not found in the request. Please enter a valid value and resubmit the request");
				return e.getMessage().split("\\[")[1].split("\\]")[0] + "was not found in the request. Please enter a valid value and resubmit the request"; 
			}
			e.printStackTrace();
			
			return e.getMessage();
		}



	

		
		return "Successfully Completed Replay";
		
	}

	public String handleFile(JSONObject connectionDetails, String payload)
	{
		String fileLocation = connectionDetails.getString("fileLocation");
		File file = new File(fileLocation);
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
				return "Replay Successfull";
		}
		catch (IOException e) 
		{
			 LOGGER.error(e.getMessage());
				e.printStackTrace();
				return e.getMessage();
		}
	}
}