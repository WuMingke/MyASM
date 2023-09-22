package com.example.myasm.realasm;

import androidx.annotation.NonNull;

public class SensorsData {
    private String address;
    private static final int age = 10;

    @NonNull
    @Override
    public String toString() {
        return "SensorsData => address-" + address + ",age-" + age;
    }
}
