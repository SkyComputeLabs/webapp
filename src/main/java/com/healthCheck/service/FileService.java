package com.healthCheck.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.healthCheck.exception.NotFoundException;
import com.healthCheck.exception.AmazonS3Exception;
import com.healthCheck.model.File;
import com.healthCheck.repository.FileRepository;

@Service
public class FileService {
	
	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	@Autowired
	private AmazonS3 s3Client;
	
	@Autowired
    private FileRepository fileRepo;
	
	private final String bucketName = System.getenv("S3_BUCKET");
	
	public File uploadFile(MultipartFile file) throws IOException {
        String fileId = java.util.UUID.randomUUID().toString();
        String key = fileId + "/" + file.getOriginalFilename();
        
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        // Upload file to S3 bucket
        s3Client.putObject(new PutObjectRequest(bucketName, key, file.getInputStream(), metadata)
                .withCannedAcl(CannedAccessControlList.PublicRead));

        // Construct public URL
        String url = s3Client.getUrl(bucketName, key).toString();

        // Save metadata in the database
        File newFile = new File();
        newFile.setId(fileId);
        newFile.setFileName(file.getOriginalFilename());
        newFile.setUrl(url);
        newFile.setUploadDate(OffsetDateTime.now());
        
        return fileRepo.save(newFile);
    }
	
	public File getFile(String id) {
	    Optional<File> fileOptional = fileRepo.findById(id);
	    if (fileOptional.isPresent()) {
	        return fileOptional.get();
	    } else {
	        throw new NotFoundException("File not found with id: " + id);
	    }
	}

	public void deleteFile(String id) throws NotFoundException {
	    File file;
	    try {
	        file = getFile(id);
	    } catch (NotFoundException e) {
	        throw new NotFoundException("File not found with id: " + id);
	    }

	    try {
	        // Extract S3 key from URL
	        String key = file.getUrl().replace(s3Client.getUrl(bucketName, "").toString(), "");

	        // Delete from S3 bucket
	        s3Client.deleteObject(bucketName, key);

	        // Delete from database
	        fileRepo.delete(file);
	    } catch (AmazonS3Exception e) {
	        // Log the error and throw a custom exception
	        logger.error("Error deleting file from S3: " + e.getMessage());
	        throw new RuntimeException("Error deleting file from S3", e);
	    } catch (Exception e) {
	        // Log the error and throw a custom exception
	        logger.error("Error deleting file: " + e.getMessage());
	        throw new RuntimeException("Error deleting file", e);
	    }
	}

}
