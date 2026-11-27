package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

public class MySQLConnection implements AutoCloseable {

    public static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/text_editor"
            + "?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";

    private final String url;
    private final String user;
    private final String password;
    private Connection connection;
    private boolean schemaReady;

    public MySQLConnection(String url, String user, String password) {
        this.url = Objects.requireNonNull(url, "url");
        this.user = user;
        this.password = password;
    }

    public static MySQLConnection fromEnvironment() {
        return new MySQLConnection(
                System.getenv().getOrDefault("DB_URL", DEFAULT_URL),
                System.getenv().getOrDefault("DB_USER", "root"),
                System.getenv().getOrDefault("DB_PASSWORD", "admin"));
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, user, password);
            schemaReady = false;
        }
        return connection;
    }

    public synchronized void initSchema() throws SQLException {
        Connection conn = getConnection();
        if (schemaReady) {
            return;
        }
        try (Statement statement = conn.createStatement()) {
            for (String sql : readSchema().split(";")) {
                if (!sql.isBlank()) {
                    statement.execute(sql);
                }
            }
        }
        schemaReady = true;
    }

    private static String readSchema() throws SQLException {
        try (InputStream in = MySQLConnection.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new SQLException("schema.sql not found on classpath");
            }
            StringBuilder sql = new StringBuilder();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.strip().startsWith("--")) {
                    sql.append(line).append('\n');
                }
            }
            return sql.toString();
        } catch (IOException e) {
            throw new SQLException("Cannot read schema.sql", e);
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        if (connection != null) {
            connection.close();
            connection = null;
            schemaReady = false;
        }
    }
}
