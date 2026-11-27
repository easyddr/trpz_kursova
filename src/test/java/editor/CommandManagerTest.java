package editor;

import com.example.CommandManager;
import com.example.DeleteTextCommand;
import com.example.InsertTextCommand;
import com.example.TextDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandManagerTest {

    private TextDocument doc;
    private CommandManager manager;

    @BeforeEach
    void setUp() {
        doc = new TextDocument("a.txt", "UTF-8", "");
        manager = new CommandManager();
    }

    @Test
    void undoAndRedoRestoreTheText() {
        manager.execute(new InsertTextCommand(doc, 0, "hello"));
        manager.execute(new DeleteTextCommand(doc, 0, 2));
        assertEquals("llo", doc.getText());

        assertTrue(manager.undo());
        assertEquals("hello", doc.getText());
        assertTrue(manager.undo());
        assertEquals("", doc.getText());
        assertFalse(manager.undo());

        assertTrue(manager.redo());
        assertTrue(manager.redo());
        assertEquals("llo", doc.getText());
        assertFalse(manager.redo());
    }

    @Test
    void newCommandClearsTheRedoStack() {
        manager.execute(new InsertTextCommand(doc, 0, "a"));
        manager.undo();
        assertTrue(manager.canRedo());

        manager.execute(new InsertTextCommand(doc, 0, "b"));

        assertFalse(manager.canRedo());
    }

    @Test
    void failedCommandDoesNotEnterTheHistory() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> manager.execute(new InsertTextCommand(doc, 10, "x")));

        assertFalse(manager.canUndo());
        assertEquals("", doc.getText());
    }

    @Test
    void recordedMacroUndoesAndRepeats() {
        manager.startRecording();
        manager.execute(new InsertTextCommand(doc, 0, "ab"));
        manager.execute(new InsertTextCommand(doc, 2, "cd"));
        MacroCommand macro = manager.stopRecording("two-inserts");

        assertEquals("two-inserts", macro.getName());
        assertEquals(2, macro.getCommands().size());

        macro.undo();
        assertEquals("", doc.getText());
        macro.execute();
        assertEquals("abcd", doc.getText());
    }

    @Test
    void recordingStateIsValidated() {
        assertThrows(IllegalStateException.class, () -> manager.stopRecording("x"));
        manager.startRecording();
        assertTrue(manager.isRecording());
        assertThrows(IllegalStateException.class, () -> manager.startRecording());
    }

    @Test
    void macroIsAtomicWhenAStepFails() {
        doc.insert(0, "abc");
        MacroCommand macro = new MacroCommand("broken", List.of(
                new InsertTextCommand(doc, 0, "X"),
                new InsertTextCommand(doc, 1, "Y"),
                new DeleteTextCommand(doc, 50, 60)));

        assertThrows(IndexOutOfBoundsException.class, () -> manager.execute(macro));

        assertEquals("abc", doc.getText());
        assertFalse(manager.canUndo());
    }
}
