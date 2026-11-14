package com.gdb.repository;

import com.gdb.db.ConnectionProvider;
import com.gdb.domain.Transaction;
import com.gdb.domain.TransactionType;
import com.gdb.exceptions.DataAccessException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-backed implementation of TransactionRepository.
 * Handles transaction record persistence, retrieval, and multi-account atomic transfers.
 */
public class JdbcTransactionRepository implements TransactionRepository {

    private final ConnectionProvider provider;

    public JdbcTransactionRepository(ConnectionProvider provider) {
        this.provider = provider;
    }

    // ============================================================
    // 📝 STEP 1: Implement save(Transaction transaction)
    //
    // INSTRUCTIONS:
    //   1. Guard against null transaction.
    //   2. Prepare INSERT INTO transactions (id, timestamp, account_number, type, amount, balance_after, status, remarks, from_account, to_account)
    //   3. Bind values and executeUpdate.
    //   4. Catch SQLException and throw DataAccessException.
    // ============================================================
    @Override
    public void save(Transaction transaction) {
        if (transaction == null) return;
        String sql = "INSERT INTO transactions (transaction_id, timestamp, account_number, type, amount, balance_after, status, from_account, to_account, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = provider.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, transaction.getTransactionId()); ps.setString(2, transaction.getTimestamp().toString());
            ps.setInt(3, transaction.getAccountNumber()); ps.setString(4, transaction.getType().name());
            ps.setDouble(5, transaction.getAmount()); ps.setDouble(6, transaction.getBalanceAfter()); ps.setString(7, transaction.getStatus());
            ps.setInt(8, transaction.getFromAccount()); ps.setInt(9, transaction.getToAccount()); ps.setString(10, transaction.getDescription()); ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("Failed to save transaction", e); }
    }

    // ============================================================
    // 📝 STEP 2: Implement findByAccount(int accountNumber)
    //
    // INSTRUCTIONS:
    //   1. Prepare SELECT * FROM transactions WHERE account_number = ? OR from_account = ? OR to_account = ?
    //   2. Bind accountNumber and execute query.
    //   3. Map rows to Transaction objects and return list.
    //   4. Catch SQLException and throw DataAccessException.
    // ============================================================
    @Override
    public List<Transaction> findByAccount(int accountNumber) {
        String sql = "SELECT * FROM transactions WHERE account_number=? OR from_account=? OR to_account=? ORDER BY timestamp ASC";
        List<Transaction> result = new ArrayList<>();
        try (Connection c = provider.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountNumber); ps.setInt(2, accountNumber); ps.setInt(3, accountNumber);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) result.add(mapRow(rs)); }
            return result;
        } catch (SQLException e) { throw new DataAccessException("Failed to find transactions", e); }
    }

    // ============================================================
    // 📝 STEP 3: Implement findAll()
    //
    // INSTRUCTIONS:
    //   1. Prepare SELECT * FROM transactions
    //   2. Map rows to Transaction objects and return list.
    // ============================================================
    @Override
    public List<Transaction> findAll() {
        List<Transaction> result = new ArrayList<>();
        try (Connection c = provider.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM transactions ORDER BY timestamp ASC"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs)); return result;
        } catch (SQLException e) { throw new DataAccessException("Failed to retrieve transactions", e); }
    }

    // ============================================================
    // 📝 STEP 4: Implement clear()
    //
    // INSTRUCTIONS:
    //   1. Prepare DELETE FROM transactions
    //   2. Execute update.
    // ============================================================
    @Override
    public void clear() {
        try (Connection c = provider.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM transactions")) { ps.executeUpdate(); }
        catch (SQLException e) { throw new DataAccessException("Failed to clear transactions", e); }
    }

    // ============================================================
    // 📝 STEP 5: Implement transfer(int fromAccountNumber, int toAccountNumber, double amount)
    //
    // INSTRUCTIONS:
    //   1. Acquire a connection from provider and setAutoCommit(false).
    //   2. Check sender balance and ensure sufficient funds.
    //   3. Decrement sender balance (UPDATE accounts SET balance = balance - ? WHERE account_number = ?).
    //   4. Increment receiver balance (UPDATE accounts SET balance = balance + ? WHERE account_number = ?).
    //   5. Insert transfer audit record into transactions table using the SAME connection.
    //   6. Call conn.commit().
    //   7. If any exception occurs, call conn.rollback() and throw DataAccessException.
    // ============================================================
    @Override
    public void transfer(int fromAccountNumber, int toAccountNumber, double amount) {
        if (amount <= 0) throw new DataAccessException("Transfer amount must be positive");
        Connection conn = null;
        try {
            conn = provider.getConnection(); conn.setAutoCommit(false);
            double balance;
            try (PreparedStatement ps = conn.prepareStatement("SELECT balance FROM accounts WHERE account_number=?")) {
                ps.setInt(1, fromAccountNumber); try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new DataAccessException("Source account does not exist");
                    balance = rs.getDouble(1);
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM accounts WHERE account_number=?")) {
                ps.setInt(1, toAccountNumber); try (ResultSet rs = ps.executeQuery()) { if (!rs.next()) throw new DataAccessException("Destination account does not exist"); }
            }
            if (balance < amount) throw new DataAccessException("Insufficient balance");
            try (PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET balance=balance-? WHERE account_number=?")) { ps.setDouble(1, amount); ps.setInt(2, fromAccountNumber); ps.executeUpdate(); }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET balance=balance+? WHERE account_number=?")) { ps.setDouble(1, amount); ps.setInt(2, toAccountNumber); ps.executeUpdate(); }
            String sql = "INSERT INTO transactions (transaction_id,timestamp,account_number,type,amount,balance_after,status,from_account,to_account,description) VALUES (?,?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, Transaction.generateId()); ps.setString(2, LocalDateTime.now().toString()); ps.setInt(3, fromAccountNumber);
                ps.setString(4, TransactionType.TRANSFER.name()); ps.setDouble(5, amount); ps.setDouble(6, balance-amount); ps.setString(7, "SUCCESS");
                ps.setInt(8, fromAccountNumber); ps.setInt(9, toAccountNumber); ps.setString(10, "Transfer of Rs. " + amount + " to Account #" + toAccountNumber); ps.executeUpdate();
            }
            conn.commit();
        } catch (Exception e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignored) {}
            if (e instanceof DataAccessException) throw (DataAccessException)e;
            throw new DataAccessException("Transfer failed: " + e.getMessage(), e);
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("transaction_id");
        String tsStr = rs.getString("timestamp");
        LocalDateTime ts = tsStr != null ? LocalDateTime.parse(tsStr) : LocalDateTime.now();
        int accNum = rs.getInt("account_number");
        String typeStr = rs.getString("type");
        TransactionType type = TransactionType.valueOf(typeStr);
        double amt = rs.getDouble("amount");
        double balanceAfter = rs.getDouble("balance_after");
        String status = rs.getString("status");
        String remarks = rs.getString("description");
        int fromAcc = rs.getInt("from_account");
        int toAcc = rs.getInt("to_account");

        return new Transaction(id, ts, accNum, type, amt, balanceAfter, status, remarks, fromAcc, toAcc);
    }
}
