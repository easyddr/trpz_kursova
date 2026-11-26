package com.example;

public class Iso88591Strategy implements EncodingStrategy {
    @Override
    public String name() {
        return "ISO-8859-1";
    }

    @Override
    public boolean supports(byte[] bytes) {
        return true;
    }
}
