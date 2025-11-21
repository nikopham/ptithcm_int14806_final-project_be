package com.ptithcm.movie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
public class MovieBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MovieBackendApplication.class, args);
	}

}
