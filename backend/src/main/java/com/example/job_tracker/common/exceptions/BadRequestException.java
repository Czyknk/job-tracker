package com.example.job_tracker.common.exceptions;

/**
 * Niepoprawne żądanie wykryte poza Bean Validation (mapowane na HTTP 400).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
