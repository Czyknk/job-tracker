package com.example.job_tracker.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Konfiguracja dostawcy zgodnego z API OpenAI (Gemini, Ollama, Groq, OpenRouter...).
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String baseUrl,
        String model,
        String apiKey,
        @DefaultValue("60s") Duration timeout) {
}
