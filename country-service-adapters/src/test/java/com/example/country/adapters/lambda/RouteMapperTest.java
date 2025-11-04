package com.example.country.adapters.lambda;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RouteMapperTest {
    private final RouteMapper mapper = new RouteMapper();

    @Test
    void mapsGetAllCountries() {
        RouteMapping mapping = mapper.map("GET", "/api/v1/countries");
        
        assertNotNull(mapping);
        assertEquals("GET_ALL", mapping.getAction());
        assertTrue(mapping.getPathParams().isEmpty());
    }

    @Test
    void mapsGetByAlpha2() {
        RouteMapping mapping = mapper.map("GET", "/api/v1/countries/code/GB");
        
        assertNotNull(mapping);
        assertEquals("GET_ALPHA2", mapping.getAction());
        assertEquals("GB", mapping.getPathParams().get("alpha2Code"));
    }

    @Test
    void mapsGetByAlpha3() {
        RouteMapping mapping = mapper.map("GET", "/api/v1/countries/code3/GBR");
        
        assertNotNull(mapping);
        assertEquals("GET_ALPHA3", mapping.getAction());
        assertEquals("GBR", mapping.getPathParams().get("alpha3Code"));
    }

    @Test
    void mapsGetByNumeric() {
        RouteMapping mapping = mapper.map("GET", "/api/v1/countries/number/826");
        
        assertNotNull(mapping);
        assertEquals("GET_NUMERIC", mapping.getAction());
        assertEquals("826", mapping.getPathParams().get("numericCode"));
    }

    @Test
    void mapsGetHistory() {
        RouteMapping mapping = mapper.map("GET", "/api/v1/countries/code/GB/history");
        
        assertNotNull(mapping);
        assertEquals("HISTORY_ALPHA2", mapping.getAction());
        assertEquals("GB", mapping.getPathParams().get("alpha2Code"));
    }

    @Test
    void mapsPostCreate() {
        RouteMapping mapping = mapper.map("POST", "/api/v1/countries");
        
        assertNotNull(mapping);
        assertEquals("CREATE", mapping.getAction());
        assertTrue(mapping.getPathParams().isEmpty());
    }

    @Test
    void mapsPutUpdate() {
        RouteMapping mapping = mapper.map("PUT", "/api/v1/countries/code/GB");
        
        assertNotNull(mapping);
        assertEquals("UPDATE_ALPHA2", mapping.getAction());
        assertEquals("GB", mapping.getPathParams().get("alpha2Code"));
    }

    @Test
    void mapsDelete() {
        RouteMapping mapping = mapper.map("DELETE", "/api/v1/countries/code/GB");
        
        assertNotNull(mapping);
        assertEquals("DELETE_ALPHA2", mapping.getAction());
        assertEquals("GB", mapping.getPathParams().get("alpha2Code"));
    }

    @Test
    void returnsNullForUnknownRoute() {
        RouteMapping mapping = mapper.map("GET", "/api/v1/unknown");
        
        assertNull(mapping);
    }

    @Test
    void returnsNullForUnknownMethod() {
        RouteMapping mapping = mapper.map("PATCH", "/api/v1/countries");
        
        assertNull(mapping);
    }

    @Test
    void handlesPathWithoutLeadingSlash() {
        RouteMapping mapping = mapper.map("GET", "api/v1/countries");
        
        assertNotNull(mapping);
        assertEquals("GET_ALL", mapping.getAction());
    }

    @Test
    void rejectsNullHttpMethod() {
        assertThrows(NullPointerException.class, () -> mapper.map(null, "/api/v1/countries"));
    }

    @Test
    void rejectsNullPath() {
        assertThrows(NullPointerException.class, () -> mapper.map("GET", null));
    }
}
