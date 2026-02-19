package com.dodo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(modifyOnCreate = false)
@SpringBootApplication
public class DodoBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DodoBackendApplication.class, args);
	}

}
