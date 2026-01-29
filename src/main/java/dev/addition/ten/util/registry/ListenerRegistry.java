package dev.addition.ten.util.registry;

import dev.addition.ten.util.inventory.CustomInventoryListener;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.addition.ten.Ten;
import dev.addition.ten.player.PlayerManagerListener;

import java.util.List;

public class ListenerRegistry extends Registry<Listener> {

    public static final ListenerRegistry INSTANCE = new ListenerRegistry();
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerRegistry.class);

    private ListenerRegistry() {
        register(
                new PlayerManagerListener(),
                new CustomInventoryListener()
        );
    }

    public void registerAll(Ten plugin) {
        List<Listener> listeners = getRegisteredItems();
        listeners.forEach(listener -> Bukkit.getServer().getPluginManager()
                .registerEvents(listener, plugin));
        LOGGER.info("Registered {} listeners", listeners.size());
    }
}
