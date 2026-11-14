package com.gdb.repository;

import com.gdb.db.ConnectionProvider;
import com.gdb.domain.*;
import com.gdb.exceptions.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-backed implementation of AccountRepository.
 * Manages database persistence for accounts using PreparedStatements and ConnectionProvider.
 */
public class JdbcAccountRepository implements AccountRepository {

    private final ConnectionProvider provider;

    public JdbcAccountRepository(ConnectionProvider provider) {
        this.provider = provider;
    }

    // ============================================================
    // 📝 STEP 1: Implement save(IAccount account)
    //
    // INSTRUCTIONS:
    //   1. Guard against null account.
    //   2. Prepare an INSERT statement for table 'accounts':
    //      INSERT INTO accounts (account_number, account_holder_name, age, balance, account_type, status, pin, opening_date, overdraft_used, interest_earned) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    //   3. Bind parameters: account_number, account_holder_name, age, balance, account_type, status, pin, opening_date, overdraft_used, interest_earned.
    //   4. Execute the update using try-with-resources.
    //   5. Catch SQLException and wrap in DataAccessException.
    // ============================================================
    @Override
    public void save(IAccount account) {
        if (account == null) return;
        String sql = "INSERT INTO accounts (account_number, account_holder_name, age, balance, account_type, status, pin, opening_date, overdraft_used, interest_earned) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = provider.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, account.getAccountNumber()); ps.setString(2, account.getAccountHolderName()); ps.setInt(3, account instanceof Account ? ((Account) account).getAge() : 18);
            ps.setDouble(4, account.getBalance()); ps.setString(5, account.getAccountType()); ps.setString(6, account.isActive() ? "Active" : "Inactive");
            ps.setObject(7, account instanceof Account ? ((Account) account).getPin() : null); ps.setString(8, account.getOpeningDate());
            ps.setDouble(9, account instanceof CurrentAccount ? ((CurrentAccount) account).getOverdraftUsed() : 0.0);
            ps.setDouble(10, 0.0); ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("Failed to save account", e); }
    }

    // ============================================================
    // 📝 STEP 2: Implement findById(int accountNumber)
    //
    // INSTRUCTIONS:
    //   1. Prepare SELECT * FROM accounts WHERE account_number = ?
    //   2. Bind accountNumber.
    //   3. Execute query and map ResultSet to Account instance using mapRow(rs).
    //   4. Return account or null if not found.
    //   5. Catch SQLException and wrap in DataAccessException.
    // ============================================================
    @Override
    public IAccount findById(int accountNumber) {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (Connection c = provider.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, accountNumber); try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapRow(rs) : null; }
        } catch (SQLException e) { throw new DataAccessException("Failed to find account", e); }
    }

    // ============================================================
    // 📝 STEP 3: Implement findAll()
    //
    // INSTRUCTIONS:
    //   1. Prepare SELECT * FROM accounts
    //   2. Execute query and iterate over ResultSet, adding mapped accounts to a List.
    //   3. Return list of accounts.
    //   4. Catch SQLException and wrap in DataAccessException.
    // ============================================================
    @Override
    public List<IAccount> findAll() {
        List<IAccount> result = new ArrayList<>();
        try (Connection c = provider.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM accounts ORDER BY account_number"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs)); return result;
        } catch (SQLException e) { throw new DataAccessException("Failed to retrieve accounts", e); }
    }

    // ============================================================
    // 📝 STEP 4: Implement update(IAccount account)
    //
    // INSTRUCTIONS:
    //   1. Guard against null account.
    //   2. Prepare UPDATE accounts SET balance = ?, status = ?, overdraft_used = ?, interest_earned = ? WHERE account_number = ?
    //   3. Bind parameters and execute update.
    //   4. Catch SQLException and wrap in DataAccessException.
    // ============================================================
    @Override
    public void update(IAccount account) {
        if (account == null) return;
        String sql = "UPDATE accounts SET balance=?, status=?, overdraft_used=?, interest_earned=? WHERE account_number=?";
        try (Connection c = provider.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, account.getBalance()); ps.setString(2, account.isActive() ? "Active" : "Inactive");
            ps.setDouble(3, account instanceof CurrentAccount ? ((CurrentAccount) account).getOverdraftUsed() : 0.0); ps.setDouble(4, 0.0); ps.setInt(5, account.getAccountNumber()); ps.executeUpdate();
        } catch (SQLException e) { throw new DataAccessException("Failed to update account", e); }
    }

    // ============================================================
    // 📝 STEP 5: Implement delete(int accountNumber)
    //
    // INSTRUCTIONS:
    //   1. Prepare DELETE FROM accounts WHERE account_number = ?
    //   2. Bind accountNumber and execute update.
    //   3. Catch SQLException and wrap in DataAccessException.
    // ============================================================
    @Override
    public void delete(int accountNumber) {
        try (Connection c = provider.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM accounts WHERE account_number=?")) { ps.setInt(1, accountNumber); ps.executeUpdate(); }
        catch (SQLException e) { throw new DataAccessException("Failed to delete account", e); }
    }

    // ============================================================
    // 📝 STEP 6: Implement exists(int accountNumber)
    //
    // INSTRUCTIONS:
    //   1. Prepare SELECT 1 FROM accounts WHERE account_number = ?
    //   2. Bind accountNumber and check if ResultSet contains any row.
    //   3. Return true if exists, false otherwise.
    // ============================================================
    @Override
    public boolean exists(int accountNumber) {
        try (Connection c = provider.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM accounts WHERE account_number=?")) {
            ps.setInt(1, accountNumber); try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new DataAccessException("Failed to check account", e); }
    }

    // ============================================================
    // 📝 STEP 7: Implement nextAccountNumber()
    //
    // INSTRUCTIONS:
    //   1. Prepare SELECT COALESCE(MAX(account_number), 1000) + 1 AS next_id FROM accounts
    //   2. If max exists, return next_id; otherwise return 1001.
    // ============================================================
    @Override
    public int nextAccountNumber() {
        try (Connection c = provider.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(account_number),1000)+1 FROM accounts")) { return rs.next() ? rs.getInt(1) : 1001; }
        catch (SQLException e) { throw new DataAccessException("Failed to generate account number", e); }
    }

    public IAccount mapRow(ResultSet rs) throws SQLException {
        int accNo = rs.getInt("account_number");
        String name = rs.getString("account_holder_name");
        int age = rs.getInt("age");
        double balance = rs.getDouble("balance");
        String type = rs.getString("account_type");
        String status = rs.getString("status");
        int pin = rs.getInt("pin");

        try {
            Account acc = (Account) AccountFactory.createAccount(type, accNo, name, Math.max(18, age), balance);
            if (pin > 0) {
                try { acc.setPin(pin); } catch (Exception ignored) {}
            }
            if (!"Active".equalsIgnoreCase(status)) {
                try { acc.closeAccount(); } catch (Exception ignored) {}
            }
            return acc;
        } catch (Exception e) {
            throw new SQLException("Error mapping row to Account: " + e.getMessage(), e);
        }
    }
}
