package com.healthCheck.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.healthCheck.exception.HealthCheckException;
import com.healthCheck.model.HealthCheck;
import com.healthCheck.repository.HealthCheckRepository;

@Service
public class HealthCheckService {

	private final HealthCheckRepository healthCheckRepo;

	@Autowired
	public HealthCheckService(HealthCheckRepository healthCheckRepo) {
		this.healthCheckRepo = healthCheckRepo;
	}

	public void recordHealthCheck() {
		try {
			HealthCheck healthCheck = new HealthCheck();
			healthCheck.setDatetime(OffsetDateTime.now(ZoneOffset.UTC));
			healthCheckRepo.save(healthCheck);
		}  catch (Exception e) {
			throw new HealthCheckException("Check Database connection", e);
		}
	}
}
