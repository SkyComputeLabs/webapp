package com.healthCheck.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Map<String, String>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
		Map<String, String> response = new HashMap<>();
		response.put("status", "405");
		response.put("error", "Method Not Allowed");
		response.put("message", ex.getMessage());
		return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(NotFoundException ex) {
		Map<String, String> response = new HashMap<>();
		response.put("status", "400");
		response.put("error", "Bad Request");
		response.put("message", ex.getMessage());
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler({HealthCheckException.class, CannotCreateTransactionException.class})
	public ResponseEntity<Map<String, String>> handleHealthCheckException(Exception ex) {
		Map<String, String> response = new HashMap<>();
		response.put("status", "503");
		response.put("error", "Service Unavailable");
		response.put("message", "Database connection error.");
		return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
	}
}
