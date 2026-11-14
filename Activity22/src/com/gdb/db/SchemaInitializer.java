package com.gdb.db;

import java.io.*;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Initializes the database schema idempotently by executing schema.sql DDL statements.
 */
public class SchemaInitializer {

    // ============================================================
    // 📝 STEP 5: Implement initialize(ConnectionProvider provider)
    //
    // INSTRUCTIONS:
    //   1. Obtain a Connection from provider.
    //   2. Read the contents of schema.sql from resource stream or file.
    //   3. Split SQL script into individual statements by semicolon ';'.
    //   4. Execute each non-empty statement using Statement.execute(sql).
    // ============================================================
    public static void initialize(ConnectionProvider provider) {
        if (provider == null) return;
        String sql = readSchemaSql();
        if (sql == null || sql.trim().isEmpty()) throw new IllegalStateException("schema.sql not found or empty");
        try (Connection conn = provider.getConnection(); Statement stmt = conn.createStatement()) {
            for (String part : sql.split(";")) { String command = part.trim(); if (!command.isEmpty()) stmt.execute(command); }
        } catch (Exception e) { throw new IllegalStateException("Schema initialization failed", e); }
    }

    // ============================================================
    // 📝 STEP 6: Helper Method to Read SQL Script
    // ============================================================
    public static String readSchemaSql() {
        String[] paths = {"schema.sql", "src/main/resources/schema.sql", "bin/schema.sql"};
        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) try { return new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8); } catch (IOException ignored) {}
        }
        try (InputStream in = SchemaInitializer.class.getResourceAsStream("/schema.sql")) {
            if (in != null) return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
        return "CREATE TABLE IF NOT EXISTS accounts (account_number INTEGER PRIMARY KEY, account_holder_name TEXT NOT NULL, age INTEGER NOT NULL, balance REAL NOT NULL, account_type TEXT NOT NULL, status TEXT NOT NULL, pin INTEGER, opening_date TEXT NOT NULL, overdraft_used REAL DEFAULT 0, interest_earned REAL DEFAULT 0); CREATE TABLE IF NOT EXISTS transactions (transaction_id TEXT PRIMARY KEY, timestamp TEXT NOT NULL, account_number INTEGER NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, balance_after REAL NOT NULL, status TEXT NOT NULL, from_account INTEGER, to_account INTEGER, description TEXT);";
    }
}
