package com.ultimo;

import static org.restheart.hal.Representation.HAL_JSON_MEDIA_TYPE;
import static org.restheart.security.handlers.IAuthToken.AUTH_TOKEN_HEADER;
import static org.restheart.security.handlers.IAuthToken.AUTH_TOKEN_LOCATION_HEADER;
import static org.restheart.security.handlers.IAuthToken.AUTH_TOKEN_VALID_HEADER;

import java.util.Map;
import java.util.Set;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import org.restheart.hal.Representation;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;

public class LoginService extends ApplicationLogicHandler {
	
	private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");
	
	 public static final String urlKey = "url";
	 
	 private String url;

	    /**
	     * Creates a new instance of GetRoleHandler
	     *
	     * @param next
	     * @param args
	     * @throws Exception
	     */


	public LoginService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
		

        if (args == null) {
            throw new IllegalArgumentException("args cannot be null");
        }

        this.url = (String) ((Map<String, Object>) args).get(urlKey);
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,
			RequestContext context) throws Exception {
		
		Representation rep;
		
		 if (context.getMethod() == METHOD.OPTIONS) {
	            exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Methods"), "GET");
	            exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Headers"), "Accept, Accept-Encoding, Authorization, Content-Length, Content-Type, Host, Origin, X-Requested-With, User-Agent, No-Auth-Challenge, " + AUTH_TOKEN_HEADER + ", " + AUTH_TOKEN_VALID_HEADER + ", " + AUTH_TOKEN_LOCATION_HEADER);
	            exchange.setResponseCode(HttpStatus.SC_OK);
	            exchange.endExchange();
	        } else if (context.getMethod() == METHOD.GET) {
	 
	        	
	        	
	            if ((exchange.getSecurityContext() == null
	                    || exchange.getSecurityContext().getAuthenticatedAccount() == null
	                    || exchange.getSecurityContext().getAuthenticatedAccount().getPrincipal() == null)) {
	                    //|| !(context.getUnmappedRequestUri().equals(URLUtils.removeTrailingSlashes(url) + "/" + exchange.getSecurityContext().getAuthenticatedAccount().getPrincipal().getName()))) {

	                {
	                	
	                    exchange.setResponseCode(HttpStatus.SC_FORBIDDEN);

	                    // REMOVE THE AUTH TOKEN HEADERS!!!!!!!!!!!
	                    exchange.getResponseHeaders().remove(AUTH_TOKEN_HEADER);
	                    exchange.getResponseHeaders().remove(AUTH_TOKEN_VALID_HEADER);
	                    exchange.getResponseHeaders().remove(AUTH_TOKEN_LOCATION_HEADER);
	                    exchange.endExchange();
	                    
	                    LOGGER.error("");
	                	LOGGER.debug("");
	                	LOGGER.trace("");
	                    return;
	                }

	            } else {
	            	
	                rep = new Representation(URLUtils.removeTrailingSlashes(url) + "/" + exchange.getSecurityContext().getAuthenticatedAccount().getPrincipal().getName());
	               
	                BasicDBObject root = new BasicDBObject();
	                root.append("authenticated", true);
	                rep.addProperties(root);
	                
	                LOGGER.info("");
                	LOGGER.debug("");
                	LOGGER.trace("");
	                
	            }

	            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, HAL_JSON_MEDIA_TYPE);
	            exchange.getResponseSender().send(rep.toString());
	            exchange.endExchange();
	            
	            
	        	
	        	
	        } else {
	        	
                LOGGER.error("Http " + context.getMethod() + " is not Allowed. Use HTTP GET Method");
            	LOGGER.debug("");
            	LOGGER.trace("");
            	
	            exchange.setResponseCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
	            exchange.endExchange();
	        }
		
	        

	}

}
