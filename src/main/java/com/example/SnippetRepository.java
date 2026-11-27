package com.example;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class SnippetRepository {

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("psvm", "public static void main(String[] args) {\n    \n}");
        DEFAULTS.put("sout", "System.out.println();");
        DEFAULTS.put("fori", "for (int i = 0; i < n; i++) {\n    \n}");
    }

    private final MySQLConnection db;
    private boolean ready;

    public SnippetRepository(MySQLConnection db) {
        this.db = Objects.requireNonNull(db, "db");
    }

    public void save(String shortcut, String template) throws SQLException {
        prepare();
        upsert(shortcut, template);
    }

    public Optional<String> findByShortcut(String shortcut) throws SQLException {
        prepare();
        return select(shortcut);
    }

    public List<String> findShortcuts(String prefix) throws SQLException {
        prepare();
        String escaped = prefix.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT shortcut FROM snippets WHERE shortcut LIKE ? ESCAPE '!' ORDER BY shortcut")) {
            ps.setString(1, escaped + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<String> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(rs.getString(1));
                }
                return result;
            }
        }
    }

    private void prepare() throws SQLException {
        if (ready) {
            return;
        }
        db.initSchema();
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            if (select(entry.getKey()).isEmpty()) {
                upsert(entry.getKey(), entry.getValue());
            }
        }
        ready = true;
    }

    private Optional<String> select(String shortcut) throws SQLException {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "SELECT template FROM snippets WHERE shortcut = ?")) {
            ps.setString(1, shortcut);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }

    private void upsert(String shortcut, String template) throws SQLException {
        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "INSERT INTO snippets (id, shortcut, template) VALUES (?, ?, ?) "
                        + "ON DUPLICATE KEY UPDATE template = VALUES(template)")) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, shortcut);
            ps.setString(3, template);
            ps.executeUpdate();
        }
    }
}
