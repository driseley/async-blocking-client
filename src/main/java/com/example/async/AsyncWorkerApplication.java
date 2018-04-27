package com.example.async;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AsyncWorkerApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(AsyncWorkerApplication.class);
		application.setAdditionalProfiles("worker","member");
		application.run(args);
	}
}
