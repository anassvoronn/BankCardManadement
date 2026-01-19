package org.nastya.demo.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleFk(DataIntegrityViolationException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "USER_HAS_CARDS");
        body.put("message", "User has active cards and cannot be deleted");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
