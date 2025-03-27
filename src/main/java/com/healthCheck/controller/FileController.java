package com.healthCheck.controller;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.healthCheck.exception.NotFoundException;
import com.healthCheck.model.File;
import com.healthCheck.service.FileService;

import io.micrometer.core.instrument.MeterRegistry;

@RestController
@RequestMapping("/v1/file")
public class FileController {
	
    private final MeterRegistry meterRegistry;
    private final FileService fileService;
	private final HttpHeaders headers = new HttpHeaders();

	@Autowired
	public FileController(FileService fileService, MeterRegistry meterRegistry) {
        this.fileService = fileService;
        this.meterRegistry = meterRegistry;

	    headers.setCacheControl("no-cache, no-store, must-revalidate");
	    headers.setPragma("no-cache");
	    headers.set("X-Content-Type-Options", "nosniff");
	}

	 //valid endpoints
	 
    @PostMapping
    public ResponseEntity<File> uploadFile(@RequestParam("profilePic") MultipartFile profilePic) {
        long startTime = System.currentTimeMillis();

        try {
            meterRegistry.counter("api.file.upload.calls").increment();

            File uploadedFile = fileService.uploadFile(profilePic);
            return ResponseEntity.status(HttpStatus.CREATED).headers(headers).body(uploadedFile);
        } catch (IOException e) {
            meterRegistry.counter("api.file.upload.errors").increment();
            return ResponseEntity.badRequest().headers(headers).build();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            meterRegistry.timer("api.file.upload.latency")
                        .record(duration, TimeUnit.MILLISECONDS);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<File> getFile(@PathVariable String id) {
        long startTime = System.currentTimeMillis();

        try {
            meterRegistry.counter("api.file.get.calls").increment();

            File file = fileService.getFile(id);
            return ResponseEntity.ok().headers(headers).body(file);
        } catch (NotFoundException e) {
            meterRegistry.counter("api.file.get.errors").increment();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).build();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            meterRegistry.timer("api.file.get.latency")
                        .record(duration, TimeUnit.MILLISECONDS);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable String id) {
        
//    	if (!isAuthenticated()) {
//    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//    	}
        long startTime = System.currentTimeMillis();
    	
    	try {
            meterRegistry.counter("api.file.delete.calls").increment();

            fileService.deleteFile(id);
            return ResponseEntity.noContent().headers(headers).build();
        } catch (NotFoundException e) {
            meterRegistry.counter("api.file.delete.errors").increment();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).build();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            meterRegistry.timer("api.file.delete.latency")
                        .record(duration, TimeUnit.MILLISECONDS);
        }
    }
    
 // Invalid endpoints with no parameters
    @GetMapping
    public ResponseEntity<Void> getFileWithoutId() {
        meterRegistry.counter("api.file.invalid_get.calls").increment();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteFileWithoutId() {
        meterRegistry.counter("api.file.invalid_delete.calls").increment();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).build();
    }

    @RequestMapping(method = { RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.OPTIONS })
    public ResponseEntity<Void> methodNotAllowed() {
        meterRegistry.counter("api.file.invalid_method.calls").increment();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers).build();
    }
    
 // Handle invalid methods for /v1/file/{id}
    @RequestMapping(value = "/{id}", method = { RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.PATCH, RequestMethod.POST, RequestMethod.HEAD })
    public ResponseEntity<Void> methodNotAllowedWithId() {
        meterRegistry.counter("api.file.invalid_method.calls").increment();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers).build();
    }
}
