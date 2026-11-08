package com.cement.prefacture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class PrefactureApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrefactureApplication.class, args);
	}

}
