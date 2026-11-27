package com.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class MacroRepository {

    private record Step(String type, int position, int start, int end, String text) {
    }

    private static final Gson GSON = new Gson();

    private final MySQLConnection db;

    public MacroRepository(MySQLConnection db) {
        this.db = Objects.requireNonNull(db, "db");
    }

    public void save(MacroCommand macro) throws SQLException {
        db.initSchema();
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "INSERT INTO macros (id, name, steps) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE steps = VALUES(steps)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, macro.getName());
            ps.setString(3, toJson(macro));
            ps.executeUpdate();
        }
    }

    public Optional<MacroCommand> load(String name, TextDocument document) throws SQLException {
        db.initSchema();
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT steps FROM macros WHERE name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? Optional.of(fromJson(name, rs.getString(1), document))
                        : Optional.empty();
            }
        }
    }

    public List<String> findAllNames() throws SQLException {
        db.initSchema();
        try (PreparedStatement ps = db.getConnection().prepareStatement("SELECT name FROM macros ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            List<String> names = new ArrayList<>();
            while (rs.next()) {
                names.add(rs.getString(1));
            }
            return names;
        }
    }

    static String toJson(MacroCommand macro) {
        List<Step> steps = new ArrayList<>();
        flatten(macro, steps);
        return GSON.toJson(steps);
    }

    static MacroCommand fromJson(String name, String json, TextDocument document) {
        List<Step> steps = GSON.fromJson(json, new TypeToken<List<Step>>() { }.getType());
        List<Command> commands = new ArrayList<>();
        for (Step step : steps) {
            commands.add(switch (step.type()) {
                case "INSERT" -> new InsertTextCommand(document, step.position(), step.text());
                case "DELETE" -> new DeleteTextCommand(document, step.start(), step.end());
                default -> throw new IllegalArgumentException("Unknown macro step type: " + step.type());
            });
        }
        return new MacroCommand(name, commands);
    }

    private static void flatten(Command command, List<Step> steps) {
        if (command instanceof InsertTextCommand insert) {
            steps.add(new Step("INSERT", insert.getPosition(), 0, 0, insert.getText()));
        } else if (command instanceof DeleteTextCommand delete) {
            steps.add(new Step("DELETE", 0, delete.getStart(), delete.getEnd(), null));
        } else if (command instanceof MacroCommand nested) {
            nested.getCommands().forEach(c -> flatten(c, steps));
        } else {
            throw new IllegalArgumentException("Command cannot be serialized: " + command.getClass().getName());
        }
    }
}

