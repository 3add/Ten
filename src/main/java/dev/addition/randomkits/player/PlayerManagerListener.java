package dev.addition.randomkits.player;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.addition.randomkits.RandomKits;

public class PlayerManagerListener implements Listener {

    private static final Logger log = LoggerFactory.getLogger(PlayerManagerListener.class);

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        PlayerManager.handleJoinAsync(player).thenAccept(playerRef ->
                log.info("Loaded player data for {} ({})", playerRef.getName(), playerRef.getUUID()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        PlayerManager.handleQuitAsync(player.getUniqueId()).thenRun(() ->
                log.info("Saved player data for {} ({})", player.getName(), player.getUniqueId()));
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (!event.getPlugin().equals(RandomKits.getInstance()))
            return;

        PlayerManager.saveCachedPlayersSync();
        log.info("Saved all player data for all players");
    }
}