package com.healthCheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HealthCheckApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthCheckApiApplication.class, args);
		
		System.out.println("Health Check API running.......");
	}

}
