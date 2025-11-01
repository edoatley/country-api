package com.example.country.adapters.lambda;

import com.example.country.adapters.api.CountryApi;
import com.example.country.application.model.CountryInput;
import com.example.country.domain.Country;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CountryLambdaHandler {
    private final CountryApi api;

    public CountryLambdaHandler(CountryApi api) {
        this.api = Objects.requireNonNull(api);
    }

    public Object handleRequest(String action, Map<String, String> pathParams, Map<String, String> queryParams, CountryInput body) {
        return switch (action) {
            case "GET_ALL" -> api.listCountries(parseInt(queryParams.getOrDefault("limit", "20")), parseInt(queryParams.getOrDefault("offset", "0")));
            case "CREATE" -> api.createCountry(body);
            case "GET_ALPHA2" -> api.getByAlpha2(pathParams.get("alpha2Code"));
            case "UPDATE_ALPHA2" -> api.updateByAlpha2(pathParams.get("alpha2Code"), body);
            case "DELETE_ALPHA2" -> {
                api.deleteByAlpha2(pathParams.get("alpha2Code"));
                yield null;
            }
            case "GET_ALPHA3" -> api.getByAlpha3(pathParams.get("alpha3Code"));
            case "GET_NUMERIC" -> api.getByNumeric(pathParams.get("numericCode"));
            case "HISTORY_ALPHA2" -> api.historyByAlpha2(pathParams.get("alpha2Code"));
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        };
    }

    private int parseInt(String s) {
        return Integer.parseInt(s);
    }
}
