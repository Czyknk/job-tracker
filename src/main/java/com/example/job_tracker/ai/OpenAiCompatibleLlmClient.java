package com.example.job_tracker.ai;

import com.example.job_tracker.common.exceptions.ServiceUnavailableException;
import com.example.job_tracker.common.exceptions.UpstreamServiceException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Klient endpointu /chat/completions w formacie OpenAI. Działa z Gemini (warstwa zgodności),
 * Ollamą, Groq i OpenRouter: wystarczy zmienić base-url, model i klucz w konfiguracji.
 */
@Slf4j
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final RestClient restClient;
    private final AiProperties properties;

    public OpenAiCompatibleLlmClient(RestClient restClient, AiProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public LlmResponse complete(String systemPrompt, String userPrompt) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new ServiceUnavailableException(
                    "AI provider is not configured: set the GEMINI_API_KEY environment variable (app.ai.api-key)");
        }

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", systemPrompt), new ChatMessage("user", userPrompt));
        try {
            return completeWithModel(properties.model(), messages);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.warn("AI provider responded with HTTP {}: {}", status, e.getResponseBodyAsString());
            if (!isRetryable(status) || !hasFallback()) {
                throw upstreamException(status);
            }
            log.info("Primary AI model {} failed with HTTP {}; trying fallback model {}",
                    properties.model(), status, properties.fallbackModel());
            try {
                return completeWithModel(properties.fallbackModel(), messages);
            } catch (RestClientResponseException fallbackException) {
                int fallbackStatus = fallbackException.getStatusCode().value();
                log.warn("Fallback AI model {} responded with HTTP {}: {}",
                        properties.fallbackModel(), fallbackStatus, fallbackException.getResponseBodyAsString());
                throw upstreamException(fallbackStatus);
            }
        } catch (RestClientException e) {
            log.warn("AI provider call failed", e);
            throw new UpstreamServiceException("AI provider is unreachable or timed out");
        }
    }

    private LlmResponse completeWithModel(String model, List<ChatMessage> messages) {
        ChatResponse response = restClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ChatRequest(model, messages, 0.2))
                .retrieve()
                .body(ChatResponse.class);
        String text = extractText(response);
        String responseModel = response.model() != null && !response.model().isBlank()
                ? response.model()
                : model;
        return new LlmResponse(text, responseModel);
    }

    private boolean hasFallback() {
        return properties.fallbackModel() != null
                && !properties.fallbackModel().isBlank()
                && !properties.fallbackModel().equals(properties.model());
    }

    private static boolean isRetryable(int status) {
        return status == 429 || status >= 500;
    }

    private static UpstreamServiceException upstreamException(int status) {
        String hint = status == 429 ? " (rate limit reached, try again later)" : "";
        return new UpstreamServiceException("AI provider returned HTTP " + status + hint);
    }

    private static String extractText(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().getFirst().message() == null
                || response.choices().getFirst().message().content() == null
                || response.choices().getFirst().message().content().isBlank()) {
            throw new UpstreamServiceException("AI provider returned an empty response");
        }
        return response.choices().getFirst().message().content().trim();
    }

    record ChatRequest(String model, List<ChatMessage> messages, double temperature) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ChatMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(String model, List<Choice> choices) {
    }
}
