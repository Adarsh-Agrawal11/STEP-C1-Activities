package com.gdb.tests;

import com.gdb.db.*;
import com.gdb.domain.*;
import com.gdb.repository.*;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class TestConnectionPooling {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("  ACTIVITY 25 — CONNECTION POOLING WITH HIKARICP (NFR)");
        System.out.println("=".repeat(60));

        String testDbUrl = "jdbc:sqlite:test_pool_gdb.db";
        Properties props = new Properties();
        props.setProperty("persistence.db.url", "jdbc:sqlite:test_pool_gdb.db");
        props.setProperty("persistence.db.driver", "org.sqlite.JDBC");
        props.setProperty("persistence.pool.enabled", "true");
        props.setProperty("persistence.pool.size", "5");
        props.setProperty("persistence.pool.timeout", "10000");

        File dbFile = new File("test_pool_gdb.db");
        if (dbFile.exists()) dbFile.delete();

        ConnectionProvider pool = new HikariConnectionProvider(props);
        SchemaInitializer.initialize(pool);
        System.out.println("[TEST 1] HikariCP Pool Initialization:");
        System.out.println("  Provider: " + pool.getProviderName());
        try (Connection c = pool.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT 1")) {
            if (!rs.next() || rs.getInt(1) != 1) throw new AssertionError("Pool connection query failed");
        }
        System.out.println("  Connection acquired and returned successfully. -> PASSED");

        System.out.println("[TEST 2] Repository Operations through Pool:");
        AccountRepository accounts = new JdbcAccountRepository(pool);
        IAccount a1 = AccountFactory.createAccount("SAVINGS", 1001, "Rajesh", 30, 50000);
        IAccount a2 = AccountFactory.createAccount("SAVINGS", 1002, "Priya", 28, 40000);
        accounts.save(a1); accounts.save(a2);
        TransactionRepository transactions = new JdbcTransactionRepository(pool);
        transactions.transfer(1001, 1002, 10000);
        if (accounts.findById(1001).getBalance() != 40000 || accounts.findById(1002).getBalance() != 50000) throw new AssertionError("Pooled repository transfer failed");
        System.out.println("  Pooled repository transfer passed.");

        System.out.println("[TEST 3] Pooled vs Direct Connection Benchmark:");
        int iterations = 20;
        long start = System.nanoTime();
        for (int i=0;i<iterations;i++) try (Connection c=pool.getConnection(); Statement st=c.createStatement(); ResultSet rs=st.executeQuery("SELECT 1")) { rs.next(); }
        long pooled = System.nanoTime()-start;
        start = System.nanoTime();
        for (int i=0;i<iterations;i++) try (Connection c=DriverManager.getConnection(testDbUrl); Statement st=c.createStatement(); ResultSet rs=st.executeQuery("SELECT 1")) { rs.next(); }
        long direct = System.nanoTime()-start;
        System.out.println("  Pooled: " + (pooled/1_000_000) + " ms; Direct: " + (direct/1_000_000) + " ms.");
        pool.shutdown();

        if (dbFile.exists()) dbFile.delete();

        System.out.println("Activity 25 tests complete.");
    }
}
