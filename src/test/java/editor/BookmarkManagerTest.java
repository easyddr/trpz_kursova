package editor;

import com.example.BookmarkManager;
import com.example.TextDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookmarkManagerTest {

    private TextDocument doc;
    private BookmarkManager manager;

    @BeforeEach
    void setUp() {
        String text = IntStream.rangeClosed(1, 45)
                .mapToObj(i -> "line " + i)
                .collect(Collectors.joining("\n"));
        doc = new TextDocument("long.txt", "UTF-8", text);
        manager = new BookmarkManager(doc, 20);
        doc.addObserver(manager);
    }

    @Test
    void bookmarkLineMustBeWithinTheDocument() {
        manager.addBookmark(1, "first");
        manager.addBookmark(45, "last");

        assertThrows(IllegalArgumentException.class, () -> manager.addBookmark(0, "x"));
        assertThrows(IllegalArgumentException.class, () -> manager.addBookmark(46, "x"));
        assertEquals(2, manager.getBookmarks().size());
    }

    @Test
    void secondBookmarkOnTheSameLineJustRenamesTheFirst() {
        manager.addBookmark(7, "old");
        manager.addBookmark(7, "new");

        assertEquals(Map.of(7, "new"), manager.getBookmarks());
    }

    @Test
    void pageNavigationUsesTheFormula() {
        assertEquals(1, manager.goToPage(1));
        assertEquals(21, manager.goToPage(2));
        assertEquals(41, manager.goToPage(3));
        assertEquals(3, manager.pageCount());
    }

    @Test
    void pageOutsideTheDocumentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> manager.goToPage(0));
        assertThrows(IllegalArgumentException.class, () -> manager.goToPage(4));
        assertEquals(1, manager.getCursorLine());
    }

    @Test
    void lineOutsideTheDocumentIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> manager.goToLine(-5));
        assertThrows(IllegalArgumentException.class, () -> manager.goToLine(46));
        assertEquals(30, manager.goToLine(30));
    }

    @Test
    void currentPageTextContainsOnlyThatPage() {
        manager.goToPage(3);

        String[] lines = manager.getCurrentPageText().split("\n");

        assertEquals(5, lines.length);
        assertEquals("line 41", lines[0]);
        assertEquals("line 45", lines[4]);
    }

    @Test
    void goToBookmarkRequiresAnExistingBookmark() {
        manager.addBookmark(12, "here");

        assertEquals(12, manager.goToBookmark(12));
        assertThrows(IllegalArgumentException.class, () -> manager.goToBookmark(13));
    }

    @Test
    void shrinkingTheDocumentDropsOutOfRangeBookmarksAndClampsTheCursor() {
        manager.addBookmark(45, "last");
        manager.addBookmark(2, "early");
        manager.goToLine(45);

        int cut = doc.getText().indexOf("line 10") - 1;
        doc.delete(cut, doc.length());

        assertEquals(Map.of(2, "early"), manager.getBookmarks());
        assertEquals(doc.lineCount(), manager.getCursorLine());
    }
}
