package com.example;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

public class CommandManager {

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private List<Command> recorded;

    public void execute(Command command) {
        Objects.requireNonNull(command, "command");
        command.execute();
        undoStack.push(command);
        redoStack.clear();
        if (recorded != null) {
            recorded.add(command);
        }
    }

    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        if (recorded != null) {
            recorded.remove(command);
        }
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        if (recorded != null) {
            recorded.add(command);
        }
        return true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void startRecording() {
        if (recorded != null) {
            throw new IllegalStateException("Macro recording is already in progress");
        }
        recorded = new ArrayList<>();
    }

    public boolean isRecording() {
        return recorded != null;
    }

    public MacroCommand stopRecording(String name) {
        if (recorded == null) {
            throw new IllegalStateException("Macro recording has not been started");
        }
        MacroCommand macro = new MacroCommand(name, recorded);
        recorded = null;
        return macro;
    }
}
