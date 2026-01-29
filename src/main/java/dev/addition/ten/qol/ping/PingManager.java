package dev.addition.ten.qol.ping;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import dev.addition.ten.player.PlayerRef;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PingManager implements PacketListener {

    public static final PingManager INSTANCE = new PingManager();

    private PingManager() {}

    private final Cache<UUID, Long> sentTimes = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .maximumSize(1_000)
            .build();
    private final Cache<UUID, Double> lastPingMs = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .maximumSize(1_000)
            .build();

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.KEEP_ALIVE)
            return;

        UUID uuid = event.getUser().getUUID();

        long now = System.nanoTime();
        sentTimes.put(uuid, now);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.KEEP_ALIVE)
            return;

        UUID uuid = event.getUser().getUUID();

        Long sent = sentTimes.getIfPresent(uuid);
        if (sent == null)
            return;

        double pingMs = (System.nanoTime() - sent) / 1_000_000D;
        lastPingMs.put(uuid, pingMs);
    }

    public double getPing(PlayerRef player) {
        Double ping = lastPingMs.getIfPresent(player.getUUID());
        return ping != null ? ping : -1D;
    }
}
