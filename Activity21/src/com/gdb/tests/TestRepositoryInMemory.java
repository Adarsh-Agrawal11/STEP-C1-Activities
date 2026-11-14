package com.gdb.tests;

import com.gdb.domain.*;
import com.gdb.repository.*;
import com.gdb.service.AccountService;
import com.gdb.logging.*;
import java.util.List;

public class TestRepositoryInMemory {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("  ACTIVITY 21 — REPOSITORY PATTERN (IN-MEMORY)");
        System.out.println("=".repeat(60));

        AccountRepository accountRepo = new InMemoryAccountRepository();
        TransactionRepository txnRepo = new InMemoryTransactionRepository();
        TransactionLogger logger = new TransactionLogger(new MemoryLogDestination());

        AccountService service = new AccountService(accountRepo, txnRepo, logger);

        System.out.println("[TEST 1] Account Repository CRUD:");
        IAccount account = AccountFactory.createAccount("SAVINGS", 1001, "Rajesh Sharma", 30, 50000.0);
        accountRepo.save(account);
        assert accountRepo.findById(1001) != null;
        account.setTenureYears(1);
        accountRepo.update(account);
        assert accountRepo.findAll().size() == 1;
        assert accountRepo.exists(1001);
        assert accountRepo.nextAccountNumber() == 1001;
        System.out.println("  Save, find, update, exists and ID generation passed.");

        System.out.println("[TEST 2] Transaction Repository Operations:");
        Transaction t = new Transaction("MEM-1", java.time.LocalDateTime.now(), 1001, TransactionType.DEPOSIT, 500, 50500, "SUCCESS", "test", 1001, 1001);
        txnRepo.save(t);
        assert txnRepo.findByAccount(1001).size() == 1;
        assert txnRepo.findAll().size() == 1;
        txnRepo.clear();
        assert txnRepo.findAll().isEmpty();
        System.out.println("  Save, query, findAll and clear passed.");

        System.out.println("[TEST 3] Service Integration:");
        IAccount john = service.openAccount("SAVINGS", "John", 25, 10000);
        john.setPin(1234);
        service.deposit(john.getAccountNumber(), 1000);
        service.withdraw(john.getAccountNumber(), 500, 1234);
        assert john.getBalance() == 10500.0;
        System.out.println("  Service operations passed. Balance: " + john.getBalance());

        System.out.println("Activity 21 Repository tests complete.");
    }
}
