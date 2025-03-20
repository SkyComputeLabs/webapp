package com.healthCheck.controller;

import java.io.IOException;

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

@RestController
@RequestMapping("/v1/file")
public class FileController {
	
	private final HttpHeaders headers = new HttpHeaders();

	@Autowired
    private FileService fileService;
	
	 public FileController() {
	        headers.setCacheControl("no-cache, no-store, must-revalidate");
	        headers.setPragma("no-cache");
	        headers.set("X-Content-Type-Options", "nosniff");
	    }

	 //valid endpoints
	 
    @PostMapping
    public ResponseEntity<File> uploadFile(@RequestParam("profilePic") MultipartFile profilePic) {
        try {
            File uploadedFile = fileService.uploadFile(profilePic);
            return ResponseEntity.status(HttpStatus.CREATED).headers(headers).body(uploadedFile);
        } catch (IOException e) {
            return ResponseEntity.badRequest().headers(headers).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<File> getFile(@PathVariable String id) {
        try {
            File file = fileService.getFile(id);
            return ResponseEntity.ok().headers(headers).body(file);
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable String id) {
        
//    	if (!isAuthenticated()) {
//    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//    	}
       
    	
    	try {
            fileService.deleteFile(id);
            return ResponseEntity.noContent().headers(headers).build();
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).headers(headers).build();
        }
    }
    
 // Invalid endpoints with no parameters
    @GetMapping
    public ResponseEntity<Void> getFileWithoutId() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteFileWithoutId() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).build();
    }

    @RequestMapping(method = { RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.HEAD, RequestMethod.OPTIONS })
    public ResponseEntity<Void> methodNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers).build();
    }
    
 // Handle invalid methods for /v1/file/{id}
    @RequestMapping(value = "/{id}", method = { RequestMethod.PUT, RequestMethod.OPTIONS, RequestMethod.PATCH, RequestMethod.POST })
    public ResponseEntity<Void> methodNotAllowedWithId() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(headers).build();
    }
}
