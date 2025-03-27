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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MeterRegistry meterRegistry;

    public GlobalExceptionHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Map<String, String>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
		Counter.builder("api.errors")
              .tag("exception", "MethodNotAllowed")
              .register(meterRegistry)
              .increment();
        
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, 
                                "Method Not Allowed", 
                                ex.getMessage());
	}

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<Map<String, String>> handleBadRequest(NotFoundException ex) {
		Counter.builder("api.errors")
              .tag("exception", "NotFound")
              .register(meterRegistry)
              .increment();
        
        return buildErrorResponse(HttpStatus.NOT_FOUND,
                                "Not Found",
                                ex.getMessage());
	}

	@ExceptionHandler(HealthCheckException.class)
	public ResponseEntity<Map<String, String>> handleHealthCheckException(HealthCheckException ex) {
		Counter.builder("api.errors")
              .tag("exception", "HealthCheckFailure")
              .register(meterRegistry)
              .increment();
        
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                                "Service Unavailable",
                                ex.getMessage());
	}
	
	@ExceptionHandler({ DataAccessException.class, SQLException.class, RuntimeException.class })
    public ResponseEntity<Void> handleDatabaseFailure(Exception ex) {
        String errorType = ex instanceof SQLException ? "DatabaseConnection" :
                          ex instanceof DataAccessException ? "DataAccess" : "Runtime";
        
        Counter.builder("api.errors")
              .tag("exception", errorType)
              .register(meterRegistry)
              .increment();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }
	
	// @ExceptionHandler(AmazonS3Exception.class)
    // public ResponseEntity<Map<String, String>> handleAmazonS3Exception(AmazonS3Exception ex) {
    //     String message = "S3 Error: " + ex.getMessage();
    //     if(ex.getExtendedRequestId() != null) {
    //         message += " (Extended Request ID: " + ex.getExtendedRequestId() + ")";
    //     }
    //     if(ex.getCloudFrontId() != null) {
    //         message += " (CloudFront ID: " + ex.getCloudFrontId() + ")";
    //     }
    //     return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "S3 Service Error", message);
    // }

    @ExceptionHandler({AmazonS3Exception.class, AmazonServiceException.class})
    public ResponseEntity<Map<String, String>> handleAmazonServiceException(AmazonServiceException ex) {
        Counter.builder("api.errors")
              .tag("exception", "AmazonService")
              .register(meterRegistry)
              .increment();
        
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                                "AWS Service Error",
                                ex.getErrorMessage());
    }

    @ExceptionHandler(SdkClientException.class)
    public ResponseEntity<Map<String, String>> handleSdkClientException(SdkClientException ex) {
        Counter.builder("api.errors")
              .tag("exception", "AWSClient")
              .register(meterRegistry)
              .increment();
        
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
