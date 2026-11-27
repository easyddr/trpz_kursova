package editor;

import com.example.TextDocument;
import com.example.TextEditorApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextEditorAppTest {

    private static final Charset WIN1251 = Charset.forName("windows-1251");

    private TextEditorApp app;

    @BeforeEach
    void setUp() {

        app = new TextEditorApp("jdbc:mysql://127.0.0.1:1/none", "u", "p");
    }

    @Test
    void openFileDetectsWindows1251(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("note.txt");
        Files.write(file, "Привіт\nСвіт".getBytes(WIN1251));

        TextDocument doc = app.openFile(file);

        assertEquals("note.txt", doc.getFileName());
        assertEquals("windows-1251", doc.getEncoding());
        assertEquals("Привіт\nСвіт", doc.getText());
        assertTrue(app.getStatusText().contains("windows-1251"));
    }

    @Test
    void openFileStripsTheBom(@TempDir Path dir) throws Exception {
        Path utf8 = dir.resolve("u8.txt");
        Files.write(utf8, concat(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                "Тест".getBytes(StandardCharsets.UTF_8)));
        Path utf16 = dir.resolve("u16.txt");
        Files.write(utf16, concat(new byte[]{(byte) 0xFF, (byte) 0xFE},
                "Тест".getBytes(StandardCharsets.UTF_16LE)));

        assertEquals("Тест", app.openFile(utf8).getText());
        TextDocument doc16 = app.openFile(utf16);
        assertEquals("UTF-16LE", doc16.getEncoding());
        assertEquals("Тест", doc16.getText());
    }

    @Test
    void failedOpenKeepsTheCurrentDocument(@TempDir Path dir) throws Exception {
        app.newDocument("keep.txt");
        app.insertText(0, "keep me");
        Path binary = dir.resolve("b.bin");
        Files.write(binary, new byte[]{0, 1, 2});

        assertThrows(FileNotFoundException.class, () -> app.openFile(dir.resolve("missing.txt")));
        assertThrows(UnsupportedEncodingException.class, () -> app.openFile(binary));

        assertEquals("keep me", app.getDocument().getText());
    }

    @Test
    void highlightKeepsTheVisibleText() {
        app.newDocument("A.java");
        app.insertText(0, "class A { int n = 1; }");

        assertTrue(app.highlight().contains("\u001B["));
        assertEquals("class A { int n = 1; }", app.highlight().replaceAll("\u001B\\[[0-9;]*m", ""));
    }

    @Test
    void editingUndoRedoAndOutOfRangeEdits() {
        app.newDocument("a.txt");
        app.insertText(0, "abc");
        assertThrows(IndexOutOfBoundsException.class, () -> app.insertText(99, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> app.deleteText(2, 99));
        assertEquals("abc", app.getDocument().getText());

        app.deleteText(0, 1);
        assertEquals("bc", app.getDocument().getText());
        assertTrue(app.undo());
        assertEquals("abc", app.getDocument().getText());
        assertTrue(app.redo());
        assertEquals("bc", app.getDocument().getText());
    }

    @Test
    void bookmarksAndNavigation() {
        app.newDocument("a.txt");
        app.insertText(0, "1\n2\n3\n4\n5");

        app.addBookmark(2, "two");
        app.addBookmark(2, "renamed");
        assertEquals(Map.of(2, "renamed"), app.getBookmarks());
        assertEquals(2, app.goToBookmark(2));
        assertEquals(1, app.goToPage(1));
        assertThrows(IllegalArgumentException.class, () -> app.goToLine(6));
        assertThrows(IllegalArgumentException.class, () -> app.addBookmark(0, "x"));
        assertTrue(app.removeBookmark(2));
        assertFalse(app.removeBookmark(2));
    }

    @Test
    void saveToFileCanChangeTheEncoding(@TempDir Path dir) throws Exception {
        app.newDocument("a.txt");
        app.insertText(0, "Привіт");
        Path out = dir.resolve("out.txt");

        app.saveToFile(out, "windows-1251");

        assertArrayEquals("Привіт".getBytes(WIN1251), Files.readAllBytes(out));
        assertEquals("windows-1251", app.getDocument().getEncoding());
    }

    @Test
    void saveRejectsTextThatTheTargetEncodingCannotRepresent(@TempDir Path dir) {
        app.newDocument("a.txt");
        app.insertText(0, "emoji 😀");

        assertThrows(UnsupportedEncodingException.class,
                () -> app.saveToFile(dir.resolve("x.txt"), "windows-1251"));
        assertFalse(Files.exists(dir.resolve("x.txt")));
    }

    @Test
    void operationsWithoutAnOpenDocumentFailClearly() {
        assertThrows(IllegalStateException.class, () -> app.insertText(0, "x"));
        assertThrows(IllegalStateException.class, () -> app.highlight());
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
