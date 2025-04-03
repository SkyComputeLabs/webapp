package com.healthCheck.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.healthCheck.exception.HealthCheckException;
import com.healthCheck.model.HealthCheck;
import com.healthCheck.repository.HealthCheckRepository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class HealthCheckService {

	private static final Logger logger = LoggerFactory.getLogger(HealthCheckService.class);

	private final HealthCheckRepository healthCheckRepo;

	private final MeterRegistry meterRegistry;
	
	@Autowired
    private DataSource dataSource;
	
	@Autowired
    private JdbcTemplate jdbcTemplate;

	public HealthCheckService(HealthCheckRepository healthCheckRepo, MeterRegistry meterRegistry) {
		this.healthCheckRepo = healthCheckRepo;
		this.meterRegistry = meterRegistry;
	}

	public void recordHealthCheck() throws HealthCheckException{
		Timer.Sample connectionTimer = Timer.start(meterRegistry);
        Timer.Sample saveTimer = Timer.start(meterRegistry);
        
        logger.info("Starting database health check recording");

		try (Connection connection = dataSource.getConnection()){
			connectionTimer.stop(meterRegistry.timer("db.healthcheck.connection.time"));

			if(connection == null || connection.isClosed()) {
				logger.error("Database connection is closed or unavailable");
                meterRegistry.counter("db.healthcheck.errors", "reason", "connection_closed").increment();
				throw new HealthCheckException("Database is down");
			} 

			logger.info("Database connection established successfully");

			HealthCheck healthCheck = new HealthCheck();
			healthCheck.setDatetime(OffsetDateTime.now(ZoneOffset.UTC));

			logger.info("Saving health check record to database");
            //saveTimer.start();
			healthCheckRepo.save(healthCheck);
			saveTimer.stop(meterRegistry.timer("db.healthcheck.save.time"));
            
            logger.info("Health check record saved successfully");
		} catch (SQLException e) {
			logger.error("SQL Exception during health check recording: {}", e.getMessage());
            meterRegistry.counter("db.healthcheck.errors", "reason", "sql_exception").increment();
			throw new HealthCheckException("Database is down", e); 
		} catch (DataAccessException e) {
            logger.error("Data access exception during health check recording: {}", e.getMessage());
            meterRegistry.counter("db.healthcheck.errors", "reason", "data_access").increment();
            throw new HealthCheckException("Database operation failed", e);
        } finally {
            connectionTimer.stop(meterRegistry.timer("db.healthcheck.connection.time"));
            saveTimer.stop(meterRegistry.timer("db.healthcheck.save.time"));
        }
	}
	
	public boolean isDatabaseRunning() {
		logger.info("Checking database connectivity");
        Timer.Sample timer = Timer.start(meterRegistry);

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
			logger.info("Database connectivity check successful");
            return true; 
        } catch (DataAccessException e) {
            logger.error("Database connectivity check failed: {}", e.getMessage());
            meterRegistry.counter("db.healthcheck.errors", "reason", "connectivity_failure").increment();
            return false;
        } finally {
            timer.stop(meterRegistry.timer("db.healthcheck.connectivity.time"));
        }
    }
}



