package dev.addition.randomkits.util.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.addition.randomkits.RandomKits;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class Repository<T, ID> {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final MySQLDataBase database = RandomKits.getDataBase();

    protected abstract String getTableName();
    protected abstract String getIdColumn();
    protected abstract RowMapper<T> getRowMapper();
    protected abstract void createTable(Connection conn) throws SQLException;

    public void initTable() {
        try (Connection conn = database.getConnection()) {
            createTable(conn);
        } catch (SQLException e) {
            log.error("Failed to initialize table: {}", getTableName(), e);
        }
    }

    public Optional<T> findById(ID id) {
        return database.<T>query()
                .select()
                .from(getTableName())
                .where(getIdColumn(), id)
                .map(getRowMapper())
                .fetchOne();
    }

    public CompletableFuture<Optional<T>> findByIdAsync(ID id) {
        return database.<T>query()
                .select()
                .from(getTableName())
                .where(getIdColumn(), id)
                .map(getRowMapper())
                .fetchOneAsync();
    }

    public List<T> findAll() {
        return database.<T>query()
                .select()
                .from(getTableName())
                .map(getRowMapper())
                .fetchAll();
    }

    public CompletableFuture<List<T>> findAllAsync() {
        return database.<T>query()
                .select()
                .from(getTableName())
                .map(getRowMapper())
                .fetchAllAsync();
    }

    public void deleteById(ID id) {
        database.<T>query()
                .deleteFrom(getTableName())
                .where(getIdColumn(), id)
                .execute();
    }

    public CompletableFuture<Integer> deleteByIdAsync(ID id) {
        return database.<T>query()
                .deleteFrom(getTableName())
                .where(getIdColumn(), id)
                .executeAsync();
    }

    public boolean exists(ID id) {
        return findById(id).isPresent();
    }

    protected <R> QueryBuilder<R> query() {
        return database.query();
    }
}