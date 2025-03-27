package com.healthCheck.controller;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthCheck.exception.HealthCheckException;
import com.healthCheck.exception.NotFoundException;
import com.healthCheck.service.HealthCheckService;

import io.micrometer.core.instrument.MeterRegistry;

@RestController
@RequestMapping("/healthz")
public class HealthCheckController {

	private final MeterRegistry meterRegistry;
    private final HealthCheckService healthCheckService;
	private final HttpHeaders headers = new HttpHeaders();

	@Autowired
	public HealthCheckController(HealthCheckService healthCheckService, MeterRegistry meterRegistry) {
		this.healthCheckService = healthCheckService;
		this.meterRegistry = meterRegistry;

		headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.set("X-Content-Type-Options", "nosniff");
	}
	//private final HealthCheckService healthCheckService;

	// public HealthCheckController(HealthCheckService healthCheckService) {
	// 	this.healthCheckService = healthCheckService;
	// }

	@GetMapping
	public ResponseEntity<Void> checkHealth(@RequestBody(required = false) String body,
			@RequestParam Map<String, String> queryParams) {

		long startTime = System.currentTimeMillis();

		// Increment the counter for total calls to this endpoint
    	meterRegistry.counter("api.healthz.get.calls").increment();

		try{
			if (body != null && !body.isEmpty() || !queryParams.isEmpty()) {
            	meterRegistry.counter("api.healthz.get.bad_request").increment();
            	return ResponseEntity.badRequest().headers(headers).build();
        	}
        	// if (!queryParams.isEmpty()) {
            // 	meterRegistry.counter("api.healthz.get.bad_request").increment();
            // 	return ResponseEntity.badRequest().headers(headers).build();
        	// }

			healthCheckService.recordHealthCheck();
			meterRegistry.counter("api.healthz.get.success").increment();
			return ResponseEntity.ok().headers(headers).build();
		} catch (HealthCheckException e) {
			meterRegistry.counter("api.healthz.get.service_unavailable").increment();
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).headers(headers).build();
		} catch (NotFoundException e) {
			 meterRegistry.counter("api.healthz.get.not_found").increment();
			return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).build();
		} catch (RuntimeException e) {  
			meterRegistry.counter("api.healthz.get.service_unavailable").increment();
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).headers(headers).build();
	    } finally {
			// Record the latency of the request
        	long duration = System.currentTimeMillis() - startTime;
        	meterRegistry.timer("api.healthz.get.latency").record(duration, TimeUnit.MILLISECONDS);
		}

	}

	@RequestMapping(method = { RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH,
			RequestMethod.HEAD, RequestMethod.OPTIONS })
	public ResponseEntity<String> methodNotAllowed() {

		meterRegistry.counter("api.healthz.invalid_method.calls").increment();
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers).build();
	}

	@RequestMapping(value = "/**", method = { RequestMethod.GET })
	public ResponseEntity<String> handleInvalidEndpoint() {

	    meterRegistry.counter("api.healthz.invalid_endpoint.calls").increment();
		return ResponseEntity.badRequest().headers(headers).build();
	}

}
