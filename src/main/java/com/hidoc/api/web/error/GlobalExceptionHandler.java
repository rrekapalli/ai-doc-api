package com.hidoc.api.web.error;

import com.hidoc.api.exception.AIServiceUnavailableException;
import com.hidoc.api.exception.AuthenticationException;
import com.hidoc.api.exception.InvalidRequestException;
import com.hidoc.api.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, request, "Validation Failed");
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        body.put("errors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class, InvalidRequestException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Object> handleBadRequest(Exception ex, HttpServletRequest request) {
        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST, request, safeMessage(ex, "Invalid request"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Object> handleTooMany(RateLimitExceededException ex, HttpServletRequest request) {
        Map<String, Object> body = baseBody(HttpStatus.TOO_MANY_REQUESTS, request, ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler({AuthenticationException.class})
    public ResponseEntity<Object> handleAuth(AuthenticationException ex, HttpServletRequest request) {
        Map<String, Object> body = baseBody(HttpStatus.UNAUTHORIZED, request, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<Object> handleSpringAuth(org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
        Map<String, Object> body = baseBody(HttpStatus.UNAUTHORIZED, request, "Authentication failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        Map<String, Object> body = baseBody(HttpStatus.FORBIDDEN, request, "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AIServiceUnavailableException.class)
    public ResponseEntity<Object> handleServiceUnavailable(AIServiceUnavailableException ex, HttpServletRequest request) {
        Map<String, Object> body = baseBody(HttpStatus.SERVICE_UNAVAILABLE, request, ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error: {}", ex.getMessage());
        Map<String, Object> body = baseBody(HttpStatus.INTERNAL_SERVER_ERROR, request, "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> baseBody(HttpStatus status, HttpServletRequest request, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("status", status.value());
        body.put("timestamp", LocalDateTime.now());
        body.put("path", request != null ? request.getRequestURI() : null);
        return body;
    }

    private String safeMessage(Exception ex, String fallback) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) return fallback;
        // Basic sanitization: strip line breaks to avoid log injection in clients
        return msg.replaceAll("[\r\n]", " ");
    }
}
