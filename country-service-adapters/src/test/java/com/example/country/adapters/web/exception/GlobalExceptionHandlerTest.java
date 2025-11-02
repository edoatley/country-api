package com.example.country.adapters.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/countries/code/XX");
    }

    @Test
    void shouldHandleNoSuchElementException() {
        NoSuchElementException ex = new NoSuchElementException("Country not found");

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.get("status"));
        assertEquals("Not Found", body.get("error"));
        assertEquals("Country not found", body.get("message"));
        assertEquals("/api/v1/countries/code/XX", body.get("path"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void shouldHandleNoSuchElementExceptionWithNullMessage() {
        NoSuchElementException ex = new NoSuchElementException();

        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Resource not found", body.get("message"));
    }

    @Test
    void shouldHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid alpha2Code");

        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(400, body.get("status"));
        assertEquals("Bad Request", body.get("error"));
        assertEquals("Invalid alpha2Code", body.get("message"));
        assertEquals("/api/v1/countries/code/XX", body.get("path"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void shouldHandleIllegalArgumentExceptionWithNullMessage() {
        IllegalArgumentException ex = new IllegalArgumentException();

        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid request", body.get("message"));
    }

    @Test
    void shouldHandleGenericException() {
        RuntimeException ex = new RuntimeException("Unexpected error");

        ResponseEntity<Map<String, Object>> response = handler.handleInternalError(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.get("status"));
        assertEquals("Internal Server Error", body.get("error"));
        assertEquals("An unexpected error occurred", body.get("message"));
        assertEquals("/api/v1/countries/code/XX", body.get("path"));
        assertNotNull(body.get("timestamp"));
    }
}
