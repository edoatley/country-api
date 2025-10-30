package com.example.country.application.model;

public record CountryInput(
        String name,
        String alpha2Code,
        String alpha3Code,
        String numericCode
) {}
