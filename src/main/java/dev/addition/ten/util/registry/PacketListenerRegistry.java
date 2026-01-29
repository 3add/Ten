package dev.addition.ten.util.registry;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import dev.addition.ten.qol.ping.PingManager;
import org.bukkit.event.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PacketListenerRegistry extends Registry<PacketListener> implements Listener {

    public static final PacketListenerRegistry INSTANCE = new PacketListenerRegistry();
    private static final Logger LOGGER = LoggerFactory.getLogger(PacketListenerRegistry.class);

    private PacketListenerRegistry() {
        register(
                PingManager.INSTANCE
        );
    }

    public void registerAll() {
        List<PacketListener> listeners = getRegisteredItems();
        listeners.forEach(listener -> PacketEvents.getAPI().getEventManager()
                .registerListener(listener, PacketListenerPriority.NORMAL));
        LOGGER.info("Registered {} packet listeners", listeners.size());
    }
}
