package com.example.country.adapters.seeding;

import com.example.country.domain.Country;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CsvCountryReader {
    
    public List<Country> readCountries(InputStream csvStream) throws Exception {
        List<Country> countries = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            // Skip header line
            String line = reader.readLine();
            if (line == null || !line.startsWith("iso2")) {
                throw new IllegalArgumentException("Invalid CSV format: missing header");
            }
            
            Instant now = Instant.now();
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] parts = parseCsvLine(line);
                if (parts.length < 5) continue;
                
                String alpha2 = parts[0].trim();
                String alpha3 = parts[1].trim();
                String numericStr = parts[2].trim();
                String countryName = parts[3].trim();
                
                // Pad numeric code to 3 digits
                String numericCode = String.format("%03d", Integer.parseInt(numericStr));
                
                Country country = Country.of(
                        countryName,
                        alpha2,
                        alpha3,
                        numericCode,
                        now,
                        null,
                        false
                );
                countries.add(country);
            }
        }
        
        return countries;
    }
    
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        
        return fields.toArray(new String[0]);
    }
}
