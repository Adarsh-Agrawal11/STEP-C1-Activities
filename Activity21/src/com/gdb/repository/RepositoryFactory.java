package com.gdb.repository;

import java.io.*;
import java.util.Properties;

/**
 * Factory creating and supplying repository instances based on persistence.properties configuration.
 */
public class RepositoryFactory {

    private static AccountRepository accountRepositoryInstance;
    private static TransactionRepository transactionRepositoryInstance;

    // ============================================================
    // 📝 STEP 14: Read persistence.mode from Configuration
    //
    // INSTRUCTIONS:
    //   1. Load properties from "config/persistence.properties" or classpath resource.
    //   2. Return property value for "persistence.mode" (default to "memory" if missing).
    // ============================================================
    public static String getPersistenceMode() {
        Properties props = new Properties();
        String[] paths = {"config/persistence.properties", "src/main/resources/config/persistence.properties", "bin/config/persistence.properties"};
        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) try (InputStream in = new FileInputStream(file)) { props.load(in); return props.getProperty("persistence.mode", "memory").trim().toLowerCase(); } catch (IOException ignored) {}
        }
        try (InputStream in = RepositoryFactory.class.getResourceAsStream("/config/persistence.properties")) {
            if (in != null) { props.load(in); return props.getProperty("persistence.mode", "memory").trim().toLowerCase(); }
        } catch (IOException ignored) {}
        return "memory";
    }

    // ============================================================
    // 📝 STEP 15: Implement getAccountRepository()
    //
    // INSTRUCTIONS:
    //   1. Check persistence mode:
    //      - "memory" -> return singleton InMemoryAccountRepository
    //      - "jdbc"   -> throw UnsupportedOperationException("JDBC repository not implemented yet - coming in Activity 22")
    //      - "file"   -> throw UnsupportedOperationException("File repository not implemented yet")
    //      - default  -> return InMemoryAccountRepository
    // ============================================================
    public static synchronized AccountRepository getAccountRepository() {
        String mode = getPersistenceMode();
        if ("jdbc".equals(mode)) throw new UnsupportedOperationException("JDBC repository not implemented yet - coming in Activity 22");
        if ("file".equals(mode)) throw new UnsupportedOperationException("File repository not implemented yet");
        if (accountRepositoryInstance == null) accountRepositoryInstance = new InMemoryAccountRepository();
        return accountRepositoryInstance;
    }

    // ============================================================
    // 📝 STEP 16: Implement getTransactionRepository()
    //
    // INSTRUCTIONS:
    //   1. Check persistence mode:
    //      - "memory" -> return singleton InMemoryTransactionRepository
    //      - "jdbc"   -> throw UnsupportedOperationException("JDBC repository not implemented yet - coming in Activity 22")
    //      - "file"   -> throw UnsupportedOperationException("File repository not implemented yet")
    //      - default  -> return InMemoryTransactionRepository
    // ============================================================
    public static synchronized TransactionRepository getTransactionRepository() {
        String mode = getPersistenceMode();
        if ("jdbc".equals(mode)) throw new UnsupportedOperationException("JDBC repository not implemented yet - coming in Activity 22");
        if ("file".equals(mode)) throw new UnsupportedOperationException("File repository not implemented yet");
        if (transactionRepositoryInstance == null) transactionRepositoryInstance = new InMemoryTransactionRepository();
        return transactionRepositoryInstance;
    }
}
