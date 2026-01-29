package dev.addition.ten.util.persistence;

import dev.addition.ten.util.ScheduleUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BatchQueryBuilder {

    private final MySQLDataBase database;
    private final StringBuilder sql = new StringBuilder();
    private final List<Object[]> batchParams = new ArrayList<>();

    public BatchQueryBuilder(MySQLDataBase database) {
        this.database = database;
    }

    public BatchQueryBuilder insertInto(String table, String... columns) {
        sql.append("INSERT INTO ").append(table)
                .append(" (").append(String.join(", ", columns)).append(") VALUES (");
        // Create the placeholders ?,?,? once
        sql.append("?,".repeat(columns.length));
        sql.setLength(sql.length() - 1); // Remove trailing comma
        sql.append(")");
        return this;
    }

    // Adds a single row of data to the batch
    public BatchQueryBuilder addBatch(Object... values) {
        batchParams.add(values);
        return this;
    }

    public BatchQueryBuilder deleteFrom(String table, String idColumn) {
        sql.setLength(0);
        sql.append("DELETE FROM ").append(table).append(" WHERE ").append(idColumn).append(" = ?");
        return this;
    }

    public BatchQueryBuilder onDuplicateKeyUpdate(String... columns) {
        sql.append(" ON DUPLICATE KEY UPDATE ");
        for (int i = 0; i < columns.length; i++) {
            sql.append(columns[i]).append(" = VALUES(").append(columns[i]).append(")");
            if (i < columns.length - 1) {
                sql.append(", ");
            }
        }
        return this;
    }

    public int[] execute() {
        if (batchParams.isEmpty()) return new int[0];

        try (Connection conn = database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (Object[] row : batchParams) {
                for (int i = 0; i < row.length; i++) {
                    ps.setObject(i + 1, row[i]);
                }
                ps.addBatch();
            }

            return ps.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Batch execution failed: " + sql, e);
        }
    }

    public CompletableFuture<int[]> executeAsync() {
        CompletableFuture<int[]> future = new CompletableFuture<>();
        ScheduleUtil.scheduleAsync(() -> future.complete(execute()));
        return future;
    }
}