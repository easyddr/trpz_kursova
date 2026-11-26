package com.example;

import java.util.Objects;

public class InsertTextCommand implements Command {
    private final TextDocument document;
    private final int position;
    private final String text;

    public InsertTextCommand(TextDocument document, int position, String text) {
        this.document = Objects.requireNonNull(document, "document");
        this.position = position;
        this.text = Objects.requireNonNull(text, "text");
    }

    @Override
    public void execute() {
        document.insert(position, text);
    }

    @Override
    public void undo() {
        document.delete(position, position + text.length());
    }

    public int getPosition() {
        return position;
    }

    public String getText() {
        return text;
    }
}
