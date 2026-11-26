package com.example;

import java.util.HashMap;
import java.util.Map;

public class TokenStyleFactory {

    public enum TokenType { KEYWORD, COMMENT, STRING, NUMBER }

    public record TokenStyle(String color, boolean bold, boolean italic) {

        public String wrap(String lexeme) {
            int rgb = Integer.parseInt(color.substring(1), 16);
            return (bold ? "\u001B[1m" : "")
                    + (italic ? "\u001B[3m" : "")
                    + String.format("\u001B[38;2;%d;%d;%dm", (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF)
                    + lexeme + "\u001B[0m";
        }
    }

    private final Map<String, TokenStyle> cache = new HashMap<>();

    public TokenStyle getStyle(TokenType type) {
        return switch (type) {
            case KEYWORD -> getStyle("#CC7832", true, false);
            case COMMENT -> getStyle("#808080", false, true);
            case STRING -> getStyle("#6A8759", false, false);
            case NUMBER -> getStyle("#6897BB", false, false);
        };
    }

    public TokenStyle getStyle(String color, boolean bold, boolean italic) {
        return cache.computeIfAbsent(color + '|' + bold + '|' + italic,
                key -> new TokenStyle(color, bold, italic));
    }

    public int size() {
        return cache.size();
    }
}
