package com.example.country.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable Country record with versioning and audit fields for country reference service.
 */
public final class Country {
    private static final Pattern ALPHA2 = Pattern.compile("^[A-Z]{2}$");
    private static final Pattern ALPHA3 = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern NUMERIC = Pattern.compile("^[0-9]{3}$");

    private final String name;
    private final String alpha2Code;
    private final String alpha3Code;
    private final String numericCode;
    private final Instant createDate;
    private final Instant expiryDate; // can be null if current
    private final boolean isDeleted;

    private Country(String name, String alpha2Code, String alpha3Code, String numericCode,
                    Instant createDate, Instant expiryDate, boolean isDeleted) {
        this.name = name;
        this.alpha2Code = alpha2Code;
        this.alpha3Code = alpha3Code;
        this.numericCode = numericCode;
        this.createDate = createDate;
        this.expiryDate = expiryDate;
        this.isDeleted = isDeleted;
    }

    public static Country of(String name, String alpha2Code, String alpha3Code, String numericCode,
                             Instant createDate, Instant expiryDate, boolean isDeleted) {
        Objects.requireNonNull(name, "name is required");
        Objects.requireNonNull(alpha2Code, "alpha2Code is required");
        Objects.requireNonNull(alpha3Code, "alpha3Code is required");
        Objects.requireNonNull(numericCode, "numericCode is required");
        Objects.requireNonNull(createDate, "createDate is required");
        if (!ALPHA2.matcher(alpha2Code).matches())
            throw new IllegalArgumentException("Invalid alpha2Code, expected [A-Z]{2}");
        if (!ALPHA3.matcher(alpha3Code).matches())
            throw new IllegalArgumentException("Invalid alpha3Code, expected [A-Z]{3}");
        if (!NUMERIC.matcher(numericCode).matches())
            throw new IllegalArgumentException("Invalid numericCode, expected [0-9]{3}");
        return new Country(name, alpha2Code, alpha3Code, numericCode, createDate, expiryDate, isDeleted);
    }

    public String name() { return name; }
    public String alpha2Code() { return alpha2Code; }
    public String alpha3Code() { return alpha3Code; }
    public String numericCode() { return numericCode; }
    public Instant createDate() { return createDate; }
    public Instant expiryDate() { return expiryDate; }
    public boolean isDeleted() { return isDeleted; }
}
