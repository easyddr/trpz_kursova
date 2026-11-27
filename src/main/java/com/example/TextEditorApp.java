package com.example;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TextEditorApp implements AutoCloseable {

    private final MySQLConnection connection;
    private final EncodingService encodingService = new EncodingService();
    private final SyntaxHighlightService highlightService = new SyntaxHighlightService(new JavaSyntaxParser());
    private final MacroRepository macroRepository;
    private final SnippetRepository snippetRepository;

    private TextDocument document;
    private CommandManager commandManager = new CommandManager();
    private StatusBar statusBar;
    private BookmarkManager bookmarkManager;

    public TextEditorApp(String url, String user, String password) {
        this.connection = new MySQLConnection(url, user, password);
        this.macroRepository = new MacroRepository(connection);
        this.snippetRepository = new SnippetRepository(connection);
    }

    public TextDocument openFile(Path path) throws IOException {
        return openFile(path, null);
    }

    public TextDocument openFile(Path path, String forcedEncoding) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new FileNotFoundException("File not found: " + path);
        }
        byte[] bytes = Files.readAllBytes(path);
        String encoding = forcedEncoding == null ? encodingService.detectEncoding(bytes) : forcedEncoding;
        Charset charset;
        try {
            charset = Charset.forName(encoding);
        } catch (IllegalArgumentException e) {
            throw new UnsupportedEncodingException("Unknown encoding: " + encoding);
        }
        String text;
        try {
            text = charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new UnsupportedEncodingException("Cannot decode file as " + encoding);
        }
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        return attach(new TextDocument(path.getFileName().toString(), encoding, text));
    }

    public TextDocument newDocument(String fileName) {
        return attach(new TextDocument(fileName, "UTF-8", ""));
    }

    public void saveToFile(Path path, String encoding) throws IOException {
        requireDocument();
        String target = encoding == null ? document.getEncoding() : encoding;
        Charset charset;
        try {
            charset = Charset.forName(target);
        } catch (IllegalArgumentException e) {
            throw new UnsupportedEncodingException("Unknown encoding: " + target);
        }
        ByteBuffer encoded;
        try {
            encoded = charset.newEncoder().encode(CharBuffer.wrap(document.getText()));
        } catch (CharacterCodingException e) {
            throw new UnsupportedEncodingException("Text cannot be represented in " + target);
        }
        byte[] data = new byte[encoded.remaining()];
        encoded.get(data);
        Files.write(path, data);
        document.setEncoding(target);
        document.notifyObservers();
    }

    private TextDocument attach(TextDocument newDocument) {
        document = newDocument;
        commandManager = new CommandManager();
        statusBar = new StatusBar();
        bookmarkManager = new BookmarkManager(newDocument);
        newDocument.addObserver(statusBar);
        newDocument.addObserver(bookmarkManager);
        newDocument.notifyObservers();
        return newDocument;
    }

    public String highlight() {
        requireDocument();
        return highlightService.highlight(document.getText());
    }

    public synchronized String highlight(String text) {
        return highlightService.highlight(text);
    }

    public void insertText(int position, String text) {
        requireDocument();
        commandManager.execute(new InsertTextCommand(document, position, text));
    }

    public void deleteText(int start, int end) {
        requireDocument();
        commandManager.execute(new DeleteTextCommand(document, start, end));
    }

    public boolean undo() {
        requireDocument();
        return commandManager.undo();
    }

    public boolean redo() {
        requireDocument();
        return commandManager.redo();
    }

    public List<String> suggest(String prefix) throws SQLException {
        return prefix.isEmpty() ? List.of() : snippetRepository.findShortcuts(prefix);
    }

    public boolean expandSnippet(int position) throws SQLException {
        requireDocument();
        String text = document.getText();
        if (position < 0 || position > text.length()) {
            throw new IndexOutOfBoundsException("Position " + position + " out of 0.." + text.length());
        }
        int start = position;
        while (start > 0 && (Character.isLetterOrDigit(text.charAt(start - 1)) || text.charAt(start - 1) == '_')) {
            start--;
        }
        if (start == position) {
            return false;
        }
        String shortcut = text.substring(start, position);
        Optional<String> template = snippetRepository.findByShortcut(shortcut);
        if (template.isEmpty()) {
            return false;
        }
        commandManager.execute(new MacroCommand("snippet:" + shortcut, List.of(
                new DeleteTextCommand(document, start, position),
                new InsertTextCommand(document, start, template.get()))));
        return true;
    }

    public void startMacroRecording() {
        requireDocument();
        commandManager.startRecording();
    }

    public MacroCommand stopMacroRecording(String name) throws SQLException {
        MacroCommand macro = commandManager.stopRecording(name);
        macroRepository.save(macro);
        return macro;
    }

    public boolean runMacro(String name) throws SQLException {
        requireDocument();
        Optional<MacroCommand> macro = macroRepository.load(name, document);
        if (macro.isEmpty()) {
            return false;
        }
        commandManager.execute(macro.get());
        return true;
    }

    public List<String> getMacroNames() throws SQLException {
        return macroRepository.findAllNames();
    }

    public void addBookmark(int line, String label) {
        requireDocument();
        bookmarkManager.addBookmark(line, label);
    }

    public boolean removeBookmark(int line) {
        requireDocument();
        return bookmarkManager.removeBookmark(line);
    }

    public Map<Integer, String> getBookmarks() {
        requireDocument();
        return bookmarkManager.getBookmarks();
    }

    public int goToLine(int line) {
        requireDocument();
        return bookmarkManager.goToLine(line);
    }

    public int goToPage(int page) {
        requireDocument();
        return bookmarkManager.goToPage(page);
    }

    public int goToBookmark(int line) {
        requireDocument();
        return bookmarkManager.goToBookmark(line);
    }

    public int getCursorLine() {
        requireDocument();
        return bookmarkManager.getCursorLine();
    }

    public String getCurrentPageText() {
        requireDocument();
        return bookmarkManager.getCurrentPageText();
    }

    public TextDocument getDocument() {
        requireDocument();
        return document;
    }

    public String getStatusText() {
        requireDocument();
        return statusBar.getStatusText();
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    private void requireDocument() {
        if (document == null) {
            throw new IllegalStateException("No document is open");
        }
    }
}
