package com.healthCheck.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthCheck.exception.HealthCheckException;
import com.healthCheck.exception.NotFoundException;
import com.healthCheck.service.HealthCheckService;

@RestController
@RequestMapping("/healthz")
public class HealthCheckController {

	HttpHeaders headers = new HttpHeaders();

	@Autowired
	private final HealthCheckService healthCheckService;

	public HealthCheckController(HealthCheckService healthCheckService) {
		this.healthCheckService = healthCheckService;
	}

	@GetMapping
	public ResponseEntity<String> checkHealth(@RequestBody(required = false) String body,
			@RequestParam Map<String, String> queryParams) {

		headers.setCacheControl("no-cache, no-store, must-revalidate");
		headers.setPragma("no-cache");
	    headers.set("X-Content-Type-Options", "nosniff");
	    
		if (body != null && !body.isEmpty()) {
			return ResponseEntity.badRequest().headers(headers).build();
		}
		
		if (!queryParams.isEmpty()) {
			return ResponseEntity.badRequest().headers(headers).build();
		}

		try {
			healthCheckService.recordHealthCheck();
			return ResponseEntity.ok().headers(headers).build();
		} catch (HealthCheckException e) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).headers(headers).build();
		} catch (NotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).build();
		}

	}

	@RequestMapping(method = { RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH,
			RequestMethod.HEAD, RequestMethod.OPTIONS })
	public ResponseEntity<String> methodNotAllowed() {

		headers.setCacheControl("no-cache, no-store, must-revalidate");
		headers.setPragma("no-cache");
	    headers.set("X-Content-Type-Options", "nosniff");
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers).build();
	}

	@RequestMapping(value = "/**", method = { RequestMethod.GET })
	public ResponseEntity<String> handleInvalidEndpoint() {
		headers.setCacheControl("no-cache, no-store, must-revalidate");
		headers.setPragma("no-cache");
	    headers.set("X-Content-Type-Options", "nosniff");
	    
		return ResponseEntity.badRequest().headers(headers).build();
	}

}
