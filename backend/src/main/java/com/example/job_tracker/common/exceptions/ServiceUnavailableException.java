package com.example.job_tracker.common.exceptions;

/**
 * Funkcja niedostępna, bo nie jest skonfigurowana (mapowane na HTTP 503).
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
