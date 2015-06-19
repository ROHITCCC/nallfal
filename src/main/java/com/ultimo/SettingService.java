package com.ultimo;

import java.util.Map;

import io.undertow.server.HttpServerExchange;

import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;

public class SettingService extends ApplicationLogicHandler{

	public SettingService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
		
		 if (args == null) {
	            throw new IllegalArgumentException("args cannot be null");
	        }
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,
			RequestContext context) throws Exception {
		    // TODO Auto-generated method stub
		
	}

}
