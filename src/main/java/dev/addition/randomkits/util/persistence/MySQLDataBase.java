package dev.addition.randomkits.util.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLDataBase {

    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private HikariDataSource dataSource;

    public MySQLDataBase(String host, String database, String username, String password) {
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public void connect() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new IllegalStateException("Database not connected. Call connect() first.");
        }
        return dataSource.getConnection();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public <T> QueryBuilder<T> query() {
        return new QueryBuilder<>(this);
    }

    public void transaction(TransactionCallback callback) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                callback.execute(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new IllegalStateException("Transaction failed", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Transaction connection failed", e);
        }
    }

    @FunctionalInterface
    public interface TransactionCallback {
        void execute(Connection conn) throws SQLException;
    }
}