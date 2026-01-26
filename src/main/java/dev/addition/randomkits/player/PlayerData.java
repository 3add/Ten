package dev.addition.randomkits.player;

import dev.addition.randomkits.economy.Wallet;

import java.time.Instant;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private final String name;
    private final Wallet wallet;
    private Instant lastSeen;

    public PlayerData(UUID uuid, String name, Wallet wallet, Instant lastSeen) {
        this.uuid = uuid;
        this.name = name;
        this.wallet = wallet;
        this.lastSeen = lastSeen;
    }

    public void updateLastSeen() {
        this.lastSeen = Instant.now();
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerData that)) return false;
        return getUUID().equals(that.getUUID());
    }

    @Override
    public int hashCode() {
        return getUUID().hashCode();
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", wallet=" + wallet +
                ", lastSeen=" + lastSeen +
                '}';
    }
}