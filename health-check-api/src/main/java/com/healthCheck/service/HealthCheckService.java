package com.healthCheck.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.healthCheck.exception.HealthCheckException;
import com.healthCheck.model.HealthCheck;
import com.healthCheck.repository.HealthCheckRepository;

@Service
public class HealthCheckService {

	@Autowired
	private final HealthCheckRepository healthCheckRepo;
	
	@Autowired
    private DataSource dataSource;
	
	@Autowired
    private JdbcTemplate jdbcTemplate;

	public HealthCheckService(HealthCheckRepository healthCheckRepo) {
		this.healthCheckRepo = healthCheckRepo;
	}

	public void recordHealthCheck() throws HealthCheckException{
		try (Connection connection = dataSource.getConnection()){
			if(connection == null || connection.isClosed()) {
				throw new HealthCheckException("Database is down");
			} else {
				HealthCheck healthCheck = new HealthCheck();
				healthCheck.setDatetime(OffsetDateTime.now(ZoneOffset.UTC));
				healthCheckRepo.save(healthCheck);
			}
		} catch (SQLException e) {
			throw new HealthCheckException("Database is down", e); 
		}
	}
	
	public boolean isDatabaseRunning() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true; 
        } catch (Exception e) {
            return false; 
        }
    }
}
