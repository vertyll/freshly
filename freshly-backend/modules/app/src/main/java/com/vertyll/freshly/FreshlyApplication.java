package com.vertyll.freshly;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.vertyll.freshly")
@EnableScheduling
@SuppressWarnings("PMD.UseUtilityClass") // Spring Boot application class must be instantiable
public class FreshlyApplication {

	public static void main(String[] args) {
		SpringApplication.run(FreshlyApplication.class, args);
	}

}
