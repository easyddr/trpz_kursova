package com.example;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Objects;

public class EncodingService {

    private final List<EncodingStrategy> strategies;

    public EncodingService() {
        this(List.of(new Utf16Strategy(true), new Utf16Strategy(false),
                new Utf8Strategy(), new Win1251Strategy(), new Iso88591Strategy()));
    }

    public EncodingService(List<EncodingStrategy> strategies) {
        this.strategies = List.copyOf(Objects.requireNonNull(strategies, "strategies"));
    }

    public String detectEncoding(byte[] bytes) throws UnsupportedEncodingException {
        if (startsWith(bytes, 0xEF, 0xBB, 0xBF)) {
            return "UTF-8";
        }
        if (startsWith(bytes, 0xFF, 0xFE)) {
            return "UTF-16LE";
        }
        if (startsWith(bytes, 0xFE, 0xFF)) {
            return "UTF-16BE";
        }
        boolean binaryChecked = false;
        for (EncodingStrategy strategy : strategies) {
            if (!strategy.name().startsWith("UTF-16") && !binaryChecked) {
                if (looksBinary(bytes)) {
                    throw new UnsupportedEncodingException("File looks binary and cannot be decoded as text");
                }
                binaryChecked = true;
            }
            if (strategy.supports(bytes)) {
                return strategy.name();
            }
        }
        throw new UnsupportedEncodingException("Unable to detect text encoding");
    }

    private static boolean looksBinary(byte[] bytes) {
        for (byte value : bytes) {
            int b = value & 0xFF;
            if (b < 0x20 && b != '\t' && b != '\n' && b != '\r' && b != '\f') {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWith(byte[] bytes, int... prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((bytes[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}

