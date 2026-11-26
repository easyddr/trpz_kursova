package com.example;

public class Win1251Strategy implements EncodingStrategy {

    @Override
    public String name() {
        return "windows-1251";
    }

    @Override
    public boolean supports(byte[] bytes) {
        for (byte b : bytes) {
            if ((b & 0xFF) == 0x98) {
                return false;
            }
        }
        return true;
    }
}
