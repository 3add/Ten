package dev.addition.ten.player;

import dev.addition.ten.economy.Wallet;
import dev.addition.ten.util.ScheduleUtil;
import dev.addition.ten.util.persistence.BatchQueryBuilder;
import dev.addition.ten.util.persistence.Repository;
import dev.addition.ten.util.persistence.RowMapper;

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
                .insertInto(getTableName(), "uuid", "name", "wallet", "last_seen")
                .values(
                        data.getUUID().toString(),
                        data.getName(),
                        data.getWallet().toJson(),
                        data.getLastSeen().toEpochMilli()
                )
                // If the UUID already exists, update these fields instead
                .onDuplicateKeyUpdate("name", "wallet", "last_seen")
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

    @Override
    public void syncMemoryToStorage() {
        BatchQueryBuilder batch = database.batch()
                .insertInto(getTableName(), "uuid", "name", "wallet", "last_seen");

        for (PlayerRef ref : PlayerManager.getCachedPlayers()) {
            PlayerData data = ref.getData();
            batch.addBatch(
                    data.getUUID().toString(),
                    data.getName(),
                    data.getWallet().toJson(),
                    data.getLastSeen().toEpochMilli()
            );
        }

        batch.onDuplicateKeyUpdate("name", "wallet", "last_seen")
                .execute();
    }
}