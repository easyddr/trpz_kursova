package com.example;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EditorWindow extends JFrame {

    private final TextEditorApp app;
    private final JTextPane textArea = new JTextPane();
    private final Timer highlightTimer = new Timer(200, e -> scheduleHighlight());
    private final JLabel status = new JLabel("Новий документ");
    private final JMenuItem startMacroItem = new JMenuItem("Почати запис");
    private final JMenuItem stopMacroItem = new JMenuItem("Зупинити запис");

    private static final String[] ENCODINGS = {"UTF-8", "UTF-16LE", "UTF-16BE", "windows-1251", "ISO-8859-1"};

    private Path currentFile;
    private SwingWorker<String, Void> highlightWorker;
    private String previousText = "";
    private boolean updating;
    private boolean recording;

    public EditorWindow() throws SQLException {
        String url = System.getenv().getOrDefault(
                "DB_URL", MySQLConnection.DEFAULT_URL);
        String user = System.getenv().getOrDefault("DB_USER", "root");
        String password = System.getenv().getOrDefault("DB_PASSWORD", "");

        app = new TextEditorApp(url, user, password);
        app.newDocument("Untitled.txt");

        setTitle("Text Editor");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        highlightTimer.setRepeats(false);

        add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        createMenu();
        connectEditor();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                closeEditor();
            }
        });

        updateStatus();
    }

    private static KeyStroke ctrl(int keyCode) {
        return KeyStroke.getKeyStroke(
                keyCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    private void createMenu() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Файл");
        JMenu editMenu = new JMenu("Редагування");
        JMenu navigationMenu = new JMenu("Навігація");
        JMenu bookmarksMenu = new JMenu("Закладки");
        JMenu snippetsMenu = new JMenu("Сніппети");
        JMenu macrosMenu = new JMenu("Макроси");

        JMenuItem newItem = new JMenuItem("Новий");
        JMenuItem openItem = new JMenuItem("Відкрити");
        JMenuItem saveItem = new JMenuItem("Зберегти");
        JMenuItem saveAsItem = new JMenuItem("Зберегти як");

        newItem.addActionListener(e -> newDocument());
        openItem.addActionListener(e -> openFile());
        saveItem.addActionListener(e -> saveFile(false));
        saveAsItem.addActionListener(e -> saveFile(true));

        JMenu encodingMenu = new JMenu("Кодування");
        JMenu reopenMenu = new JMenu("Перечитати файл у кодуванні");
        JMenu saveEncodingMenu = new JMenu("Зберегти у кодуванні");
        for (String encoding : ENCODINGS) {
            JMenuItem reopenItem = new JMenuItem(encoding);
            reopenItem.addActionListener(e -> reopenWithEncoding(encoding));
            reopenMenu.add(reopenItem);
            JMenuItem saveEncodingItem = new JMenuItem(encoding);
            saveEncodingItem.addActionListener(e -> saveWithEncoding(encoding));
            saveEncodingMenu.add(saveEncodingItem);
        }
        encodingMenu.add(reopenMenu);
        encodingMenu.add(saveEncodingMenu);

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);

        JMenuItem undoItem = new JMenuItem("Скасувати");
        JMenuItem redoItem = new JMenuItem("Повторити");

        undoItem.setAccelerator(ctrl(KeyEvent.VK_Z));
        redoItem.setAccelerator(ctrl(KeyEvent.VK_Y));
        undoItem.addActionListener(e -> undo());
        redoItem.addActionListener(e -> redo());

        editMenu.add(undoItem);
        editMenu.add(redoItem);

        JMenuItem goLineItem = new JMenuItem("Перейти до рядка");
        JMenuItem goPageItem = new JMenuItem("Перейти на сторінку");
        goLineItem.setAccelerator(ctrl(KeyEvent.VK_G));
        goLineItem.addActionListener(e -> goToLine());
        goPageItem.addActionListener(e -> goToPage());

        navigationMenu.add(goLineItem);
        navigationMenu.add(goPageItem);

        JMenuItem addBookmarkItem = new JMenuItem("Додати закладку на поточному рядку");
        JMenuItem removeBookmarkItem = new JMenuItem("Видалити закладку з поточного рядка");
        JMenuItem listBookmarksItem = new JMenuItem("Список закладок");
        addBookmarkItem.setAccelerator(ctrl(KeyEvent.VK_B));
        addBookmarkItem.addActionListener(e -> addBookmark());
        removeBookmarkItem.addActionListener(e -> removeBookmark());
        listBookmarksItem.addActionListener(e -> showBookmarks());

        bookmarksMenu.add(addBookmarkItem);
        bookmarksMenu.add(removeBookmarkItem);
        bookmarksMenu.add(listBookmarksItem);

        JMenuItem expandItem = new JMenuItem("Розгорнути сніппет");
        JMenuItem suggestItem = new JMenuItem("Підказки");
        expandItem.setAccelerator(ctrl(KeyEvent.VK_J));
        suggestItem.setAccelerator(ctrl(KeyEvent.VK_K));
        expandItem.addActionListener(e -> expandSnippet());
        suggestItem.addActionListener(e -> suggest());

        snippetsMenu.add(expandItem);
        snippetsMenu.add(suggestItem);

        JMenuItem runMacroItem = new JMenuItem("Виконати макрос");
        startMacroItem.addActionListener(e -> startMacro());
        stopMacroItem.addActionListener(e -> stopMacro());
        runMacroItem.addActionListener(e -> runMacro());

        macrosMenu.add(startMacroItem);
        macrosMenu.add(stopMacroItem);
        macrosMenu.add(runMacroItem);
        updateMacroMenu();

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(encodingMenu);
        menuBar.add(navigationMenu);
        menuBar.add(bookmarksMenu);
        menuBar.add(snippetsMenu);
        menuBar.add(macrosMenu);

        setJMenuBar(menuBar);
    }

    private void connectEditor() {
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                syncDocument();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                syncDocument();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    private void syncDocument() {
        if (updating) {
            return;
        }

        String currentText = textArea.getText();

        int prefix = 0;
        while (prefix < previousText.length()
                && prefix < currentText.length()
                && previousText.charAt(prefix) == currentText.charAt(prefix)) {
            prefix++;
        }

        int suffix = 0;
        while (suffix < previousText.length() - prefix
                && suffix < currentText.length() - prefix
                && previousText.charAt(previousText.length() - 1 - suffix)
                == currentText.charAt(currentText.length() - 1 - suffix)) {
            suffix++;
        }

        try {
            int oldEnd = previousText.length() - suffix;
            int newEnd = currentText.length() - suffix;

            if (oldEnd > prefix) {
                app.deleteText(prefix, oldEnd);
            }

            if (newEnd > prefix) {
                app.insertText(prefix, currentText.substring(prefix, newEnd));
            }

            previousText = currentText;
            updateStatus();
            highlightTimer.restart();
        } catch (RuntimeException ex) {
            showError(ex);
            refreshFromApp();
        }
    }

    private void refreshFromApp() {
        updating = true;
        try {
            previousText = app.getDocument().getText();
            textArea.setText(previousText);
            updateStatus();
        } finally {
            updating = false;
        }
        highlightTimer.restart();
    }

    private void resetMacroRecording() {
        recording = false;
        updateMacroMenu();
    }

    private void updateMacroMenu() {
        startMacroItem.setEnabled(!recording);
        stopMacroItem.setEnabled(recording);
    }

    private void newDocument() {
        String name = JOptionPane.showInputDialog(
                this, "Назва нового файлу:", "Untitled.txt");

        if (name == null || name.isBlank()) {
            return;
        }

        app.newDocument(name);
        currentFile = null;
        resetMacroRecording();
        refreshFromApp();
        setTitle("Text Editor — " + name);
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        loadInBackground(chooser.getSelectedFile().toPath(), null);
    }

    private void reopenWithEncoding(String encoding) {
        if (currentFile == null) {
            JOptionPane.showMessageDialog(this, "Спочатку відкрийте файл.");
            return;
        }
        loadInBackground(currentFile, encoding);
    }

    private void loadInBackground(Path path, String encoding) {
        textArea.setEnabled(false);
        status.setText("Читання файлу...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                app.openFile(path, encoding);
                return null;
            }

            @Override
            protected void done() {
                textArea.setEnabled(true);
                try {
                    get();
                    currentFile = path;
                    resetMacroRecording();
                    refreshFromApp();
                    syncDocument();
                    setTitle("Text Editor — " + path.getFileName());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showMessage(cause instanceof java.io.UnsupportedEncodingException
                            ? "Не вдалося визначити кодування: " + cause.getMessage()
                            : String.valueOf(cause.getMessage()));
                    updateStatus();
                }
            }
        }.execute();
    }

    private void saveWithEncoding(String encoding) {
        if (currentFile == null) {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            currentFile = chooser.getSelectedFile().toPath();
        }
        try {
            app.saveToFile(currentFile, encoding);
            updateStatus();
            JOptionPane.showMessageDialog(this, "Файл збережено у кодуванні " + encoding + ".");
        } catch (IOException | RuntimeException ex) {
            showError(ex);
        }
    }

    private void scheduleHighlight() {
        if (highlightWorker != null) {
            highlightWorker.cancel(true);
        }
        String snapshot = textArea.getText();
        highlightWorker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return app.highlight(snapshot);
            }

            @Override
            protected void done() {
                if (isCancelled() || !snapshot.equals(textArea.getText())) {
                    return;
                }
                try {
                    applyAnsi(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException | RuntimeException ex) {
                    status.setText("Помилка підсвічування");
                }
            }
        };
        highlightWorker.execute();
    }

    private void applyAnsi(String ansi) {
        StyledDocument styled = textArea.getStyledDocument();
        AttributeSet plain = new SimpleAttributeSet();
        styled.setCharacterAttributes(0, styled.getLength(), plain, true);
        boolean bold = false;
        boolean italic = false;
        Color color = null;
        int offset = 0;
        int i = 0;
        while (i < ansi.length()) {
            char c = ansi.charAt(i);
            if (c == '\u001B' && i + 1 < ansi.length() && ansi.charAt(i + 1) == '[') {
                int end = ansi.indexOf('m', i);
                if (end < 0) {
                    break;
                }
                String[] params = ansi.substring(i + 2, end).split(";");
                if (params.length == 1 && params[0].equals("0")) {
                    bold = false;
                    italic = false;
                    color = null;
                } else if (params.length == 1 && params[0].equals("1")) {
                    bold = true;
                } else if (params.length == 1 && params[0].equals("3")) {
                    italic = true;
                } else if (params.length == 5 && params[0].equals("38") && params[1].equals("2")) {
                    color = new Color(Integer.parseInt(params[2]), Integer.parseInt(params[3]),
                            Integer.parseInt(params[4]));
                }
                i = end + 1;
                continue;
            }
            int runStart = i;
            while (i < ansi.length() && ansi.charAt(i) != '\u001B') {
                i++;
            }
            int length = i - runStart;
            if (color != null || bold || italic) {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                if (color != null) {
                    StyleConstants.setForeground(attrs, color);
                }
                StyleConstants.setBold(attrs, bold);
                StyleConstants.setItalic(attrs, italic);
                styled.setCharacterAttributes(offset, length, attrs, false);
            }
            offset += length;
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Помилка", JOptionPane.ERROR_MESSAGE);
    }

    private void saveFile(boolean saveAs) {
        if (saveAs || currentFile == null) {
            JFileChooser chooser = new JFileChooser();

            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            currentFile = chooser.getSelectedFile().toPath();
        }

        try {
            app.saveToFile(currentFile, null);
            setTitle("Text Editor — " + currentFile.getFileName());
            JOptionPane.showMessageDialog(this, "Файл збережено.");
        } catch (IOException | RuntimeException ex) {
            showError(ex);
        }
    }

    private void undo() {
        if (app.undo()) {
            refreshFromApp();
        }
    }

    private void redo() {
        if (app.redo()) {
            refreshFromApp();
        }
    }

    private int caretLine() {
        try {
            return textArea.getStyledDocument().getDefaultRootElement()
                    .getElementIndex(textArea.getCaretPosition()) + 1;
        } catch (RuntimeException ex) {
            return 1;
        }
    }

    private void moveCaretToLine(int line) {
        Element root = textArea.getStyledDocument().getDefaultRootElement();
        if (line < 1 || line > root.getElementCount()) {
            showError(new IllegalArgumentException("Рядок " + line + " поза діапазоном 1.." + root.getElementCount()));
            return;
        }
        textArea.setCaretPosition(root.getElement(line - 1).getStartOffset());
        textArea.requestFocusInWindow();
    }

    private void goToLine() {
        String input = JOptionPane.showInputDialog(
                this, "Номер рядка:");

        if (input == null) {
            return;
        }

        try {
            int line = Integer.parseInt(input.trim());
            app.goToLine(line);
            moveCaretToLine(line);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Введи ціле число.");
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void goToPage() {
        String input = JOptionPane.showInputDialog(
                this, "Номер сторінки:");

        if (input == null) {
            return;
        }

        try {
            int page = Integer.parseInt(input.trim());
            moveCaretToLine(app.goToPage(page));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Введи ціле число.");
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void addBookmark() {
        int line = caretLine();
        String label = JOptionPane.showInputDialog(
                this, "Мітка закладки для рядка " + line + ":");

        if (label == null) {
            return;
        }

        try {
            app.addBookmark(line, label.isBlank() ? "Рядок " + line : label.trim());
            JOptionPane.showMessageDialog(this, "Закладку додано на рядку " + line + ".");
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void removeBookmark() {
        int line = caretLine();

        try {
            if (app.removeBookmark(line)) {
                JOptionPane.showMessageDialog(this, "Закладку з рядка " + line + " видалено.");
            } else {
                JOptionPane.showMessageDialog(this, "На рядку " + line + " немає закладки.");
            }
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void showBookmarks() {
        try {
            Map<Integer, String> bookmarks = app.getBookmarks();

            if (bookmarks.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Закладок немає.");
                return;
            }

            List<Integer> lines = new ArrayList<>(bookmarks.keySet());
            String[] items = new String[lines.size()];
            for (int i = 0; i < items.length; i++) {
                items[i] = "Рядок " + lines.get(i) + " — " + bookmarks.get(lines.get(i));
            }

            Object choice = JOptionPane.showInputDialog(
                    this, "Оберіть закладку:", "Закладки",
                    JOptionPane.PLAIN_MESSAGE, null, items, items[0]);

            if (choice == null) {
                return;
            }

            for (int i = 0; i < items.length; i++) {
                if (items[i].equals(choice)) {
                    int line = app.goToBookmark(lines.get(i));
                    moveCaretToLine(line);
                    return;
                }
            }
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private boolean expandAt(int position) throws SQLException {
        int before = textArea.getDocument().getLength();

        if (!app.expandSnippet(position)) {
            return false;
        }

        refreshFromApp();
        int after = textArea.getDocument().getLength();
        textArea.setCaretPosition(
                Math.max(0, Math.min(position + after - before, after)));
        textArea.requestFocusInWindow();
        return true;
    }

    private void expandSnippet() {
        try {
            if (!expandAt(textArea.getCaretPosition())) {
                JOptionPane.showMessageDialog(
                        this, "Для слова перед курсором сніппета немає.");
            }
        } catch (SQLException | RuntimeException ex) {
            showError(ex);
        }
    }

    private void suggest() {
        int caret = textArea.getCaretPosition();
        String text = textArea.getText();
        int start = caret;

        while (start > 0 && (Character.isLetterOrDigit(text.charAt(start - 1))
                || text.charAt(start - 1) == '_')) {
            start--;
        }

        String prefix = text.substring(start, caret);

        if (prefix.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "Спочатку введи початок скорочення сніппета.");
            return;
        }

        try {
            List<String> options = app.suggest(prefix);

            if (options.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Підказок немає.");
                return;
            }

            Object choice = JOptionPane.showInputDialog(
                    this, "Підказки для \"" + prefix + "\":", "Підказки",
                    JOptionPane.PLAIN_MESSAGE, null,
                    options.toArray(new String[0]), options.get(0));

            if (choice == null) {
                return;
            }

            String shortcut = (String) choice;
            StyledDocument styled = textArea.getStyledDocument();
            styled.remove(start, caret - start);
            styled.insertString(start, shortcut, null);
            expandAt(start + shortcut.length());
        } catch (SQLException | BadLocationException | RuntimeException ex) {
            showError(ex);
        }
    }

    private void startMacro() {
        try {
            app.startMacroRecording();
            recording = true;
            updateMacroMenu();
            status.setText("Запис макросу... | " + status.getText());
        } catch (RuntimeException ex) {
            showError(ex);
        }
    }

    private void stopMacro() {
        String name = JOptionPane.showInputDialog(this, "Назва макросу:");

        if (name == null || name.isBlank()) {
            return;
        }

        try {
            app.stopMacroRecording(name.trim());
            JOptionPane.showMessageDialog(this, "Макрос збережено.");
        } catch (SQLException | RuntimeException ex) {
            showError(ex);
        } finally {
            resetMacroRecording();
            updateStatus();
        }
    }

    private void runMacro() {
        try {
            List<String> names = app.getMacroNames();

            if (names.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Збережених макросів немає.");
                return;
            }

            Object choice = JOptionPane.showInputDialog(
                    this, "Оберіть макрос:", "Макроси",
                    JOptionPane.PLAIN_MESSAGE, null,
                    names.toArray(new String[0]), names.get(0));

            if (choice == null) {
                return;
            }

            if (app.runMacro((String) choice)) {
                refreshFromApp();
            }
        } catch (SQLException | RuntimeException ex) {
            showError(ex);
            refreshFromApp();
        }
    }

    private void updateStatus() {
        if (app.getDocument() != null) {
            status.setText(
                    app.getStatusText()
                            + " | Кодування: " + app.getDocument().getEncoding());
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                ex.getMessage(),
                "Помилка",
                JOptionPane.ERROR_MESSAGE);
    }

    private void closeEditor() {
        try {
            app.close();
            dispose();
        } catch (SQLException ex) {
            showError(ex);
        }
    }
}

