package org.bithub.controller;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex){
        var first = ex.getBindingResult().getFieldErrors().stream().findFirst();
        String msg = first.map(f -> f.getField() + " " + f.getDefaultMessage())
                          .orElse("Validation error");
        return ResponseEntity.badRequest().body(Map.of(
                "error","VALIDATION",
                "message", msg
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleOther(Exception ex){
        return ResponseEntity.status(500).body(Map.of(
                "error","INTERNAL",
                "message","Unexpected error"
        ));
    }
}
