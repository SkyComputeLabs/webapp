package com.healthCheck.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.healthCheck.exception.NotFoundException;
import com.healthCheck.exception.AmazonS3Exception;
import com.healthCheck.model.File;
import com.healthCheck.repository.FileRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class FileService {

	private final MeterRegistry meterRegistry;
	
	private static final Logger logger = LoggerFactory.getLogger(FileService.class);

	private final AmazonS3 s3Client;

    private final FileRepository fileRepo;
	
	private final String bucketName = "first-s3-bucket-6225";

	@Autowired
	public FileService(AmazonS3 s3Client, FileRepository fileRepo, MeterRegistry meterRegistry) {
		this.s3Client = s3Client;
		this.fileRepo = fileRepo;
		this.meterRegistry = meterRegistry;
	}
	
	public File uploadFile(MultipartFile file) throws IOException {
		String fileId = java.util.UUID.randomUUID().toString();
        String key = fileId + "/" + file.getOriginalFilename();
         
		logger.info("Starting file upload process for file: {}", file.getOriginalFilename());
		logger.info("Generated file ID: {}", fileId);
        logger.info("S3 key for file: {}", key);

		Timer.Sample s3Timer = Timer.start(meterRegistry);
        Timer.Sample dbTimer = Timer.start(meterRegistry);

		try {
        	ObjectMetadata metadata = new ObjectMetadata();
        	metadata.setContentType(file.getContentType());
        	metadata.setContentLength(file.getSize());

			logger.info("Preparing to upload file to S3. Bucket: {}, key: {}", bucketName, key);

        	// Upload file to S3 bucket
        	s3Client.putObject(new PutObjectRequest(bucketName, key, file.getInputStream(), metadata));

			logger.info("File successfully uploaded to S3");
			s3Timer.stop(meterRegistry.timer("s3.upload.time"));

        	// Construct public URL
        	String url = s3Client.getUrl(bucketName, key).toString();
			logger.info("Generated public URL for file: {}", url);

        	// Save metadata in the database
        	File newFile = new File();
        	newFile.setId(fileId);
        	newFile.setFileName(file.getOriginalFilename());
        	newFile.setUrl(url);
        	newFile.setUploadDate(OffsetDateTime.now());

			logger.info("Saving file metadata to database");
			File savedFile = fileRepo.save(newFile);
			logger.info("File metadata successfully saved to database");
			dbTimer.stop(meterRegistry.timer("db.file.save.time"));

			return savedFile;

		} catch (AmazonS3Exception e) {
            logger.error("AmazonS3Exception occurred while uploading file. Error: {}", e.getMessage());
            logger.error("Error Code: {}, Error Type: {}, Request ID: {}", e.getErrorCode(), e.getErrorType(), e.getRequestId());
			meterRegistry.counter("s3.upload.errors").increment();
            throw new RuntimeException("Error uploading file to S3: " + e.getMessage(), e);
        } catch (SdkClientException e) {
            logger.error("SdkClientException occurred while uploading file. Error: {}", e.getMessage());
			meterRegistry.counter("s3.upload.errors").increment();
            throw new RuntimeException("AWS SDK client error: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.error("IllegalArgumentException occurred. This might be due to invalid input. Error: {}", e.getMessage());
            meterRegistry.counter("file.upload.errors").increment();
			throw new RuntimeException("Invalid argument provided: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("IOException occurred while reading file input stream. Error: {}", e.getMessage());
			meterRegistry.counter("file.upload.errors").increment();
            throw new RuntimeException("Error reading file: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred during file upload. Error: {}", e.getMessage());
			meterRegistry.counter("file.upload.errors").increment();
            throw new RuntimeException("Unexpected error during file upload: " + e.getMessage(), e);
        } finally {
            s3Timer.stop(meterRegistry.timer("s3.upload.time"));
            dbTimer.stop(meterRegistry.timer("db.file.save.time"));
        }
    }
	
	public File getFile(String id) {
		logger.info("Attempting to retrieve file with id: {}", id);
        Timer.Sample timer = Timer.start(meterRegistry);
		try{
			Optional<File> fileOptional = fileRepo.findById(id);
	    	if (fileOptional.isPresent()) {
				logger.info("File found with id: {}", id);
	        	return fileOptional.get();
	    	} else {
				logger.warn("File not found with id: {}", id);
                meterRegistry.counter("db.file.not_found").increment();
	        	throw new NotFoundException("File not found with id: " + id);
	    	}
		} finally {
            timer.stop(meterRegistry.timer("db.file.read.time"));
        }
	}

	public void deleteFile(String id) throws NotFoundException {
	    logger.info("Attempting to delete file with id: {}", id);
        Timer.Sample s3Timer = Timer.start(meterRegistry);
        Timer.Sample dbTimer = Timer.start(meterRegistry);
		
	    try {
	        File file = getFile(id);
			
			 // Extract S3 key from URL
            String key = file.getUrl().replace(s3Client.getUrl(bucketName, "").toString(), "");
			logger.info("Deleting file from S3. Bucket: {}, Key: {}", bucketName, key);

			// Delete from S3 bucket
            s3Client.deleteObject(bucketName, key);
            logger.info("File successfully deleted from S3");
            s3Timer.stop(meterRegistry.timer("s3.delete.time"));

            logger.info("Deleting file metadata from database");
            // Delete from database
            fileRepo.delete(file);
            logger.info("File metadata successfully deleted from database");
            dbTimer.stop(meterRegistry.timer("db.file.delete.time"));

	    } catch (AmazonS3Exception e) {
	        // Log the error and throw a custom exception
	        logger.error("Error deleting file from S3: " + e.getMessage());
			meterRegistry.counter("s3.delete.errors").increment();
	        throw new RuntimeException("Error deleting file from S3", e);
	    } catch (Exception e) {
	        // Log the error and throw a custom exception
	        logger.error("Error deleting file: " + e.getMessage());
			meterRegistry.counter("db.file.delete.errors").increment();
	        throw new RuntimeException("Error deleting file", e);
	    } finally {
            s3Timer.stop(meterRegistry.timer("s3.delete.time"));
            dbTimer.stop(meterRegistry.timer("db.file.delete.time"));
        }
	}
}


