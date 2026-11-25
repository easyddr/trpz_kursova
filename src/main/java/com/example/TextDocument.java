package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TextDocument {

    private final String fileName;
    private String encoding;
    private final StringBuilder buffer;
    private final List<DocumentObserver> observers = new ArrayList<>();

    public TextDocument(String fileName, String encoding, String text) {
        this.fileName = Objects.requireNonNull(fileName, "fileName");
        this.encoding = Objects.requireNonNull(encoding, "encoding");
        this.buffer = new StringBuilder(Objects.requireNonNull(text, "text"));
    }

    public String getFileName() {
        return fileName;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = Objects.requireNonNull(encoding, "encoding");
    }

    public String getText() {
        return buffer.toString();
    }

    public int length() {
        return buffer.length();
    }

    public int lineCount() {
        int count = 1;
        for (int i = 0; i < buffer.length(); i++) {
            if (buffer.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    public String getLines(int fromLine, int toLine) {
        int total = lineCount();
        if (fromLine < 1 || toLine > total || fromLine > toLine) {
            throw new IndexOutOfBoundsException("Lines " + fromLine + ".." + toLine + " out of 1.." + total);
        }
        String[] lines = getText().split("\n", -1);
        return String.join("\n", List.of(lines).subList(fromLine - 1, toLine));
    }

    public void insert(int position, String text) {
        Objects.requireNonNull(text, "text");
        if (position < 0 || position > buffer.length()) {
            throw new IndexOutOfBoundsException("Position " + position + " out of 0.." + buffer.length());
        }
        buffer.insert(position, text);
        notifyObservers();
    }

    public String delete(int start, int end) {
        if (start < 0 || end > buffer.length() || start > end) {
            throw new IndexOutOfBoundsException("Range " + start + ".." + end + " out of 0.." + buffer.length());
        }
        String removed = buffer.substring(start, end);
        buffer.delete(start, end);
        notifyObservers();
        return removed;
    }

    public void addObserver(DocumentObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    public void notifyObservers() {
        for (DocumentObserver observer : new ArrayList<>(observers)) {
            observer.update(this);
        }
    }
}
