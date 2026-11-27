package editor;

import com.example.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryIntegrationTest {

    private String url;
    private MySQLConnection db;

    @BeforeEach
    void setUp() {
        url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        db = new MySQLConnection(url, "sa", "");
    }

    @Test
    void builtInSnippetsAreSeededAndSavingUpdatesTheTemplate() throws Exception {
        SnippetRepository snippets = new SnippetRepository(db);

        assertTrue(snippets.findByShortcut("psvm").orElseThrow().startsWith("public static void main"));
        assertEquals(Optional.empty(), snippets.findByShortcut("nope"));

        snippets.save("sout", "System.out.println(\"\");");
        assertEquals("System.out.println(\"\");", snippets.findByShortcut("sout").orElseThrow());
    }

    @Test
    void suggestionsMatchByPrefix() throws Exception {
        SnippetRepository snippets = new SnippetRepository(db);
        snippets.save("sorted", "x");

        assertEquals(List.of("sorted", "sout"), snippets.findShortcuts("so"));
        assertEquals(List.of("psvm"), snippets.findShortcuts("p"));
        assertTrue(snippets.findShortcuts("zzz").isEmpty());
        assertTrue(snippets.findShortcuts("%").isEmpty());
    }

    @Test
    void macroIsSavedLoadedAndOverwrittenByName() throws Exception {
        MacroRepository macros = new MacroRepository(db);
        TextDocument first = new TextDocument("a.txt", "UTF-8", "");
        macros.save(new MacroCommand("m", List.of(new InsertTextCommand(first, 0, "one"))));
        macros.save(new MacroCommand("m", List.of(new InsertTextCommand(first, 0, "two"))));

        TextDocument other = new TextDocument("b.txt", "UTF-8", "");
        macros.load("m", other).orElseThrow().execute();

        assertEquals("two", other.getText());
        assertEquals(List.of("m"), macros.findAllNames());
        assertTrue(macros.load("unknown", other).isEmpty());
    }

    @Test
    void appExpandsSnippetsWithOneUndoStep() throws Exception {
        TextEditorApp app = new TextEditorApp(url, "sa", "");
        app.newDocument("S.java");
        app.insertText(0, "psvm");

        assertEquals(List.of("psvm"), app.suggest("ps"));
        assertTrue(app.expandSnippet(4));
        assertTrue(app.getDocument().getText().startsWith("public static void main"));

        app.undo();
        assertEquals("psvm", app.getDocument().getText());
        assertFalse(app.expandSnippet(0));
        app.deleteText(0, 4);
        app.insertText(0, "zzz");
        assertFalse(app.expandSnippet(3));
        assertEquals("zzz", app.getDocument().getText());
        assertTrue(app.suggest("").isEmpty());
    }

    @Test
    void appRecordsAMacroAndAnotherSessionRunsIt() throws Exception {
        TextEditorApp app = new TextEditorApp(url, "sa", "");
        app.newDocument("a.txt");
        app.startMacroRecording();
        app.insertText(0, "A");
        app.insertText(1, "B");
        app.stopMacroRecording("ab");

        TextEditorApp restarted = new TextEditorApp(url, "sa", "");
        restarted.newDocument("b.txt");
        assertTrue(restarted.runMacro("ab"));
        assertEquals("AB", restarted.getDocument().getText());
        assertEquals(List.of("ab"), restarted.getMacroNames());

        restarted.undo();
        assertEquals("", restarted.getDocument().getText());
        assertFalse(restarted.runMacro("unknown"));
    }

    @Test
    void failingMacroStepRollsBackTheWholeMacro() throws Exception {
        TextEditorApp app = new TextEditorApp(url, "sa", "");
        app.newDocument("a.txt");
        app.insertText(0, "abcdefgh");
        app.startMacroRecording();
        app.insertText(0, "X");
        app.deleteText(5, 8);
        app.stopMacroRecording("risky");

        app.newDocument("b.txt");
        app.insertText(0, "abc");

        org.junit.jupiter.api.Assertions.assertThrows(IndexOutOfBoundsException.class,
                () -> app.runMacro("risky"));
        assertEquals("abc", app.getDocument().getText());
    }
}
