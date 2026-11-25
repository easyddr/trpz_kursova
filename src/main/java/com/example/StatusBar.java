package com.example;

public class StatusBar implements DocumentObserver {

    private String statusText = "";

    @Override
    public void update(TextDocument document) {
        statusText = String.format("%s | %s | Ln %d | %d chars",
                document.getFileName(), document.getEncoding(), document.lineCount(), document.length());
    }

    public String getStatusText() {
        return statusText;
    }
}
