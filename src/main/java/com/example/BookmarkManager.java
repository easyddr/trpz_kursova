package com.example;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class BookmarkManager implements DocumentObserver {

    public static final int DEFAULT_PAGE_SIZE = 20;

    private final TextDocument document;
    private final int pageSize;
    private final Map<Integer, String> bookmarks = new TreeMap<>();
    private int cursorLine = 1;

    public BookmarkManager(TextDocument document) {
        this(document, DEFAULT_PAGE_SIZE);
    }

    public BookmarkManager(TextDocument document, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be >= 1: " + pageSize);
        }
        this.document = Objects.requireNonNull(document, "document");
        this.pageSize = pageSize;
    }

    public void addBookmark(int line, String label) {
        checkLine(line);
        bookmarks.put(line, Objects.requireNonNull(label, "label"));
    }

    public boolean removeBookmark(int line) {
        return bookmarks.remove(line) != null;
    }

    public Map<Integer, String> getBookmarks() {
        return Collections.unmodifiableMap(new TreeMap<>(bookmarks));
    }

    public int goToLine(int line) {
        checkLine(line);
        cursorLine = line;
        return cursorLine;
    }

    public int goToPage(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be >= 1: " + page);
        }
        long target = (long) (page - 1) * pageSize + 1;
        if (target > document.lineCount()) {
            throw new IllegalArgumentException("Page " + page + " is beyond the document (" + pageCount() + " pages)");
        }
        return goToLine((int) target);
    }

    public int goToBookmark(int line) {
        if (!bookmarks.containsKey(line)) {
            throw new IllegalArgumentException("No bookmark on line " + line);
        }
        return goToLine(line);
    }

    public int getCursorLine() {
        return cursorLine;
    }

    public int pageCount() {
        return (document.lineCount() + pageSize - 1) / pageSize;
    }

    public String getCurrentPageText() {
        int page = (cursorLine - 1) / pageSize + 1;
        int from = (page - 1) * pageSize + 1;
        int to = Math.min(document.lineCount(), page * pageSize);
        return document.getLines(from, to);
    }

    @Override
    public void update(TextDocument changed) {
        int lines = changed.lineCount();
        bookmarks.keySet().removeIf(line -> line > lines);
        if (cursorLine > lines) {
            cursorLine = lines;
        }
    }

    private void checkLine(int line) {
        int total = document.lineCount();
        if (line < 1 || line > total) {
            throw new IllegalArgumentException("Line " + line + " is out of range 1.." + total);
        }
    }
}

