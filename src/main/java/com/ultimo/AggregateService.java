package com.ultimo;

import java.util.Map;

import io.undertow.server.HttpServerExchange;

import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.utils.HttpStatus;
import org.restheart.utils.ResponseHelper;

public class AggregateService extends ApplicationLogicHandler {

	public AggregateService(PipedHttpHandler next, Map<String, Object> args) {
		super(next, args);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void handleRequest(HttpServerExchange exchange,
			RequestContext context) throws Exception {
		// TODO Auto-generated method stub
		 if (context.getMethod() == METHOD.GET) {
	            ResponseHelper.endExchangeWithMessage(exchange, HttpStatus.SC_OK, "Hello RestHeart");
	        } else {
	            exchange.setResponseCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
	            exchange.endExchange();
	        }
		

	}

}
