package dev.addition.randomkits.util.command;

import com.github.retrooper.packetevents.util.MojangAPIUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.addition.randomkits.player.PlayerManager;
import dev.addition.randomkits.player.PlayerRef;
import dev.addition.randomkits.util.ScheduleUtil;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PlayerRefArgument {

    private static final Cache<String, UUID> uuidCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(1000)
            .build();

    private static final Cache<String, Boolean> nonExistentPlayerCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(1000)
            .build();

    public static Argument<PlayerRef> onlinePlayerRefArgument(String nodeName) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            String inputName = info.input();
            if (!isValidUsername(inputName)) error(inputName + " is not a valid Minecraft username");

            PlayerRef playerRef = PlayerManager.getCachedPlayer(inputName);
            if (playerRef == null || !playerRef.isOnline()) error("Failed to find an online player named " + inputName);

            return playerRef;
        }).replaceSuggestions((info, builder) -> {
            PlayerManager.getOnlinePlayers().stream()
                    .map(PlayerRef::getName)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        });
    }

    /**
     * This will return a player name, use {@link PlayerRefArgument#resolvePlayerRef(String, Consumer)} to get the PlayerRef
     * This basically just checks if the input is a valid Minecraft username and suggests cached player names
     * @param nodeName the argument node name
     * @return the input player name
     */
    public static Argument<String> offlinePlayerRefArgument(String nodeName) {
        return new CustomArgument<>(new StringArgument(nodeName), info -> {
            String inputName = info.input();
            if (!isValidUsername(inputName)) error(inputName + " is not a valid Minecraft username");

            return inputName;
        }).replaceSuggestions((info, builder) -> {
            PlayerManager.getCachedPlayers().stream()
                    .map(PlayerRef::getName)
                    .forEach(builder::suggest);
            return builder.buildFuture();
        });
    }

    /**
     * Try to get a PlayerRef synchronously from cache only.
     * Does not perform any network or database lookups.
     *
     * @param name the player name
     * @return the PlayerRef if cached, null otherwise
     */
    public static @Nullable PlayerRef getCachedPlayerRef(String name) {
        return PlayerManager.getCachedPlayer(name);
    }

    /**
     * Resolve a PlayerRef with automatic sync/async handling.
     * If the player is cached, onComplete is called immediately (synchronously).
     * If not cached, performs async lookup and calls onComplete when done.
     * This is the RECOMMENDED method for best performance.
     *
     * @param name the player name
     * @param onComplete called with the PlayerRef (or null if not found)
     */
    public static void resolvePlayerRef(String name, Consumer<@Nullable PlayerRef> onComplete) {
        String lowerName = name.toLowerCase();

        // First check PlayerManager cache (this is fast and includes online players)
        PlayerRef cached = PlayerManager.getCachedPlayer(name);
        if (cached != null) {
            onComplete.accept(cached);
            return;
        }

        // Check if we know this player doesn't exist (after checking caches)
        if (nonExistentPlayerCache.getIfPresent(lowerName) != null) {
            onComplete.accept(null);
            return;
        }

        resolvePlayerRefAsync(name).thenAccept(onComplete);
    }

    private static CompletableFuture<@Nullable PlayerRef> resolvePlayerRefAsync(String name) {
        String lowerCaseName = name.toLowerCase();

        // Double-check cache in case it was populated while we were waiting
        PlayerRef cached = PlayerManager.getCachedPlayer(name);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return getUUIDFromName(name).thenCompose(uuid -> {
            if (uuid == null) {
                // Player doesn't exist in Mojang's system - cache this
                nonExistentPlayerCache.put(lowerCaseName, Boolean.TRUE);
                return CompletableFuture.completedFuture(null);
            }

            return PlayerManager.loadPlayerAsync(uuid)
                    .thenApply(opt -> {
                        if (opt.isEmpty()) {
                            // Player exists in Mojang but not in our storage, cache as non-existent
                            nonExistentPlayerCache.put(lowerCaseName, Boolean.TRUE);
                        }
                        return opt.orElse(null);
                    });
        });
    }

    private static boolean isValidUsername(String name) {
        if (name == null) return false;
        return name.matches("^[A-Za-z0-9_]{3,16}$");
    }

    private static CompletableFuture<@Nullable UUID> getUUIDFromName(String name) {
        UUID cached = uuidCache.getIfPresent(name.toLowerCase());
        if (cached != null) return CompletableFuture.completedFuture(cached);

        CompletableFuture<@Nullable UUID> future = new CompletableFuture<>();

        ScheduleUtil.scheduleAsync(() -> {
            try {
                UUID uuid = MojangAPIUtil.requestPlayerUUID(name);
                uuidCache.put(name.toLowerCase(), uuid);
                future.complete(uuid);
            } catch (IllegalStateException e) {
                future.complete(null);
            }
        });

        return future;
    }

    private static void error(String message) throws CustomArgument.CustomArgumentException {
        throw CustomArgument.CustomArgumentException.fromAdventureComponent(Component.text(message, NamedTextColor.RED));
    }
}