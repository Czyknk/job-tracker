package com.example.job_tracker.common;

import java.util.LinkedHashMap;
import java.util.Map;

import com.example.job_tracker.common.exceptions.*;
import com.example.job_tracker.storage.StorageException;
import com.example.job_tracker.storage.StorageObjectNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Place to map exceptions to RFC 9457 (ProblemDetail) error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        // The content of the database exception is intentionally not sent to the client.
        log.warn("Data integrity violation", ex);
        return problem(HttpStatus.CONFLICT, "Data integrity violation",
                "The request conflicts with existing data");
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed",
                "Request validation failed");
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody() {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request",
                "Request body is malformed or contains an invalid value");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid parameter",
                "Invalid value for parameter '" + ex.getName() + "'");
    }

    @ExceptionHandler(UpstreamServiceException.class)
    public ProblemDetail handleUpstream(UpstreamServiceException ex) {
        return problem(HttpStatus.BAD_GATEWAY, "Upstream service error", ex.getMessage());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ProblemDetail handleUnavailable(ServiceUnavailableException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable", ex.getMessage());
    }

    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorage(StorageException ex) {
        log.error("Object storage operation failed", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Object storage unavailable",
                "The document storage is temporarily unavailable");
    }

    @ExceptionHandler(StorageObjectNotFoundException.class)
    public ProblemDetail handleMissingStorageObject() {
        return problem(HttpStatus.NOT_FOUND, "Document file not found",
                "The document metadata exists, but its file is missing from object storage");
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
