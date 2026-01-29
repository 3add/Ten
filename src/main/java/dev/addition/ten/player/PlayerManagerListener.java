package dev.addition.ten.player;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerManagerListener implements Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerManagerListener.class);

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        PlayerManager.handleJoinAsync(player).thenAccept(playerRef ->
                LOGGER.info("Loaded player data for {} ({})", playerRef.getName(), playerRef.getUUID()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        PlayerManager.handleQuitAsync(player.getUniqueId()).thenRun(() ->
                LOGGER.info("Saved player data for {} ({})", player.getName(), player.getUniqueId()));
    }
}