package com.example.country.adapters.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyAuthenticationFilterTest {

    private ApiKeyAuthenticationFilter filter;
    private Environment environment;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        filter = new ApiKeyAuthenticationFilter(environment);
    }

    @Test
    void shouldAllowRequestWithValidApiKey() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/countries");
        when(request.getHeader("X-API-KEY")).thenReturn("valid-key");
        when(environment.getProperty("api.key", "default-test-key")).thenReturn("valid-key");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void shouldRejectRequestWithInvalidApiKey() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        
        when(request.getRequestURI()).thenReturn("/api/v1/countries");
        when(request.getHeader("X-API-KEY")).thenReturn("invalid-key");
        when(environment.getProperty("api.key", "default-test-key")).thenReturn("valid-key");
        when(response.getWriter()).thenReturn(writer);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        assertTrue(stringWriter.toString().contains("Unauthorized"));
    }

    @Test
    void shouldRejectRequestWithMissingApiKey() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        
        when(request.getRequestURI()).thenReturn("/api/v1/countries");
        when(request.getHeader("X-API-KEY")).thenReturn(null);
        when(environment.getProperty("api.key", "default-test-key")).thenReturn("valid-key");
        when(response.getWriter()).thenReturn(writer);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        assertTrue(stringWriter.toString().contains("Unauthorized"));
    }

    @Test
    void shouldAllowActuatorEndpointsWithoutApiKey() throws Exception {
        when(request.getRequestURI()).thenReturn("/actuator/health");
        when(request.getHeader("X-API-KEY")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void shouldAllowActuatorEndpointsWithNullPath() throws Exception {
        when(request.getRequestURI()).thenReturn(null);
        when(request.getHeader("X-API-KEY")).thenReturn(null);
        when(environment.getProperty("api.key", "default-test-key")).thenReturn("valid-key");
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilterInternal(request, response, filterChain);

        // When path is null, it won't start with /actuator/, so it should require API key
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(401);
    }

    @Test
    void shouldUseDefaultApiKeyWhenNotConfigured() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/countries");
        when(request.getHeader("X-API-KEY")).thenReturn("default-test-key");
        when(environment.getProperty("api.key", "default-test-key")).thenReturn("default-test-key");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
