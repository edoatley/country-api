package com.example.country.adapters.web.exception;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Error response schema matching OpenAPI specification.
     * This schema is used by SpringDoc to generate the Error schema in the OpenAPI spec.
     * Note: No description on schema itself to match static spec.
     * Note: No example values on properties - examples are at schema level in static spec.
     */
    @Schema(name = "Error")
    public static class ErrorResponse {
        @Schema(format = "date-time", required = true)
        public String timestamp;
        
        @Schema(format = "int32", required = true)
        public Integer status;
        
        @Schema(required = true)
        public String error;
        
        @Schema(description = "A human-readable description of the error.", required = true)
        public String message;
        
        @Schema(required = true)
        public String path;
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ApiResponse(responseCode = "404", description = "Not Found", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex, jakarta.servlet.http.HttpServletRequest request) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.NOT_FOUND.value(),
                "error", "Not Found",
                "message", ex.getMessage() != null ? ex.getMessage() : "Resource not found",
                "path", request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ApiResponse(responseCode = "400", description = "Bad Request", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex, jakarta.servlet.http.HttpServletRequest request) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "error", "Bad Request",
                "message", ex.getMessage() != null ? ex.getMessage() : "Invalid request",
                "path", request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    @ApiResponse(responseCode = "500", description = "Internal Server Error", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Map<String, Object>> handleInternalError(Exception ex, jakarta.servlet.http.HttpServletRequest request) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "error", "Internal Server Error",
                "message", "An unexpected error occurred",
                "path", request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
