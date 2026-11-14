package com.gdb.tests;

import com.gdb.domain.*;
import com.gdb.repository.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

public class TestFileRepository {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("  ACTIVITY 26 — FILE REPOSITORY (CSV + ATOMIC WRITES + BACKUP)");
        System.out.println("=".repeat(60));

        String accPath = "test_data/accounts.csv";
        String txnPath = "test_data/transactions.csv";

        // Clean test files
        new File(accPath).delete();
        new File(txnPath).delete();

        FileAccountRepository accounts = new FileAccountRepository(accPath, true, 1001);
        IAccount a1 = AccountFactory.createAccount("SAVINGS", 1001, "Rajesh Sharma", 30, 50000.0);
        a1.setPin(1234);
        accounts.save(a1);
        IAccount found = accounts.findById(1001);
        if (found == null || found.getBalance() != 50000.0) throw new AssertionError("Account save/read failed");
        ((Account) found).setBalance(60000.0); accounts.update(found);
        if (accounts.findById(1001).getBalance() != 60000.0) throw new AssertionError("Account update failed");
        IAccount a2 = AccountFactory.createAccount("CURRENT", 1002, "Priya Patel", 28, 40000.0);
        accounts.save(a2);
        if (accounts.findAll().size() != 2) throw new AssertionError("findAll failed");
        accounts.delete(1002);
        if (accounts.exists(1002)) throw new AssertionError("Delete failed");
        System.out.println("[TEST 1] Account CSV save/read/update/delete passed.");

        FileTransactionRepository transactions = new FileTransactionRepository(txnPath, true, 1);
        Transaction t1 = new Transaction("FILE-TXN-1", LocalDateTime.now(), 1001, TransactionType.DEPOSIT, 5000, 65000, "SUCCESS", "Deposit", 1001, 1001);
        Transaction t2 = new Transaction("FILE-TXN-2", LocalDateTime.now(), 1001, TransactionType.TRANSFER, 1000, 64000, "SUCCESS", "Transfer", 1001, 1002);
        transactions.save(t1); transactions.save(t2);
        if (transactions.findByAccount(1001).size() != 2 || transactions.findAll().size() != 2) throw new AssertionError("Transaction CSV query failed");
        System.out.println("[TEST 2] Transaction CSV save/query passed.");

        accounts.save(a2);
        transactions.transfer(1001, 1002, 10000.0);
        FileAccountRepository reloadedAccounts = new FileAccountRepository(accPath, false, 1001);
        if (reloadedAccounts.findById(1001).getBalance() != 50000.0 || reloadedAccounts.findById(1002).getBalance() != 50000.0)
            throw new AssertionError("File-backed transfer failed");
        System.out.println("[TEST 3] Atomic file transfer passed.");

        File accountFile = new File(accPath);
        if (!accountFile.exists()) throw new AssertionError("Account CSV not written");
        File[] backups = accountFile.getAbsoluteFile().getParentFile().listFiles((dir, name) -> name.startsWith("accounts.csv.bak."));
        System.out.println("[TEST 4] Backup files created: " + (backups == null ? 0 : backups.length));
        System.out.println("All Activity 26 file repository checks passed.");

        System.out.println("Activity 26 tests complete.");
    }
}
