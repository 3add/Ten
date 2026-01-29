package dev.addition.ten.util;

import com.github.retrooper.packetevents.util.MojangAPIUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MojangUtil {

    private static final Cache<String, UUID> uuidCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(1000)
            .build();

    private static final Cache<String, Boolean> nonExistentNameCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .maximumSize(1000)
            .build();

    public static boolean isValidUsername(String name) {
        return name != null && name.matches("^[A-Za-z0-9_]{3,16}$");
    }

    public static CompletableFuture<@Nullable UUID> getUUIDFromName(String name) {
        String lowerName = name.toLowerCase();

        if (nonExistentNameCache.getIfPresent(lowerName) != null) {
            return CompletableFuture.completedFuture(null);
        }

        UUID cached = uuidCache.getIfPresent(lowerName);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        CompletableFuture<@Nullable UUID> future = new CompletableFuture<>();
        ScheduleUtil.scheduleAsync(() -> {
            try {
                UUID uuid = MojangAPIUtil.requestPlayerUUID(name);
                uuidCache.put(lowerName, uuid);
                future.complete(uuid);
            } catch (Throwable t) {
                nonExistentNameCache.put(lowerName, true);
                future.complete(null);
            }
        });

        return future;
    }

    public static void invalidate(String username) {
        String lowerName = username.toLowerCase();
        uuidCache.invalidate(lowerName);
        nonExistentNameCache.invalidate(lowerName);
    }
}