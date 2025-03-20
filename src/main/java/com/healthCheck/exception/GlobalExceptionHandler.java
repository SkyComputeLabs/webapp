package com.healthCheck.exception;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;

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

	@ExceptionHandler(HealthCheckException.class)
	public ResponseEntity<Map<String, String>> handleHealthCheckException(HealthCheckException ex) {
		Map<String, String> response = new HashMap<>();
		response.put("status", "503");
		response.put("error", "Service Unavailable");
		response.put("message", ex.getMessage());
		return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
	}
	
	@ExceptionHandler({ DataAccessException.class, SQLException.class, RuntimeException.class })
    public ResponseEntity<Void> handleDatabaseFailure(Exception ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
	
	@ExceptionHandler(AmazonS3Exception.class)
    public ResponseEntity<Map<String, String>> handleAmazonS3Exception(AmazonS3Exception ex) {
        String message = "S3 Error: " + ex.getMessage();
        if(ex.getExtendedRequestId() != null) {
            message += " (Extended Request ID: " + ex.getExtendedRequestId() + ")";
        }
        if(ex.getCloudFrontId() != null) {
            message += " (CloudFront ID: " + ex.getCloudFrontId() + ")";
        }
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "S3 Service Error", message);
    }

    @ExceptionHandler(AmazonS3Exception.class)
    public ResponseEntity<Map<String, String>> handleAmazonServiceException(AmazonServiceException ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                                "AWS Service Error", 
                                ex.getErrorMessage());
    }

    @ExceptionHandler(SdkClientException.class)
    public ResponseEntity<Map<String, String>> handleSdkClientException(SdkClientException ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                                "AWS Client Error",
                                "Failed to communicate with AWS services: " + ex.getMessage());
    }

    // Utility method for consistent error responses
    private ResponseEntity<Map<String, String>> buildErrorResponse(
            HttpStatus status, String error, String message) {
        
        Map<String, String> response = new HashMap<>();
        response.put("status", String.valueOf(status.value()));
        response.put("error", error);
        response.put("message", message);
        return new ResponseEntity<>(response, status);
    }
}
