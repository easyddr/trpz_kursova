package com.example;

public interface EncodingStrategy {
    String name();

    boolean supports(byte[] bytes);
}
