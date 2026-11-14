package com.gdb.file;

import com.gdb.domain.*;
import com.gdb.exceptions.AccountException;

import java.time.LocalDateTime;

/**
 * Utility mapping between Domain models and CSV formatted rows.
 */
public class CsvMapper {

    public static final String ACCOUNT_HEADER = "account_number,account_holder_name,age,balance,account_type,status,pin,opening_date,overdraft_used,interest_earned";
    public static final String TRANSACTION_HEADER = "transaction_id,timestamp,account_number,type,amount,balance_after,status,from_account,to_account,description";

    // ============================================================
    // 📝 STEP 1: Implement toCsv(IAccount account)
    //
    // INSTRUCTIONS:
    //   Format account fields as comma-separated string matching ACCOUNT_HEADER.
    // ============================================================
    public static String toCsv(IAccount account) {
        if (account == null) throw new IllegalArgumentException("Account cannot be null");
        Account a = (Account) account;
        String type = account.getAccountType().replaceAll("\\s+", "").toUpperCase();
        double overdraft = account instanceof CurrentAccount ? ((CurrentAccount) account).getOverdraftUsed() : 0.0;
        return join(account.getAccountNumber(), account.getAccountHolderName(), a.getAge(), account.getBalance(), type,
                account.isActive() ? "Active" : "Inactive", a.getPin() == null ? "" : a.getPin(), account.getOpeningDate(), overdraft, 0.0);
    }

    // ============================================================
    // 📝 STEP 2: Implement fromCsv(String line)
    //
    // INSTRUCTIONS:
    //   Parse CSV line and reconstruct appropriate IAccount subclass using AccountFactory.
    // ============================================================
    public static IAccount fromCsv(String line) {
        if (line == null || line.trim().isEmpty() || line.startsWith("account_number")) return null;
        try {
            String[] f = split(line);
            String type = f[4].trim().replaceAll("\\s+", "");
            IAccount account = AccountFactory.createAccount(type, Integer.parseInt(f[0]), f[1], Integer.parseInt(f[2]), Double.parseDouble(f[3]));
            Account base = (Account) account;
            if (f.length > 6 && !f[6].isEmpty()) account.setPin(Integer.parseInt(f[6]));
            if (f.length > 7) base.setOpeningDate(f[7]);
            if (account instanceof CurrentAccount && f.length > 8 && !f[8].isEmpty()) ((CurrentAccount) account).setOverdraftUsed(Double.parseDouble(f[8]));
            if (account instanceof FixedDepositAccount && f.length > 9 && !f[9].isEmpty()) ((FixedDepositAccount) account).setInterestEarned(Double.parseDouble(f[9]));
            if (f.length > 5 && "Inactive".equalsIgnoreCase(f[5])) account.closeAccount();
            return account;
        } catch (Exception e) { throw new IllegalArgumentException("Invalid account CSV row: " + line, e); }
    }

    // ============================================================
    // 📝 STEP 3: Implement toCsv(Transaction transaction)
    //
    // INSTRUCTIONS:
    //   Format transaction fields matching TRANSACTION_HEADER.
    // ============================================================
    public static String toCsv(Transaction transaction) {
        if (transaction == null) throw new IllegalArgumentException("Transaction cannot be null");
        return join(transaction.getTransactionId(), transaction.getTimestamp(), transaction.getAccountNumber(), transaction.getType(),
                transaction.getAmount(), transaction.getBalanceAfter(), transaction.getStatus(), transaction.getFromAccount(),
                transaction.getToAccount(), transaction.getDescription());
    }

    // ============================================================
    // 📝 STEP 4: Implement fromCsvTransaction(String line)
    //
    // INSTRUCTIONS:
    //   Parse CSV line and reconstruct Transaction model.
    // ============================================================
    public static Transaction fromCsvTransaction(String line) {
        if (line == null || line.trim().isEmpty() || line.startsWith("transaction_id")) return null;
        try {
            String[] f = split(line);
            return new Transaction(f[0], LocalDateTime.parse(f[1]), Integer.parseInt(f[2]), TransactionType.valueOf(f[3]),
                    Double.parseDouble(f[4]), Double.parseDouble(f[5]), f[6], f[9], Integer.parseInt(f[7]), Integer.parseInt(f[8]));
        } catch (Exception e) { throw new IllegalArgumentException("Invalid transaction CSV row: " + line, e); }
    }
    private static String join(Object... values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append(',');
            String value = values[i] == null ? "" : values[i].toString();
            if (value.contains(",") || value.contains("\"") || value.contains("\n")) out.append('"').append(value.replace("\"", "\"\"")).append('"');
            else out.append(value);
        }
        return out.toString();
    }

    private static String[] split(String line) {
        java.util.List<String> values = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { current.append('"'); i++; }
                else quoted = !quoted;
            } else if (ch == ',' && !quoted) { values.add(current.toString()); current.setLength(0); }
            else current.append(ch);
        }
        values.add(current.toString());
        return values.toArray(new String[0]);
    }
}
