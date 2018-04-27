package com.example.async;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AsyncControllerApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(AsyncControllerApplication.class);
		application.setAdditionalProfiles("controller");
		application.run(args);
	}
}
