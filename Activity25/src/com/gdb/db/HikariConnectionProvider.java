package com.gdb.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Production-grade ConnectionProvider backed by HikariCP Connection Pool.
 */
public class HikariConnectionProvider implements ConnectionProvider {

    // ============================================================
    // 📝 STEP 1: Declare DataSource Field
    // ============================================================
    private HikariDataSource dataSource;

    // ============================================================
    // 📝 STEP 2: Implement Constructor with HikariConfig
    //
    // INSTRUCTIONS:
    //   1. Create new HikariConfig().
    //   2. Set jdbcUrl from config "persistence.db.url" (default "jdbc:sqlite:gdb.db").
    //   3. Set driverClassName from config "persistence.db.driver" (default "org.sqlite.JDBC").
    //   4. Set maximumPoolSize from config "persistence.pool.size" (default "10").
    //   5. Set connectionTimeout from config "persistence.pool.timeout" (default "30000").
    //   6. Initialize this.dataSource = new HikariDataSource(hikariConfig).
    // ============================================================
    public HikariConnectionProvider(Properties config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.getProperty("persistence.db.url", "jdbc:sqlite:gdb.db"));
        hikariConfig.setDriverClassName(config.getProperty("persistence.db.driver", "org.sqlite.JDBC"));
        hikariConfig.setMaximumPoolSize(Integer.parseInt(config.getProperty("persistence.pool.size", "10")));
        hikariConfig.setConnectionTimeout(Long.parseLong(config.getProperty("persistence.pool.timeout", "30000")));
        hikariConfig.setIdleTimeout(Long.parseLong(config.getProperty("persistence.pool.idleTimeout", "600000")));
        try { this.dataSource = new HikariDataSource(hikariConfig); }
        catch (RuntimeException e) { throw new com.gdb.exceptions.DataAccessException("Failed to initialize HikariCP", e); }
    }

    // ============================================================
    // 📝 STEP 3: Implement getConnection()
    //
    // INSTRUCTIONS:
    //   Return a Connection acquired from dataSource.getConnection().
    // ============================================================
    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // ============================================================
    // 📝 STEP 4: Implement shutdown()
    //
    // INSTRUCTIONS:
    //   Close the dataSource if not already closed.
    // ============================================================
    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    @Override
    public String getProviderName() {
        return "HikariConnectionProvider [Pool size: " + (dataSource != null ? dataSource.getMaximumPoolSize() : "N/A") + "]";
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }
}
