package dev.addition.randomkits.player;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class PlayerManager {

    private static final Map<UUID, PlayerRef> onlineCache = new ConcurrentHashMap<>();

    // Caches recent players that left and recent looked up offline players
    private static final Cache<UUID, PlayerRef> offlineCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .maximumSize(1_000)
            .build();

    // Caches UUIDs of non-existent players to avoid repeated database lookups
    private static final Cache<UUID, Boolean> nonExistentCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(1_000)
            .build();

    public static @Nullable PlayerRef getCachedPlayer(UUID uuid) {
        PlayerRef online = onlineCache.get(uuid);
        if (online != null) return online;

        return offlineCache.getIfPresent(uuid);
    }

    public static @Nullable PlayerRef getCachedPlayer(String name) {
        return getCachedPlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(name))
                .findAny()
                .orElse(null);
    }

    public static @UnmodifiableView Collection<PlayerRef> getCachedPlayers() {
        return Stream.concat(
                onlineCache.values().stream(),
                offlineCache.asMap().values().stream()
        ).toList();
    }

    public static @UnmodifiableView Collection<PlayerRef> getOnlinePlayers() {
        return onlineCache.values();
    }

    /**
     * Load a player asynchronously by UUID with optional name for cache invalidation.
     * @param uuid the player's UUID
     */
    public static CompletableFuture<Optional<PlayerRef>> loadPlayerAsync(UUID uuid) {
        // Check if player exists in either cache
        PlayerRef cached = getCachedPlayer(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        // Check if we know this player doesn't exist
        if (nonExistentCache.getIfPresent(uuid) != null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        // Load from database
        return PlayerDataRepository.INSTANCE.findByIdAsync(uuid.toString())
                .thenApply(optionalData -> {
                    if (optionalData.isPresent()) {
                        PlayerRef ref = new PlayerRef(optionalData.get());
                        offlineCache.put(uuid, ref);
                        return Optional.of(ref);
                    } else {
                        // Mark as non-existent to avoid future DB lookups
                        nonExistentCache.put(uuid, Boolean.TRUE);
                        return Optional.empty();
                    }
                });
    }

    static CompletableFuture<PlayerRef> handleJoinAsync(Player bukkitPlayer) {
        UUID uuid = bukkitPlayer.getUniqueId();

        // Check if already in online cache
        PlayerRef existing = onlineCache.get(uuid);
        if (existing != null) {
            existing.setOnline(true);
            return CompletableFuture.completedFuture(existing);
        }

        // Check offline cache
        existing = offlineCache.getIfPresent(uuid);
        if (existing != null) {
            existing.setOnline(true);
            offlineCache.invalidate(uuid); // Remove from offline cache
            onlineCache.put(uuid, existing); // Move to online cache

            // Remove from non-existent cache if present (shouldn't happen, but safety)
            nonExistentCache.invalidate(uuid);

            return CompletableFuture.completedFuture(existing);
        }

        // Load from database
        return PlayerDataRepository.INSTANCE.findByIdAsync(uuid.toString())
                .thenCompose(optionalData -> {
                    PlayerRef ref = optionalData
                            .map(PlayerRef::new)
                            .orElseGet(() -> new PlayerRef(bukkitPlayer));

                    ref.setOnline(true);
                    onlineCache.put(uuid, ref);

                    // Remove from non-existent cache since player now exists
                    nonExistentCache.invalidate(uuid);

                    if (optionalData.isEmpty()) {
                        // First time join - save to database
                        return PlayerDataRepository.INSTANCE.saveAsync(ref.getData())
                                .thenApply(v -> ref);
                    }

                    return CompletableFuture.completedFuture(ref);
                });
    }

    static CompletableFuture<Void> handleQuitAsync(UUID uuid) {
        PlayerRef ref = onlineCache.remove(uuid); // Remove from online cache
        if (ref == null) return CompletableFuture.completedFuture(null);

        ref.setOnline(false);
        ref.getData().updateLastSeen();

        // Move to offline cache for future lookups
        offlineCache.put(uuid, ref);

        return PlayerDataRepository.INSTANCE.updateAsync(ref.getData());
    }

    static void saveCachedPlayersSync() {
        // Save online players

        for (UUID uuid : onlineCache.keySet()) {
            PlayerRef ref = onlineCache.get(uuid);
            if (ref != null) {
                ref.getData().updateLastSeen();
                PlayerDataRepository.INSTANCE.update(ref.getData());
            }
        }

        // Save offline cached players
        for (PlayerRef ref : offlineCache.asMap().values()) {
            PlayerDataRepository.INSTANCE.update(ref.getData());
        }

        onlineCache.clear();
        offlineCache.invalidateAll();
    }
}