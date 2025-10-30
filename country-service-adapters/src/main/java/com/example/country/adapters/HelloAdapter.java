package com.example.country.adapters;

import com.example.country.application.HelloApplication;

public class HelloAdapter {
    public static String msg() { return HelloApplication.msg() + " | Hello from Adapter"; }
}
