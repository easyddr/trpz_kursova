package com.example;

import java.util.List;
import java.util.Objects;

public abstract class AbstractSyntaxParser {

    protected record Token(TokenStyleFactory.TokenType type, int start, int end) {
    }

    private final TokenStyleFactory styleFactory;

    protected AbstractSyntaxParser(TokenStyleFactory styleFactory) {
        this.styleFactory = Objects.requireNonNull(styleFactory, "styleFactory");
    }

    public final String parse(String text) {
        StringBuilder out = new StringBuilder();
        int last = 0;
        for (Token token : tokenize(text)) {
            out.append(text, last, token.start());
            out.append(styleFactory.getStyle(token.type()).wrap(text.substring(token.start(), token.end())));
            last = token.end();
        }
        out.append(text, last, text.length());
        return out.toString();
    }

    protected abstract List<Token> tokenize(String text);
}

