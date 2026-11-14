package com.gdb.repository;

import com.gdb.domain.Transaction;
import com.gdb.domain.Account;
import com.gdb.domain.IAccount;
import com.gdb.domain.TransactionType;
import com.gdb.exceptions.DataAccessException;
import com.gdb.file.AtomicFileWriter;
import com.gdb.file.BackupManager;
import com.gdb.file.CsvMapper;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * File-backed implementation of TransactionRepository.
 * Stores transaction records in a text file and supports atomic balance transfers.
 */
public class FileTransactionRepository implements TransactionRepository {

    private final String filePath;
    private final boolean autosave;
    private final List<Transaction> cache = new ArrayList<>();

    public FileTransactionRepository(String filePath, boolean autosave, int startId) {
        this.filePath = filePath;
        this.autosave = autosave;
        load();
    }

    public FileTransactionRepository(String filePath, int startId) {
        this(filePath, true, startId);
    }

    public FileTransactionRepository(String filePath) {
        this(filePath, true, 1);
    }

    // ============================================================
    // 📝 STEP 1: Implement load() and flush()
    // ============================================================
    public synchronized void load() {
        cache.clear();
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return;
        try { for (String line : Files.readAllLines(path)) {
            if (line.trim().isEmpty() || line.startsWith(CsvMapper.TRANSACTION_HEADER)) continue;
            Transaction txn = CsvMapper.fromCsvTransaction(line); if (txn != null) cache.add(txn);
        }} catch (IOException | RuntimeException e) { throw new DataAccessException("Unable to load transactions file", e); }
    }

    public synchronized void flush() {
        try {
            if (Files.exists(Paths.get(filePath))) BackupManager.backup(filePath, 5);
            List<String> lines = new ArrayList<>(); lines.add(CsvMapper.TRANSACTION_HEADER);
            for (Transaction txn : cache) lines.add(CsvMapper.toCsv(txn));
            AtomicFileWriter.write(filePath, lines);
        } catch (IOException e) { throw new DataAccessException("Unable to write transactions file", e); }
    }

    // ============================================================
    // 📝 STEP 2: Implement save(Transaction transaction)
    // ============================================================
    @Override
    public synchronized void save(Transaction transaction) {
        if (transaction == null) return;
        cache.add(transaction); if (autosave) flush();
    }

    // ============================================================
    // 📝 STEP 3: Implement findByAccount(int accountNumber)
    // ============================================================
    @Override
    public synchronized List<Transaction> findByAccount(int accountNumber) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : cache) if (t.getAccountNumber() == accountNumber || t.getFromAccount() == accountNumber || t.getToAccount() == accountNumber) result.add(t);
        return result;
    }

    // ============================================================
    // 📝 STEP 4: Implement findAll()
    // ============================================================
    @Override
    public synchronized List<Transaction> findAll() {
        return new ArrayList<>(cache);
    }

    // ============================================================
    // 📝 STEP 5: Implement clear()
    // ============================================================
    @Override
    public synchronized void clear() {
        cache.clear(); if (autosave) flush();
    }

    // ============================================================
    // 📝 STEP 6: Implement transfer(int fromAccountNumber, int toAccountNumber, double amount)
    // ============================================================
    @Override
    public synchronized void transfer(int fromAccountNumber, int toAccountNumber, double amount) {
        if (amount <= 0) throw new DataAccessException("Transfer amount must be positive");
        Path accountPath = Paths.get(filePath).resolveSibling("accounts.csv");
        FileAccountRepository accounts = new FileAccountRepository(accountPath.toString(), false, 1001);
        IAccount from = accounts.findById(fromAccountNumber), to = accounts.findById(toAccountNumber);
        if (from == null || to == null) throw new DataAccessException("Source or destination account not found");
        if (from.getBalance() < amount) throw new DataAccessException("Insufficient balance");
        double oldFrom = from.getBalance(), oldTo = to.getBalance();
        if (!(from instanceof Account) || !(to instanceof Account)) throw new DataAccessException("Unsupported account implementation");
        Transaction transfer = null;
        try {
            ((Account) from).setBalance(oldFrom - amount); ((Account) to).setBalance(oldTo + amount);
            accounts.update(from); accounts.update(to); accounts.flush();
            transfer = new Transaction(Transaction.generateId(), LocalDateTime.now(), fromAccountNumber,
                    TransactionType.TRANSFER, amount, oldFrom - amount, "SUCCESS", "Transfer to Account #" + toAccountNumber,
                    fromAccountNumber, toAccountNumber);
            cache.add(transfer); if (autosave) flush();
        } catch (RuntimeException e) {
            if (transfer != null) cache.remove(transfer);
            ((Account) from).setBalance(oldFrom); ((Account) to).setBalance(oldTo);
            accounts.update(from); accounts.update(to); accounts.flush();
            throw new DataAccessException("File transfer failed", e);
        }
    }
}
