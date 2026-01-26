package dev.addition.randomkits.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.UnknownNullability;
import dev.addition.randomkits.economy.Wallet;

import java.time.Instant;
import java.util.UUID;

public class PlayerRef {

    private final PlayerData data;
    private boolean isOnline;

    /**
     * Creates a new PlayerRef for a player who is joining for the first time.
     * @param bukkitPlayer The Bukkit Player object.
     */
    public PlayerRef(Player bukkitPlayer) {
        this.data = new PlayerData(
                bukkitPlayer.getUniqueId(),
                bukkitPlayer.getName(),
                new Wallet(),
                Instant.now()
        );

        this.isOnline = true;
    }

    /**
     * Creates a new PlayerRef with existing PlayerData.
     * @param data The PlayerData object.
     */
    public PlayerRef(PlayerData data) {
        this.data = data;
        this.isOnline = false;
    }

    public PlayerData getData() {
        return data;
    }

    public UUID getUUID() {
        return data.getUUID();
    }

    public String getName() {
        return data.getName();
    }

    public boolean isOnline() {
        return isOnline;
    }

    void setOnline(boolean online) {
        this.isOnline = online;
    }

    private @UnknownNullability Player toBukkitPlayer() {
        if (!isOnline) return null;
        return Bukkit.getPlayer(data.getUUID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerRef player)) return false;
        return data.equals(player.getData());
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PlayerRef{" +
                "data=" + data.toString() +
                ", isOnline=" + isOnline +
                '}';
    }
}