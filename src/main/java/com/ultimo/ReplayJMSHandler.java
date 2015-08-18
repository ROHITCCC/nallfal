package com.ultimo;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.Vector;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.DeliveryMode;

import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class ReplayJMSHandler {
	
   private static final Logger LOGGER = LoggerFactory.getLogger("com.ultimo");	



//	public static void main(String[] args){
//		
//
//			handleTibcoJMS("errorspot.dev.queue", "queue", "com.tibco.tibjms.TibjmsQueueConnectionFactory", "172.16.120.157", 7222, "admin", "",  "non_persistent", "test payload");
//			handleTibcoJMS("errorspot.dev.topic", "topic", "com.tibco.tibjms.TibjmsTopicConnectionFactory", "172.16.120.157", 7222, "admin", "",  "non_persistent", "test payload");
//			hanldeWeblogicJMS("jms/queue1", "queue", "jms/connectionFactory1", "localhost", 7001, "weblogic", "welcome1", "weblogic.jndi.WLInitialContextFactory", "Weblogic payload");
//			hanldeWeblogicJMS("jms/topic1", "topic", "jms/connectionFactory1", "localhost", 7001, "weblogic", "welcome1", "weblogic.jndi.WLInitialContextFactory", "Weblogic payload");
//	}

	

public static String handleJMS(JSONObject connectionDetails, String payload){
	String result ="";
	
	String destinationName = connectionDetails.getString("destinationName");
	String destinationType = connectionDetails.getString("destinationType");
	String connectionFactory  = connectionDetails.getString("connectionFactory");
    String host = connectionDetails.getString("host");
    int port = Integer.parseInt(connectionDetails.getString("port"));
    String username = connectionDetails.getString("username");
    String password = connectionDetails.getString("password");
    String deliveryMode = connectionDetails.getString("deliveryMode");
	
	if(connectionDetails.getString("jmsServerType").equalsIgnoreCase("tibco")){
	
		result = handleTibcoJMS(destinationName, destinationType, connectionFactory, host, port, username, password,  deliveryMode, payload);

	}else if (connectionDetails.getString("jmsServerType").equalsIgnoreCase("weblogic")){

		String initalContextFactory = connectionDetails.getString("initalContextFactory");
		
				  
		result = hanldeWeblogicJMS(destinationName, destinationType, connectionFactory, host, port, username, password, deliveryMode, initalContextFactory, payload);

	}else{
		result = "JMS server type: " + connectionDetails.getString("jmsServerType") + " not supported"; 
	}
		
	
	return result;
}
	
	
public static String handleTibcoJMS(String destinationName, String destinationType, 
		String connectionFactory,  String host, int port, String username, String password,
        String deliveryMode, String payload){
		
	
        String output = "";	
		String serverUrl = host + ":" +port;
		/*String username = "admin";
		String password = "";*/
 
		
		try {
			 
			Vector<Object> data = new Vector<Object>();
			data.add(payload);
 
			LOGGER.info("Connecting to Tibco EMS server: " + serverUrl+" with destination type: "+destinationType);
			LOGGER.trace("destination Name: "+destinationName+" destinationType: "+destinationType+" Tibco JMS Connection Factory: "+connectionFactory+" host: "+host+" port: "+port+" username: "+username+" password: "+password+" delivery mode: "+deliveryMode+" paylload: "+payload);
			

            Class<?> tibJmsConnFactory = Class.forName(connectionFactory);
 
			if (destinationType.equalsIgnoreCase("queue")){
				
				
				
				QueueConnectionFactory factory = (QueueConnectionFactory)tibJmsConnFactory.getDeclaredConstructor(String.class).newInstance(serverUrl);

				QueueConnection connection = factory.createQueueConnection(username, password);
				QueueSession session = connection.createQueueSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
	 
				Class<?> tibQueue = Class.forName("com.tibco.tibjms.TibjmsQueue");
				Destination queue = (Destination)tibQueue.getDeclaredConstructor(String.class).newInstance(destinationName);
				MessageProducer producer = session.createProducer(queue);
				
				
				if(deliveryMode.equalsIgnoreCase("persistent")){
					producer.setDeliveryMode(DeliveryMode.PERSISTENT);
				} else{
					producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
				}
				
				
				/* publish messages */
				for (int i = 0; i < data.size(); i++) {
					TextMessage jmsMessage = session.createTextMessage();
					String text = (String) data.elementAt(i);
					jmsMessage.setText(text);
					producer.send(jmsMessage);
					LOGGER.info("JMS Message sent to queue: " + destinationName);
				}
	 
				connection.close();
				LOGGER.info("closing connection to server: "+serverUrl);
				output = "Success";
				
			} else if (destinationType.equalsIgnoreCase("topic")){
				
				TopicConnectionFactory factory = (TopicConnectionFactory)tibJmsConnFactory.getDeclaredConstructor(String.class).newInstance(serverUrl);
				TopicConnection connection = factory.createTopicConnection(username, password);
				TopicSession session = connection.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
	 
				
				Class<?> tibTopic = Class.forName("com.tibco.tibjms.TibjmsTopic");
				Topic topic = (Topic)tibTopic.getDeclaredConstructor(String.class).newInstance(destinationName);

				TopicPublisher publisher = session.createPublisher(topic);
				
				
				if(deliveryMode.equalsIgnoreCase("persistent")){
					publisher.setDeliveryMode(DeliveryMode.PERSISTENT);
				} else{
					publisher.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
				}
				
				/* publish messages */
				for (int i = 0; i < data.size(); i++) {
					TextMessage jmsMessage = session.createTextMessage();
					String text = (String) data.elementAt(i);
					jmsMessage.setText(text);
					publisher.publish(jmsMessage);
					
					LOGGER.info("JMS Message sent to topic: " + destinationName);

				}
	 
				connection.close();
				LOGGER.info("closing connection to server: "+serverUrl);
				output = "Success";
				
			} else {
				
				LOGGER.error("Destination is not a queue or topic, please specifiy with correct spelling.");
				output =  "Destination is not a queue or topic, please specifiy with correct spelling.";
				
			}
 
	} catch (JMSException e) {
			LOGGER.error("",e);
		    output = e.getMessage();
		    
		}
		catch (ClassNotFoundException e){
			LOGGER.error("",e);
			output = e.getMessage();
		} catch (InstantiationException e) {
			LOGGER.error("",e);
			output = e.getMessage();
		} catch (IllegalAccessException e) {
			LOGGER.error("",e);
			output = e.getMessage();
		} catch (IllegalArgumentException e) {
			LOGGER.error("",e);
			output = e.getMessage();
		} catch (InvocationTargetException e) {
			LOGGER.error("",e);
			output = e.getMessage();
		} catch (NoSuchMethodException e) {
			LOGGER.error("",e);
			output = e.getMessage();
		} catch (SecurityException e) {
			LOGGER.error("",e);
			output = e.getMessage();
		}

		return output;
	}


public static String hanldeWeblogicJMS(String destinationName, String destinationType, String connectionFactory, 
		String host, int port, String username, String password, String deliveryMode, String initalContextFactory, String payload) {
	
	String output = "";
	try {
	//final String JNDI_FACTORY="weblogic.jndi.WLInitialContextFactory";
	final String JNDI_FACTORY = initalContextFactory;
	final String JMS_FACTORY= connectionFactory;
	QueueConnectionFactory qconFactory;
	QueueConnection qcon;
	QueueSession qsession;
	QueueSender qsender;
	Queue queue;
	TopicConnectionFactory tconFactory;
	TopicConnection tcon;
	TopicSession tsession;
	TopicPublisher tsender;
	Topic topic;
	TextMessage msg;
	String url = "t3://" + host + ":" + Integer.toString(port);
	LOGGER.info("Connecting to Weblogic JMS server url: " + url+" with destination type: "+destinationType);
	LOGGER.trace("destination Name: "+destinationName+" destinationType: "+destinationType+" Tibco JMS Connection Factory: "+connectionFactory+" host: "+host+" port: "+port+" username: "+username+" password: "+password+" delivery mode: "+deliveryMode+" paylload: "+payload);
	
	Hashtable<String,String> env = new Hashtable<String,String>();
	env.put(Context.INITIAL_CONTEXT_FACTORY, JNDI_FACTORY);
	env.put(Context.PROVIDER_URL, url);
	InitialContext ic = new InitialContext(env);
	String message = "";
	
	if (destinationType.equalsIgnoreCase("queue")){
		
		LOGGER.debug("Using connection factory: " + JMS_FACTORY);
		qconFactory = (QueueConnectionFactory) ic.lookup(JMS_FACTORY);
		qcon = qconFactory.createQueueConnection();
		qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		queue = (Queue) ic.lookup(destinationName);
		qsender = qsession.createSender(queue);
		msg = qsession.createTextMessage();
		qcon.start();
		

		msg.setText(message);
		qsender.send(msg);
		LOGGER.info("JMS Message sent to queue: " + destinationName);

		qsender.close();
		qsession.close();
		qcon.close();
		
		output = "Success";
		
	} else if (destinationType.equalsIgnoreCase("topic")){
		
		LOGGER.debug("Using connection factory: " + JMS_FACTORY);
		tconFactory = (TopicConnectionFactory) ic.lookup(JMS_FACTORY);
		tcon = tconFactory.createTopicConnection();
		tsession = tcon.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		topic = (Topic) ic.lookup(destinationName);
		tsender = (TopicPublisher) tsession.createProducer(topic);
		msg = tsession.createTextMessage();
		tcon.start();
		

		msg.setText(message);
		tsender.send(msg);
		LOGGER.info("JMS Message sent to topic: " + destinationName);
		
		tsender.close();
		tsession.close();
		tcon.close();
		output = "Success";
	} else {
		
		LOGGER.error("Destination is not a queue or topic, please specifiy with correct spelling.");
		output =  "Destination is not a queue or topic, please specifiy with correct spelling.";
		
	}
	
	LOGGER.info("closing connection to server: "+url);
	}catch (JMSException e){
		LOGGER.error("",e);
		output = e.getMessage();
		
	} catch (NamingException e) {
		LOGGER.error("",e);
		output = e.getMessage();
	}
	
	return output;
	
}
}
