package com.example;

import java.util.Objects;

public class SyntaxHighlightService {

    private final AbstractSyntaxParser parser;

    public SyntaxHighlightService(AbstractSyntaxParser parser) {
        this.parser = Objects.requireNonNull(parser, "parser");
    }

    public String highlight(String text) {
        return parser.parse(text);
    }
}
