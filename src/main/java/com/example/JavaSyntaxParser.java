package com.example;


import com.example.TokenStyleFactory.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JavaSyntaxParser extends AbstractSyntaxParser {

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null", "record", "var");

    public JavaSyntaxParser() {
        this(new TokenStyleFactory());
    }

    public JavaSyntaxParser(TokenStyleFactory styleFactory) {
        super(styleFactory);
    }

    @Override
    protected List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        int n = text.length();
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);
            if (text.startsWith("//", i)) {
                int end = text.indexOf('\n', i);
                end = end < 0 ? n : end;
                tokens.add(new Token(TokenType.COMMENT, i, end));
                i = end;
            } else if (text.startsWith("/*", i)) {
                int end = text.indexOf("*/", i + 2);
                end = end < 0 ? n : end + 2;
                tokens.add(new Token(TokenType.COMMENT, i, end));
                i = end;
            } else if (text.startsWith("\"\"\"", i)) {
                int end = text.indexOf("\"\"\"", i + 3);
                end = end < 0 ? n : end + 3;
                tokens.add(new Token(TokenType.STRING, i, end));
                i = end;
            } else if (c == '"' || c == '\'') {
                int end = scanQuoted(text, i, c);
                tokens.add(new Token(TokenType.STRING, i, end));
                i = end;
            } else if (Character.isDigit(c)) {
                int end = i + 1;
                while (end < n && (Character.isLetterOrDigit(text.charAt(end))
                        || text.charAt(end) == '.' || text.charAt(end) == '_')) {
                    end++;
                }
                tokens.add(new Token(TokenType.NUMBER, i, end));
                i = end;
            } else if (Character.isJavaIdentifierStart(c)) {
                int end = i + 1;
                while (end < n && Character.isJavaIdentifierPart(text.charAt(end))) {
                    end++;
                }
                if (KEYWORDS.contains(text.substring(i, end))) {
                    tokens.add(new Token(TokenType.KEYWORD, i, end));
                }
                i = end;
            } else {
                i++;
            }
        }
        return tokens;
    }

    private static int scanQuoted(String text, int start, char quote) {
        int n = text.length();
        int j = start + 1;
        while (j < n) {
            char ch = text.charAt(j);
            if (ch == '\\') {
                j += 2;
                continue;
            }
            if (ch == quote) {
                j++;
                break;
            }
            if (ch == '\n') {
                break;
            }
            j++;
        }
        return Math.min(j, n);
    }
}
