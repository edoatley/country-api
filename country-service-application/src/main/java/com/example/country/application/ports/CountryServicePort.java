package com.example.country.application.ports;

import com.example.country.application.model.CountryInput;
import com.example.country.domain.Country;

import java.util.List;

public interface CountryServicePort {
    Country create(CountryInput input);
    Country getByAlpha2(String alpha2Code);
    Country getByAlpha3(String alpha3Code);
    Country getByNumeric(String numericCode);
    List<Country> listAll(int limit, int offset);
    Country updateByAlpha2(String alpha2Code, CountryInput input);
    void deleteByAlpha2(String alpha2Code);
    List<Country> historyByAlpha2(String alpha2Code);
}
