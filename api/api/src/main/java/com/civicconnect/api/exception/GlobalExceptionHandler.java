package com.civicconnect.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler that provides consistent error responses
 * with correlation IDs for request tracing.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String CORRELATION_ID_KEY = "correlationId";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            fieldErrors.put(fieldName, message);
        });

        log.warn("Validation failed: {} errors | Path: {} | Errors: {}",
                ex.getErrorCount(), request.getRequestURI(), fieldErrors);

        Map<String, Object> error = buildBaseError(HttpStatus.BAD_REQUEST, "Validation Failed");
        error.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        log.warn("Missing required header: {} | Path: {}", ex.getHeaderName(), request.getRequestURI());

        Map<String, Object> error = buildBaseError(HttpStatus.BAD_REQUEST, "Bad Request");
        error.put("message", "Missing required header: " + ex.getHeaderName());
        error.put("hint", "Ensure you are logged in and the X-User-Id header is set");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {} | Path: {}", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        String message = ex.getMessage();

        // Check for common patterns to return appropriate status
        if (message != null && message.toLowerCase().contains("not found")) {
            log.warn("Runtime not found: {} | Path: {}", message, request.getRequestURI());
            return buildErrorResponse(HttpStatus.NOT_FOUND, "Not Found", message);
        }

        // Log full stack trace for unexpected errors
        log.error("Unhandled runtime exception | Path: {} | Method: {} | Query: {}",
                request.getRequestURI(),
                request.getMethod(),
                request.getQueryString(),
                ex);

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                message != null ? message : "An unexpected error occurred");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected exception | Path: {} | Method: {} | Type: {}",
                request.getRequestURI(),
                request.getMethod(),
                ex.getClass().getSimpleName(),
                ex);

        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.");
    }

    /**
     * Builds a standard error response with the given status, error type, and message.
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String error, String message) {
        Map<String, Object> errorMap = buildBaseError(status, error);
        errorMap.put("message", message);
        return ResponseEntity.status(status).body(errorMap);
    }

    /**
     * Builds the base error map with timestamp, status, error type, and correlation ID.
     */
    private Map<String, Object> buildBaseError(HttpStatus status, String error) {
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("timestamp", LocalDateTime.now());
        errorMap.put("status", status.value());
        errorMap.put("error", error);

        // Include correlation ID for request tracing
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (correlationId != null) {
            errorMap.put("correlationId", correlationId);
        }

        return errorMap;
    }
}
