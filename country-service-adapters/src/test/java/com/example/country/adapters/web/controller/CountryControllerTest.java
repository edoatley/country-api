package com.example.country.adapters.web.controller;

import com.example.country.adapters.api.CountryApi;
import com.example.country.application.model.CountryInput;
import com.example.country.domain.Country;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CountryControllerTest {

    private CountryController controller;
    private CountryApi countryApi;

    @BeforeEach
    void setUp() {
        countryApi = mock(CountryApi.class);
        controller = new CountryController(countryApi);
    }

    @Test
    void shouldGetAllCountries() {
        Country country = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now(), null, false);
        when(countryApi.listCountries(20, 0)).thenReturn(List.of(country));

        ResponseEntity<List<Country>> response = controller.getAllCountries(20, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("GB", response.getBody().get(0).alpha2Code());
        verify(countryApi).listCountries(20, 0);
    }

    @Test
    void shouldCreateCountry() {
        CountryInput input = new CountryInput("United Kingdom", "GB", "GBR", "826");
        Country created = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now(), null, false);
        when(countryApi.createCountry(input)).thenReturn(created);

        ResponseEntity<Country> response = controller.createCountry(input);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("GB", response.getBody().alpha2Code());
        verify(countryApi).createCountry(input);
    }

    @Test
    void shouldGetByAlpha2() {
        Country country = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now(), null, false);
        when(countryApi.getByAlpha2("GB")).thenReturn(country);

        ResponseEntity<Country> response = controller.getByAlpha2("GB");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("GB", response.getBody().alpha2Code());
        verify(countryApi).getByAlpha2("GB");
    }

    @Test
    void shouldUpdateByAlpha2() {
        CountryInput input = new CountryInput("United Kingdom Updated", "GB", "GBR", "826");
        Country updated = Country.of("United Kingdom Updated", "GB", "GBR", "826", Instant.now(), null, false);
        when(countryApi.updateByAlpha2("GB", input)).thenReturn(updated);

        ResponseEntity<Country> response = controller.updateByAlpha2("GB", input);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("United Kingdom Updated", response.getBody().name());
        verify(countryApi).updateByAlpha2("GB", input);
    }

    @Test
    void shouldDeleteByAlpha2() {
        doNothing().when(countryApi).deleteByAlpha2("GB");

        ResponseEntity<Void> response = controller.deleteByAlpha2("GB");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(countryApi).deleteByAlpha2("GB");
    }

    @Test
    void shouldGetHistoryByAlpha2() {
        Country country1 = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now().minusSeconds(3600), null, false);
        Country country2 = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now(), null, false);
        when(countryApi.historyByAlpha2("GB")).thenReturn(List.of(country2, country1));

        ResponseEntity<List<Country>> response = controller.getHistory("GB");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(countryApi).historyByAlpha2("GB");
    }

    @Test
    void shouldGetByAlpha3() {
        Country country = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now(), null, false);
        when(countryApi.getByAlpha3("GBR")).thenReturn(country);

        ResponseEntity<Country> response = controller.getByAlpha3("GBR");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("GBR", response.getBody().alpha3Code());
        verify(countryApi).getByAlpha3("GBR");
    }

    @Test
    void shouldGetByNumeric() {
        Country country = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now(), null, false);
        when(countryApi.getByNumeric("826")).thenReturn(country);

        ResponseEntity<Country> response = controller.getByNumeric("826");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("826", response.getBody().numericCode());
        verify(countryApi).getByNumeric("826");
    }
}
