package com.example.job_tracker.common.exceptions;

/**
 * Błąd zewnętrznej usługi, np. dostawcy modelu językowego (mapowane na HTTP 502).
 * Komunikat trafia do klienta, więc nie może zawierać sekretów.
 */
public class UpstreamServiceException extends RuntimeException {

    public UpstreamServiceException(String message) {
        super(message);
    }
}
