package com.healthCheck.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class File {

	@Id
	private String id = UUID.randomUUID().toString();
	
	private String fileName;
	
	private String url;
	
	private OffsetDateTime uploadDate = OffsetDateTime.now();

	public File() {
		super();
	}

	public File(String fileName, String url) {
		super();
		this.fileName = fileName;
		this.url = url;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public OffsetDateTime getUploadDate() {
		return uploadDate;
	}

	public void setUploadDate(OffsetDateTime uploadDate) {
		this.uploadDate = uploadDate;
	}
	
	
}
