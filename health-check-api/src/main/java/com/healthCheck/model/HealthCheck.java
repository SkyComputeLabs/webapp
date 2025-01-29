package com.healthCheck.model;

import java.time.OffsetDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class HealthCheck {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long checkId;
	private OffsetDateTime datetime;
	
	public Long getCheckId() {
		return checkId;
	}
	
	public void setCheckId(Long checkId) {
		this.checkId = checkId;
	}
	
	public OffsetDateTime getDatetime() {
		return datetime;
	}
	
	public void setDatetime(OffsetDateTime datetime) {
		this.datetime = datetime;
	}
	
	
}
