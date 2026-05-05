package com.team.apk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAsync
public class ApkApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApkApplication.class, args);
	}
}
