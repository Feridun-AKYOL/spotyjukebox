package org.bithub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Global exception handler for REST controllers.
 * Provides consistent error responses for validation and unexpected errors.
 */
@RestControllerAdvice
public class ApiErrorHandler {

    /**
     * Handles validation errors triggered by invalid request body parameters.
     * Returns the first field error message in a standardized format.
     *
     * @param ex the exception containing validation details
     * @return a {@link ResponseEntity} with an error message and HTTP 400 status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        var firstError = ex.getBindingResult().getFieldErrors().stream().findFirst();
        String message = firstError
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("Validation error");

        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION",
                "message", message
        ));
    }

    /**
     * Handles all unexpected exceptions that occur during request processing.
     *
     * @param ex the thrown exception
     * @return a {@link ResponseEntity} with a generic error message and HTTP 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex) {
        return ResponseEntity.status(500).body(Map.of(
                "error", "INTERNAL",
                "message", "Unexpected error"
        ));
    }
}
