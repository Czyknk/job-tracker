package com.example.job_tracker.common.exceptions;

/**
 * Exception thrown when a user tries to access a resource without proper authorization.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
