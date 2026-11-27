package editor;

import com.example.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MacroJsonTest {

    @Test
    void macroRoundTripsAndRunsOnAnotherDocument() {
        TextDocument recordedOn = new TextDocument("a.txt", "UTF-8", "");
        MacroCommand macro = new MacroCommand("m", List.of(
                new InsertTextCommand(recordedOn, 0, "Привіт \"світе\"\n"),
                new InsertTextCommand(recordedOn, 15, "!"),
                new DeleteTextCommand(recordedOn, 0, 1)));

        String json = MacroRepository.toJson(macro);
        TextDocument other = new TextDocument("b.txt", "UTF-8", "");
        MacroCommand loaded = MacroRepository.fromJson("m", json, other);
        loaded.execute();

        assertEquals("ривіт \"світе\"\n!", other.getText());
        assertEquals("", recordedOn.getText());
    }

    @Test
    void nestedMacrosAreFlattened() {
        TextDocument doc = new TextDocument("a.txt", "UTF-8", "");
        MacroCommand inner = new MacroCommand("inner", List.of(new InsertTextCommand(doc, 0, "ab")));
        MacroCommand outer = new MacroCommand("outer", List.of(inner, new InsertTextCommand(doc, 0, "X")));

        MacroCommand loaded = MacroRepository.fromJson("outer", MacroRepository.toJson(outer), doc);

        assertEquals(2, loaded.getCommands().size());
    }

    @Test
    void unknownStepTypeIsRejected() {
        TextDocument doc = new TextDocument("a.txt", "UTF-8", "");

        assertThrows(IllegalArgumentException.class, () -> MacroRepository.fromJson(
                "bad", "[{\"type\":\"WAT\",\"position\":0,\"start\":0,\"end\":0}]", doc));
    }
}
