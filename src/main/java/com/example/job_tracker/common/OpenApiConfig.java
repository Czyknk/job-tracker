package com.example.job_tracker.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobTrackerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Job Tracker API")
                .description("Track job applications, recruitment stages and companies in one place.")
                .version("0.1.0"));
    }
}
