package com.healthCheck.exception;

public class HealthCheckException extends RuntimeException{
	
	public HealthCheckException(String message) {
		super(message);
	}
	
	public HealthCheckException(String message, Throwable cause) {
		super(message, cause);
	} 

}
