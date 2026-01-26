package dev.addition.randomkits.player;

import dev.addition.randomkits.economy.Wallet;
import dev.addition.randomkits.util.ScheduleUtil;
import dev.addition.randomkits.util.persistence.Repository;
import dev.addition.randomkits.util.persistence.RowMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// Stringified UUID as primary key
public class PlayerDataRepository extends Repository<PlayerData, String> {

    public final static PlayerDataRepository INSTANCE = new PlayerDataRepository();

    @Override
    protected String getTableName() {
        return "player_data";
    }

    @Override
    protected String getIdColumn() {
        return "uuid";
    }

    @Override
    protected RowMapper<PlayerData> getRowMapper() {
        return rs -> new PlayerData(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("name"),
                Wallet.fromJson(rs.getString("wallet")),
                Instant.ofEpochMilli(rs.getLong("last_seen"))
        );
    }

    @Override
    protected void createTable(Connection conn) throws SQLException {
        conn.createStatement().execute("""
            CREATE TABLE IF NOT EXISTS player_data (
                uuid VARCHAR(36) PRIMARY KEY,
                name VARCHAR(16) NOT NULL,
                wallet TEXT NOT NULL,
                last_seen BIGINT DEFAULT 0
            )
        """);
    }

    public void save(PlayerData data) {
        database.<PlayerData>query()
                .insertInto("player_data", "uuid", "name", "wallet", "last_seen")
                .values(
                        data.getUUID().toString(),
                        data.getName(),
                        data.getWallet().toJson(), // Serialize wallet to JSON
                        data.getLastSeen().toEpochMilli()
                )
                .execute();
    }

    public CompletableFuture<Void> saveAsync(PlayerData data) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ScheduleUtil.scheduleAsync(() -> {
            save(data);
            future.complete(null);
        });

        return future;
    }

    public void update(PlayerData data) {
        database.<PlayerData>query()
                .update(getTableName())
                .set("name", data.getName())
                .set("wallet", data.getWallet().toJson()) // Serialize on update too
                .set("last_seen", data.getLastSeen().toEpochMilli())
                .where("uuid", data.getUUID().toString())
                .execute();
    }

    public CompletableFuture<Void> updateAsync(PlayerData data) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        ScheduleUtil.scheduleAsync(() -> {
            update(data);
            future.complete(null);
        });

        return future;
    }
}