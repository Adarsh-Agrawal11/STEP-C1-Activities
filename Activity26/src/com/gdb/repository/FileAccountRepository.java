package com.gdb.repository;

import com.gdb.domain.*;
import com.gdb.exceptions.DataAccessException;
import com.gdb.file.AtomicFileWriter;
import com.gdb.file.BackupManager;
import com.gdb.file.CsvMapper;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * File-backed implementation of AccountRepository.
 * Stores account records in a CSV/text file with atomic writes.
 */
public class FileAccountRepository implements AccountRepository {

    private final String filePath;
    private final boolean autosave;
    private int nextAccountNumber;
    private final Map<Integer, IAccount> cache = new HashMap<>();

    public FileAccountRepository(String filePath, boolean autosave, int startAccountNumber) {
        this.filePath = filePath;
        this.autosave = autosave;
        this.nextAccountNumber = startAccountNumber;
        load();
    }

    public FileAccountRepository(String filePath, int startAccountNumber) {
        this(filePath, true, startAccountNumber);
    }

    public FileAccountRepository(String filePath) {
        this(filePath, true, 1001);
    }

    // ============================================================
    // 📝 STEP 1: Implement load()
    //
    // INSTRUCTIONS:
    //   Read accounts from filePath and populate the memory cache.
    // ============================================================
    public synchronized void load() {
        cache.clear();
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return;
        try {
            for (String line : Files.readAllLines(path)) {
                if (line.trim().isEmpty() || line.startsWith(CsvMapper.ACCOUNT_HEADER)) continue;
                IAccount account = CsvMapper.fromCsv(line);
                if (account != null) { cache.put(account.getAccountNumber(), account); nextAccountNumber = Math.max(nextAccountNumber, account.getAccountNumber() + 1); }
            }
        } catch (IOException | RuntimeException e) { throw new DataAccessException("Unable to load accounts file", e); }
    }

    // ============================================================
    // 📝 STEP 2: Implement flush()
    //
    // INSTRUCTIONS:
    //   Serialize cache to CSV lines and write using AtomicFileWriter.
    // ============================================================
    public synchronized void flush() {
        try {
            if (Files.exists(Paths.get(filePath))) BackupManager.backup(filePath, 5);
            List<String> lines = new ArrayList<>(); lines.add(CsvMapper.ACCOUNT_HEADER);
            for (IAccount account : cache.values()) lines.add(CsvMapper.toCsv(account));
            AtomicFileWriter.write(filePath, lines);
        } catch (IOException e) { throw new DataAccessException("Unable to write accounts file", e); }
    }

    // ============================================================
    // 📝 STEP 3: Implement save(IAccount account)
    // ============================================================
    @Override
    public synchronized void save(IAccount account) {
        if (account == null) return;
        cache.put(account.getAccountNumber(), account);
        nextAccountNumber = Math.max(nextAccountNumber, account.getAccountNumber() + 1);
        if (autosave) flush();
    }

    // ============================================================
    // 📝 STEP 4: Implement findById(int accountNumber)
    // ============================================================
    @Override
    public synchronized IAccount findById(int accountNumber) {
        return cache.get(accountNumber);
    }

    // ============================================================
    // 📝 STEP 5: Implement findAll()
    // ============================================================
    @Override
    public synchronized List<IAccount> findAll() {
        return new ArrayList<>(cache.values());
    }

    // ============================================================
    // 📝 STEP 6: Implement update(IAccount account)
    // ============================================================
    @Override
    public synchronized void update(IAccount account) {
        if (account != null && cache.containsKey(account.getAccountNumber())) { cache.put(account.getAccountNumber(), account); if (autosave) flush(); }
    }

    // ============================================================
    // 📝 STEP 7: Implement delete(int accountNumber)
    // ============================================================
    @Override
    public synchronized void delete(int accountNumber) {
        cache.remove(accountNumber); if (autosave) flush();
    }

    // ============================================================
    // 📝 STEP 8: Implement exists(int accountNumber)
    // ============================================================
    @Override
    public synchronized boolean exists(int accountNumber) {
        return false;
    }

    // ============================================================
    // 📝 STEP 9: Implement nextAccountNumber()
    // ============================================================
    @Override
    public synchronized int nextAccountNumber() {
        return nextAccountNumber++;
    }
}
