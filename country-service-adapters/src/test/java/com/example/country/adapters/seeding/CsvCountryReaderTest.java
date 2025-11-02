package com.example.country.adapters.seeding;

import com.example.country.domain.Country;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvCountryReaderTest {
    
    @Test
    void canReadCountriesFromCsv() throws Exception {
        String csv = "iso2,iso3,iso_num,country,country_common\n" +
                "GB,GBR,826,United Kingdom of Great Britain and Northern Ireland (the),United Kingdom\n" +
                "US,USA,840,United States of America (the),United States of America";
        
        CsvCountryReader reader = new CsvCountryReader();
        List<Country> countries = reader.readCountries(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        );
        
        assertEquals(2, countries.size());
        assertEquals("GB", countries.get(0).alpha2Code());
        assertEquals("GBR", countries.get(0).alpha3Code());
        assertEquals("826", countries.get(0).numericCode());
        assertEquals("United Kingdom of Great Britain and Northern Ireland (the)", countries.get(0).name());
        
        assertEquals("US", countries.get(1).alpha2Code());
        assertEquals("USA", countries.get(1).alpha3Code());
        assertEquals("840", countries.get(1).numericCode());
    }
    
    @Test
    void padsNumericCodeToThreeDigits() throws Exception {
        String csv = "iso2,iso3,iso_num,country,country_common\n" +
                "AX,ALA,248,Aland Islands,Aland Islands";
        
        CsvCountryReader reader = new CsvCountryReader();
        List<Country> countries = reader.readCountries(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))
        );
        
        assertEquals(1, countries.size());
        assertEquals("248", countries.get(0).numericCode());
    }
}
