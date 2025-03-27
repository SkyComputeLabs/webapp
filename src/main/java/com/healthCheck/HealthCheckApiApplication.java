package com.healthCheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy
public class HealthCheckApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthCheckApiApplication.class, args);
		
		System.out.println("Health Check API running.......");
	}

}
