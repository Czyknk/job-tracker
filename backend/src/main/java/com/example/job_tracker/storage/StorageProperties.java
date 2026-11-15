package com.example.job_tracker.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        @DefaultValue("job-tracker") String bucket,
        @DefaultValue("us-east-1") String region) {
}
