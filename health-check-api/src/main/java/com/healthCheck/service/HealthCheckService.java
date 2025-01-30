package com.healthCheck.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

import com.healthCheck.exception.HealthCheckException;
import com.healthCheck.model.HealthCheck;
import com.healthCheck.repository.HealthCheckRepository;

@Service
public class HealthCheckService {

	private final HealthCheckRepository healthCheckRepo;

	@Autowired
	public HealthCheckService(HealthCheckRepository healthCheckRepo) {
		super();
		this.healthCheckRepo = healthCheckRepo;
	}

	public void recordHealthCheck() {
		try {
			HealthCheck healthCheck = new HealthCheck();
			healthCheck.setDatetime(OffsetDateTime.now(ZoneOffset.UTC));
			healthCheckRepo.save(healthCheck);
		} catch (CannotCreateTransactionException e) {
			throw new HealthCheckException("Database connection error", e);
		} catch (DataAccessException e) {
			throw new HealthCheckException("Database access error", e);
		} catch (Exception e) {
			throw new HealthCheckException("Unexpected error during health check", e);
		}
	}
}
