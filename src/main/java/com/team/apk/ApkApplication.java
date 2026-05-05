package com.team.apk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ApkApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApkApplication.class, args);
	}
}
