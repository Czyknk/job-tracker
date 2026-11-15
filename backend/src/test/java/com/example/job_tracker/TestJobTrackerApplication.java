package com.example.job_tracker;

import org.springframework.boot.SpringApplication;

public class TestJobTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.from(JobTrackerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
