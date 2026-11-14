package com.gdb.tests;

import com.gdb.command.*;
import com.gdb.db.SimulatedDatabase;
import com.gdb.domain.*;
import com.gdb.logging.*;
import java.util.List;

public class TestBridgeLogging {
    public static void main(String[] args) throws Exception {
        System.out.println("=".repeat(60));
        System.out.println("  ACTIVITY 18 — BRIDGE PATTERN (FILE + DB)");
        System.out.println("=".repeat(60));

        // Setup test accounts
        IAccount acc1 = AccountFactory.createAccount("SAVINGS", 1001, "John", 25, 15000);
        acc1.setPin(1234);
        IAccount acc2 = AccountFactory.createAccount("SAVINGS", 1002, "Jane", 30, 10000);

        FileLogDestination fileDest = new FileLogDestination();
        fileDest.clear();
        SimulatedDatabase db = new SimulatedDatabase();
        DatabaseLogDestination dbDest = new DatabaseLogDestination(db);
        MemoryLogDestination memDest = new MemoryLogDestination();

        TransactionLogger logger = new TransactionLogger(fileDest);
        for (int i = 0; i < 3; i++) {
            DepositCommand cmd = new DepositCommand(acc1, 100 + i);
            cmd.execute();
            logger.log(cmd);
        }
        System.out.println("[STEP 25] Logging to FILE destination...");
        System.out.println("  FILE log count: " + logger.readAll().size());

        logger.setDestination(dbDest);
        for (int i = 0; i < 3; i++) {
            DepositCommand cmd = new DepositCommand(acc1, 200 + i);
            cmd.execute();
            logger.log(cmd);
        }
        System.out.println("[STEP 26] Switched to DATABASE destination...");
        System.out.println("  DATABASE log count: " + logger.readAll().size());

        logger.setDestination(memDest);
        for (int i = 0; i < 3; i++) {
            DepositCommand cmd = new DepositCommand(acc1, 300 + i);
            cmd.execute();
            logger.log(cmd);
        }
        System.out.println("[STEP 27] Switched to MEMORY destination...");
        System.out.println("  MEMORY log count: " + logger.readAll().size());

        System.out.println("[STEP 28] Verifying Data Isolation:");
        System.out.println("  FILE count: " + fileDest.readAll().size() + " [EXPECTED: 3]");
        System.out.println("  DATABASE count: " + dbDest.readAll().size() + " [EXPECTED: 3]");
        System.out.println("  MEMORY count: " + memDest.readAll().size() + " [EXPECTED: 3]");
        if (fileDest.readAll().size() == 3 && dbDest.readAll().size() == 3 && memDest.readAll().size() == 3) {
            System.out.println("[STEP 29] All Bridge Pattern log backends verified successfully!");
        } else {
            throw new AssertionError("Log destination isolation failed");
        }

    }
}
