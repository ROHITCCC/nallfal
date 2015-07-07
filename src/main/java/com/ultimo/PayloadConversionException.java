package com.ultimo;

public class PayloadConversionException extends RuntimeException 
{
	private static final long serialVersionUID = 1L;
	String applicationMessage;
	
	public PayloadConversionException (String message)
	{
		super(message);
		this.applicationMessage = message;
		System.out.println("HELLO!");
	}
	public PayloadConversionException()
	{
		super();
	}
	
	public String getApplicationMessage() {
		return applicationMessage;
	}
	public void setApplicationMessage(String applicationMessage) {
		this.applicationMessage = applicationMessage;
	}
	
	
}
