package com.example.job_tracker.common.exceptions;

/**
 * Naruszenie reguły biznesowej / konflikt ze stanem zasobu (mapowane na HTTP 409).
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
