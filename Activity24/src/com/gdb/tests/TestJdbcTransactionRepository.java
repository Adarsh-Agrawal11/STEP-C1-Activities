package com.gdb.tests;

import com.gdb.command.*;
import com.gdb.db.*;
import com.gdb.domain.*;
import com.gdb.logging.*;
import com.gdb.repository.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

public class TestJdbcTransactionRepository {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("  ACTIVITY 24 — JDBC TRANSACTION REPOSITORY & DATA MIGRATION");
        System.out.println("=".repeat(60));

        String testDbUrl = "jdbc:sqlite:test_txn_gdb.db";
        File dbFile = new File("test_txn_gdb.db");
        if (dbFile.exists()) dbFile.delete();

        ConnectionProvider provider = new JdbcConnectionProvider(testDbUrl);
        SchemaInitializer.initialize(provider);
        JdbcTransactionRepository txnRepo = new JdbcTransactionRepository(provider);

        Transaction t1 = new Transaction("TXN-101", LocalDateTime.now(), 1001, TransactionType.DEPOSIT, 5000, 55000, "SUCCESS", "Initial Deposit", 1001, 1001);
        Transaction t2 = new Transaction("TXN-102", LocalDateTime.now(), 1001, TransactionType.TRANSFER, 2000, 53000, "SUCCESS", "Transfer to #1002", 1001, 1002);
        txnRepo.save(t1); txnRepo.save(t2);
        if (txnRepo.findByAccount(1001).size() != 2) throw new AssertionError("Transaction query failed");
        if (txnRepo.findByAccount(1002).size() != 1) throw new AssertionError("Transfer recipient query failed");
        System.out.println("[TEST 1] Transaction save and account queries passed.");

        txnRepo.clear();
        LogDestination memory = new MemoryLogDestination();
        IAccount sender = AccountFactory.createAccount("SAVINGS", 1001, "Rajesh", 30, 50000);
        IAccount receiver = AccountFactory.createAccount("SAVINGS", 1002, "Priya", 28, 30000);
        sender.setPin(1234);
        DepositCommand deposit = new DepositCommand(sender, 1000); deposit.execute(); memory.write(deposit);
        WithdrawCommand withdraw = new WithdrawCommand(sender, 500, 1234); withdraw.execute(); memory.write(withdraw);
        TransferCommand transfer = new TransferCommand(sender, receiver, 500, 1234); transfer.execute(); memory.write(transfer);
        int migrated = DataMigrator.migrate(memory, txnRepo);
        if (migrated != 3 || txnRepo.findAll().size() != 3) throw new AssertionError("Migration failed");
        System.out.println("[TEST 2] Migrated " + migrated + " transaction records from memory to SQLite.");

        JdbcAccountRepository accountRepo = new JdbcAccountRepository(provider);
        accountRepo.save(AccountFactory.createAccount("SAVINGS", 1001, "Sender", 30, 50000));
        accountRepo.save(AccountFactory.createAccount("SAVINGS", 1002, "Receiver", 29, 30000));
        txnRepo.clear();
        txnRepo.transfer(1001, 1002, 10000);
        if (accountRepo.findById(1001).getBalance() != 40000 || accountRepo.findById(1002).getBalance() != 40000) throw new AssertionError("Atomic transfer failed");
        if (txnRepo.findAll().size() != 1) throw new AssertionError("Transfer audit record missing");
        try { txnRepo.transfer(1001, 1002, 999999); throw new AssertionError("Expected insufficient balance failure"); }
        catch (com.gdb.exceptions.DataAccessException expected) { }
        if (accountRepo.findById(1001).getBalance() != 40000 || accountRepo.findById(1002).getBalance() != 40000) throw new AssertionError("Rollback failed");
        System.out.println("[TEST 3] Atomic transfer and rollback passed.");

        provider.shutdown();
        if (dbFile.exists()) dbFile.delete();
        System.out.println("Activity 24 tests complete.");
    }
}
