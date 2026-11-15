package com.example.job_tracker.ai;

/**
 * Cienka abstrakcja nad modelem językowym: "wyślij prompt, dostań tekst".
 * Prompty należą do funkcji biznesowych (np. streszczenie oferty), a nie do klienta.
 */
public interface LlmClient {

    LlmResponse complete(String systemPrompt, String userPrompt);

    record LlmResponse(String text, String model) {
    }
}
