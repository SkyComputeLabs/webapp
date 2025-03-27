package com.healthCheck.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
public class AwsConfig {

	@Bean
	public AmazonS3 amazonS3() {
		String region = "us-east-2";
		//String region = System.getenv("AWS_REGION");
		// if (region == null || region.isEmpty()) {
		// 	throw new IllegalStateException("AWS_REGION environment variable is not set");
		// }
		return AmazonS3ClientBuilder.standard().withRegion(region)
				.withCredentials(new DefaultAWSCredentialsProviderChain()).build();
	}
}
