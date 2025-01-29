package com.healthCheck.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.healthCheck.model.HealthCheck;

public interface HealthCheckRepository extends JpaRepository<HealthCheck, Long>{

}
