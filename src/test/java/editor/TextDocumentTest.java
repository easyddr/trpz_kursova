package editor;

import com.example.TextDocument;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextDocumentTest {

    @Test
    void insertAndDeleteChangeTheBuffer() {
        TextDocument doc = new TextDocument("a.txt", "UTF-8", "Hello");
        doc.insert(5, " world");
        assertEquals("Hello world", doc.getText());

        assertEquals("Hello ", doc.delete(0, 6));
        assertEquals("world", doc.getText());
    }

    @Test
    void invalidPositionThrowsAndLeavesTextUntouched() {
        TextDocument doc = new TextDocument("a.txt", "UTF-8", "abc");

        assertThrows(IndexOutOfBoundsException.class, () -> doc.insert(4, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> doc.insert(-1, "x"));
        assertThrows(IndexOutOfBoundsException.class, () -> doc.delete(2, 9));
        assertThrows(IndexOutOfBoundsException.class, () -> doc.delete(2, 1));

        assertEquals("abc", doc.getText());
    }

    @Test
    void lineCountAndLineRange() {
        assertEquals(1, new TextDocument("e", "UTF-8", "").lineCount());

        TextDocument doc = new TextDocument("a", "UTF-8", "a\nb\nc");
        assertEquals(3, doc.lineCount());
        assertEquals("b\nc", doc.getLines(2, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> doc.getLines(1, 4));
    }

    @Test
    void observersAreNotifiedOnEveryChange() {
        TextDocument doc = new TextDocument("a", "UTF-8", "");
        AtomicInteger calls = new AtomicInteger();
        doc.addObserver(changed -> calls.incrementAndGet());

        doc.insert(0, "x");
        doc.delete(0, 1);

        assertEquals(2, calls.get());
    }
}
