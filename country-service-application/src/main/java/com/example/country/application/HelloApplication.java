package com.example.country.application;

import com.example.country.domain.HelloDomain;

public class HelloApplication {
    public static String msg() { return HelloDomain.msg() + " | Hello from Application"; }
}
