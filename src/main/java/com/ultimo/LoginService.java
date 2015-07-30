
package com.ultimo;

import com.mongodb.BasicDBObject;

import org.restheart.hal.Representation;
import org.restheart.handlers.PipedHttpHandler;
import org.restheart.handlers.RequestContext;
import org.restheart.handlers.RequestContext.METHOD;
import org.restheart.handlers.applicationlogic.ApplicationLogicHandler;
import org.restheart.utils.HttpStatus;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.util.Map;
import java.util.Set;

import static org.restheart.hal.Representation.HAL_JSON_MEDIA_TYPE;
import static org.restheart.security.handlers.IAuthToken.AUTH_TOKEN_HEADER;
import static org.restheart.security.handlers.IAuthToken.AUTH_TOKEN_LOCATION_HEADER;
import static org.restheart.security.handlers.IAuthToken.AUTH_TOKEN_VALID_HEADER;

import org.restheart.utils.URLUtils;

/**
 *
 * @author Andrea Di Cesare <andrea@softinstigate.com>
 */
public class LoginService extends ApplicationLogicHandler {
    /**
     * the key for the url property.
     */
    public static final String urlKey = "url";

    private String url;

    /**
     * Creates a new instance of GetRoleHandler
     *
     * @param next
     * @param args
     * @throws Exception
     */
    public LoginService(PipedHttpHandler next, Map<String, Object> args) throws Exception {
        super(next, args);

        if (args == null) {
            throw new IllegalArgumentException("args cannot be null");
        }

        this.url = (String) ((Map<String, Object>) args).get(urlKey);
    }

    /**
     * Handles the request.
     *
     * @param exchange
     * @param context
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange, RequestContext context) throws Exception {
        Representation rep;
        
        if (context.getMethod() == METHOD.OPTIONS) {
           ErrorSpotSinglton.optionsMethod(exchange);
        } else if (context.getMethod() == METHOD.GET) {
            if ((exchange.getSecurityContext() == null
                    || exchange.getSecurityContext().getAuthenticatedAccount() == null
                    || exchange.getSecurityContext().getAuthenticatedAccount().getPrincipal() == null)){
                    
                    //|| !(context.getUnmappedRequestUri().equals(URLUtils.removeTrailingSlashes(url) + "/" + exchange.getSecurityContext().getAuthenticatedAccount().getPrincipal().getName()))) {

                {
                    exchange.setResponseCode(HttpStatus.SC_FORBIDDEN);

                    // REMOVE THE AUTH TOKEN HEADERS!!!!!!!!!!!
                    exchange.getResponseHeaders().remove(AUTH_TOKEN_HEADER);
                    exchange.getResponseHeaders().remove(AUTH_TOKEN_VALID_HEADER);
                    exchange.getResponseHeaders().remove(AUTH_TOKEN_LOCATION_HEADER);

                    exchange.endExchange();
                    return;
                }

            } else {
                rep = new Representation(URLUtils.removeTrailingSlashes(url) + "/" + exchange.getSecurityContext().getAuthenticatedAccount().getPrincipal().getName());
                BasicDBObject root = new BasicDBObject();

                Set<String> _roles = exchange.getSecurityContext().getAuthenticatedAccount().getRoles();

                root.append("authenticated", true);
                root.append("roles", _roles);

                rep.addProperties(root);
            }

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, HAL_JSON_MEDIA_TYPE);
            exchange.getResponseSender().send(rep.toString());
            exchange.endExchange();
        } else {
            exchange.setResponseCode(HttpStatus.SC_METHOD_NOT_ALLOWED);
            exchange.endExchange();
        }
    }
}
