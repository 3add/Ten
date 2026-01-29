package dev.addition.ten.util.persistence;

import dev.addition.ten.util.ScheduleUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class QueryBuilder<T> {

    private final MySQLDataBase database;
    private final StringBuilder sql = new StringBuilder();
    private final List<Object> params = new ArrayList<>();
    private RowMapper<T> mapper;

    public QueryBuilder(MySQLDataBase database) {
        this.database = database;
    }

    public QueryBuilder<T> select(String... columns) {
        sql.append("SELECT ").append(columns.length == 0 ? "*" : String.join(", ", columns));
        return this;
    }

    public QueryBuilder<T> from(String table) {
        sql.append(" FROM ").append(table);
        return this;
    }

    public QueryBuilder<T> where(String column, Object value) {
        sql.append(" WHERE ").append(column).append(" = ?");
        params.add(value);
        return this;
    }

    public QueryBuilder<T> and(String column, Object value) {
        sql.append(" AND ").append(column).append(" = ?");
        params.add(value);
        return this;
    }

    public QueryBuilder<T> or(String column, Object value) {
        sql.append(" OR ").append(column).append(" = ?");
        params.add(value);
        return this;
    }

    public QueryBuilder<T> orderBy(String column, boolean ascending) {
        sql.append(" ORDER BY ").append(column).append(ascending ? " ASC" : " DESC");
        return this;
    }

    public QueryBuilder<T> limit(int limit) {
        sql.append(" LIMIT ?");
        params.add(limit);
        return this;
    }

    public QueryBuilder<T> insertInto(String table, String... columns) {
        sql.append("INSERT INTO ").append(table)
                .append(" (").append(String.join(", ", columns)).append(") VALUES (");
        sql.append("?,".repeat(columns.length));
        sql.setLength(sql.length() - 1); // Remove trailing comma
        sql.append(")");
        return this;
    }

    public QueryBuilder<T> values(Object... values) {
        params.addAll(Arrays.asList(values));
        return this;
    }

    public QueryBuilder<T> update(String table) {
        sql.append("UPDATE ").append(table);
        return this;
    }

    public QueryBuilder<T> set(String column, Object value) {
        if (sql.toString().contains(" SET ")) {
            sql.append(", ");
        } else {
            sql.append(" SET ");
        }
        sql.append(column).append(" = ?");
        params.add(value);
        return this;
    }

    public QueryBuilder<T> deleteFrom(String table) {
        sql.append("DELETE FROM ").append(table);
        return this;
    }

    public QueryBuilder<T> map(RowMapper<T> mapper) {
        this.mapper = mapper;
        return this;
    }

    public QueryBuilder<T> onDuplicateKeyUpdate(String... columns) {
        sql.append(" ON DUPLICATE KEY UPDATE ");
        for (int i = 0; i < columns.length; i++) {
            // Use VALUES(column) to refer to the value that was intended for insertion
            sql.append(columns[i]).append(" = VALUES(").append(columns[i]).append(")");
            if (i < columns.length - 1) {
                sql.append(", ");
            }
        }
        return this;
    }

    public Optional<T> fetchOne() {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            setParams(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && mapper != null) {
                    return Optional.ofNullable(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Query failed: " + sql, e);
        }
        return Optional.empty();
    }

    public List<T> fetchAll() {
        List<T> results = new ArrayList<>();
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            setParams(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next() && mapper != null) {
                    results.add(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Query failed: " + sql, e);
        }

        return results;
    }

    public CompletableFuture<Optional<T>> fetchOneAsync() {
        CompletableFuture<Optional<T>> future = new CompletableFuture<>();
        ScheduleUtil.scheduleAsync(() -> future.complete(fetchOne()));

        return future;
    }

    public CompletableFuture<List<T>> fetchAllAsync() {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        ScheduleUtil.scheduleAsync(() -> future.complete(fetchAll()));

        return future;
    }

    public int execute() {
        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            setParams(ps);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Execute failed: " + sql, e);
        }
    }

    public CompletableFuture<Integer> executeAsync() {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        ScheduleUtil.scheduleAsync(() -> future.complete(execute()));

        return future;
    }

    private void setParams(PreparedStatement ps) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}