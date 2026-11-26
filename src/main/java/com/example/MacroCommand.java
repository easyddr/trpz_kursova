package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MacroCommand implements Command {

    private final String name;
    private final List<Command> commands;

    public MacroCommand(String name, List<? extends Command> commands) {
        this.name = Objects.requireNonNull(name, "name");
        this.commands = List.copyOf(Objects.requireNonNull(commands, "commands"));
    }

    @Override
    public void execute() {
        List<Command> applied = new ArrayList<>();
        for (Command command : commands) {
            try {
                command.execute();
                applied.add(command);
            } catch (RuntimeException failure) {
                for (int i = applied.size() - 1; i >= 0; i--) {
                    try {
                        applied.get(i).undo();
                    } catch (RuntimeException secondary) {
                        failure.addSuppressed(secondary);
                    }
                }
                throw failure;
            }
        }
    }

    @Override
    public void undo() {
        List<Command> reversed = new ArrayList<>(commands);
        Collections.reverse(reversed);
        reversed.forEach(Command::undo);
    }

    public String getName() {
        return name;
    }

    public List<Command> getCommands() {
        return commands;
    }
}
