package dev.addition.ten.player;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.addition.ten.util.MojangUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
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
    private static final Cache<UUID, Boolean> nonExistentUUIDCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(1_000)
            .build();

    /**
     * Get a cached online player by Bukkit Player instance.
     * @param bukkitInsane the bukkit player instance
     * @return the cached PlayerRef
     */
    public static @NotNull PlayerRef getOnlinePlayer(Player bukkitInsane) {
        PlayerRef playerRef = getOnlinePlayer(bukkitInsane.getUniqueId());
        if (playerRef == null)
            throw new IllegalStateException("Failed to find cached playerRef for " + bukkitInsane.getName() + " (" + bukkitInsane.getUniqueId() + ")");

        return playerRef;
    }

    /**
     * Get a cached online player by UUID.
     * @param uuid the player's UUID
     * @return the cached PlayerRef or null if not found
     */
    public static @UnknownNullability PlayerRef getOnlinePlayer(UUID uuid) {
        return onlineCache.values().stream()
                .filter(player -> player.getUUID().equals(uuid))
                .findAny()
                .orElse(null);
    }

    /**
     * Get a cached player by name, checking online and offline caches. (All online players have a cached PlayerRef)
     * @param name the player's name
     * @return the cached PlayerRef or null if not found
     */
    public static @UnknownNullability PlayerRef getOnlinePlayer(String name) {
        return onlineCache.values().stream()
                .filter(player -> player.getName().equalsIgnoreCase(name))
                .findAny()
                .orElse(null);
    }

    /**
     * Get a cached player by UUID, checking online and offline caches. (All online players have a cached PlayerRef)
     * @param uuid the player's UUID
     * @return the cached PlayerRef or null if not found
     */
    public static @Nullable PlayerRef getCachedPlayer(UUID uuid) {
        return getCachedPlayers().stream()
                .filter(player -> player.getUUID().equals(uuid))
                .findAny()
                .orElse(null);
    }

    /**
     * Get a cached player by name, checking online and offline caches. (All online players have a cached PlayerRef)
     * @param name the player's name
     * @return the cached PlayerRef or null if not found
     */
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
     * Load a player by name asynchronously.
     * This handles the Mojang lookup and existence checks in one place.
     */
    public static CompletableFuture<Optional<PlayerRef>> loadPlayerAsync(String name) {
        PlayerRef cached = getCachedPlayer(name);
        if (cached != null) return CompletableFuture.completedFuture(Optional.of(cached));

        return MojangUtil.getUUIDFromName(name).thenCompose(uuid -> {
            if (uuid == null) return CompletableFuture.completedFuture(Optional.empty());
            return loadPlayerAsync(uuid);
        });
    }

    /**
     * Load a player asynchronously by UUID with optional name for cache invalidation.
     * @param uuid the player's UUID
     */
    public static CompletableFuture<Optional<PlayerRef>> loadPlayerAsync(UUID uuid) {
        PlayerRef cached = getCachedPlayer(uuid);
        if (cached != null) return CompletableFuture.completedFuture(Optional.of(cached));

        if (nonExistentUUIDCache.getIfPresent(uuid) != null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return PlayerDataRepository.INSTANCE.findByIdAsync(uuid.toString())
                .thenApply(optionalData -> {
                    if (optionalData.isPresent()) {
                        PlayerRef ref = new PlayerRef(optionalData.get());
                        offlineCache.put(uuid, ref);
                        return Optional.of(ref);
                    } else {
                        nonExistentUUIDCache.put(uuid, true); // Cache that they've never played
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
            nonExistentUUIDCache.invalidate(uuid);
            MojangUtil.invalidate(bukkitPlayer.getName());

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
                    nonExistentUUIDCache.invalidate(uuid);
                    MojangUtil.invalidate(bukkitPlayer.getName());

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

        return PlayerDataRepository.INSTANCE.saveAsync(ref.getData());
    }
}