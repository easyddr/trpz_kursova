package com.example;

import java.util.Objects;

public class DeleteTextCommand implements Command {

    private final TextDocument document;
    private final int start;
    private final int end;
    private String deleted;

    public DeleteTextCommand(TextDocument document, int start, int end) {
        this.document = Objects.requireNonNull(document, "document");
        this.start = start;
        this.end = end;
    }

    @Override
    public void execute() {
        deleted = document.delete(start, end);
    }

    @Override
    public void undo() {
        if (deleted == null) {
            throw new IllegalStateException("Command has not been executed");
        }
        document.insert(start, deleted);
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }
}
