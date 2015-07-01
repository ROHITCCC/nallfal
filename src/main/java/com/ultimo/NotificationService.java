package com.ultimo;

import java.util.Map;

import io.undertow.server.HttpServerExchange;

import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;

public class NotificationService extends ApplicationLogicHandler {

	public NotificationService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,
			RequestContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
