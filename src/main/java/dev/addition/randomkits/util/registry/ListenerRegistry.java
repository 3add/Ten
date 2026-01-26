package dev.addition.randomkits.util.registry;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.addition.randomkits.RandomKits;
import dev.addition.randomkits.player.PlayerManagerListener;

import java.util.List;

public class ListenerRegistry extends Registry<Listener> {

    public static final ListenerRegistry INSTANCE = new ListenerRegistry();
    private static final Logger log = LoggerFactory.getLogger(ListenerRegistry.class);

    private ListenerRegistry() {
        register(new PlayerManagerListener()
        );
    }

    public void registerAll(RandomKits plugin) {
        List<Listener> listeners = getRegisteredItems();
        listeners.forEach(listener -> Bukkit.getServer().getPluginManager().registerEvents(listener, plugin));
        log.info("Registered {} listeners", listeners.size());
    }
}
