package com.gdb.tests;

import com.gdb.db.ConnectionProvider;
import com.gdb.db.JdbcConnectionProvider;
import com.gdb.db.SchemaInitializer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestJdbcConnection {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("  ACTIVITY 22 — JDBC FOUNDATION (CONNECTION & SCHEMA)");
        System.out.println("=".repeat(60));

        String testDbUrl = "jdbc:sqlite:test_gdb.db";
        ConnectionProvider provider = new JdbcConnectionProvider(testDbUrl);

        System.out.println("[TEST 1] Database Connection Establishment:");
        try (Connection conn = provider.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT 1")) {
            if (rs.next()) System.out.println("  Connected to: " + testDbUrl + " | SELECT 1 = " + rs.getInt(1));
        }
        System.out.println("[TEST 2] Schema DDL Execution:");
        SchemaInitializer.initialize(provider);
        System.out.println("  Tables initialized successfully.");
        System.out.println("[TEST 3] Schema Verification:");
        try (Connection conn = provider.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
            boolean accounts = false, transactions = false;
            while (rs.next()) { String name = rs.getString(1); if ("accounts".equals(name)) accounts = true; if ("transactions".equals(name)) transactions = true; }
            if (!accounts || !transactions) throw new AssertionError("Required tables missing");
            System.out.println("  Table 'accounts' exists: " + accounts);
            System.out.println("  Table 'transactions' exists: " + transactions);
        }

        provider.shutdown();
        System.out.println("Activity 22 JDBC tests complete.");
    }
}
