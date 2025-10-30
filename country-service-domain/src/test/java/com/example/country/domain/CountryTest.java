package com.example.country.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CountryTest {
    @Test
    void canCreateValidCountry() {
        Country c = Country.of("United Kingdom", "GB", "GBR", "826", Instant.now(), null, false);
        assertEquals("GB", c.alpha2Code());
        assertEquals("GBR", c.alpha3Code());
        assertEquals("826", c.numericCode());
        assertFalse(c.isDeleted());
    }

    @Test
    void rejectsInvalidAlpha2() {
        assertThrows(IllegalArgumentException.class, () ->
            Country.of("Foo", "G", "ABC", "123", Instant.now(), null, false));
        assertThrows(IllegalArgumentException.class, () ->
            Country.of("Foo", "GB1", "ABC", "123", Instant.now(), null, false));
    }

    @Test
    void rejectsInvalidAlpha3() {
        assertThrows(IllegalArgumentException.class, () ->
            Country.of("Foo", "GB", "B", "123", Instant.now(), null, false));
        assertThrows(IllegalArgumentException.class, () ->
            Country.of("Foo", "GB", "BBBB", "123", Instant.now(), null, false));
    }

    @Test
    void rejectsInvalidNumeric() {
        assertThrows(IllegalArgumentException.class, () ->
            Country.of("Foo", "GB", "ABC", "12", Instant.now(), null, false));
        assertThrows(IllegalArgumentException.class, () ->
            Country.of("Foo", "GB", "ABC", "ABC", Instant.now(), null, false));
    }

    @Test
    void rejectsNullInputs() {
        assertThrows(NullPointerException.class, () ->
            Country.of(null, "GB", "GBR", "826", Instant.now(), null, false));
        assertThrows(NullPointerException.class, () ->
            Country.of("UK", null, "GBR", "826", Instant.now(), null, false));
        assertThrows(NullPointerException.class, () ->
            Country.of("UK", "GB", null, "826", Instant.now(), null, false));
        assertThrows(NullPointerException.class, () ->
            Country.of("UK", "GB", "GBR", null, Instant.now(), null, false));
        assertThrows(NullPointerException.class, () ->
            Country.of("UK", "GB", "GBR", "826", null, null, false));
    }
}
