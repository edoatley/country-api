package com.example.country.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.country.adapters", "com.example.country.bootstrap"})
public class CountryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CountryServiceApplication.class, args);
    }
}
