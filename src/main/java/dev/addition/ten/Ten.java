package dev.addition.ten;

import dev.addition.ten.util.ScheduleUtil;
import dev.addition.ten.util.registry.CommandRegistry;
import dev.addition.ten.util.registry.PacketListenerRegistry;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.addition.ten.util.persistence.MySQLDataBase;
import dev.addition.ten.util.registry.ListenerRegistry;
import dev.addition.ten.util.registry.RepositoryRegistry;

public final class Ten extends JavaPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(Ten.class);
    private static Ten INSTANCE;
    private static MySQLDataBase DATA_BASE;

    @Override
    public void onEnable() {
        LOGGER.info("Starting up Ten plugin...");
        long startTime = System.nanoTime();

        Ten.INSTANCE = this;

        // If connecting failed, disable plugin
        if (!connectDataBase()) getServer().getPluginManager().disablePlugin(this);

        // Load Repositories' (that have implemented this method) data synchronously
        RepositoryRegistry.INSTANCE.loadRepositoryData();

        // Register Commands
        CommandRegistry.INSTANCE.registerAll(this);

        // Register Bukkit Listeners
        ListenerRegistry.INSTANCE.registerAll(this);

        // Register PacketEvents Packet Listeners
        PacketListenerRegistry.INSTANCE.registerAll();

        // Initialize Repositories and start auto-saving
        RepositoryRegistry.INSTANCE.initializeAll();
        RepositoryRegistry.INSTANCE.startAutoSyncingData(60 * 5, 30, ScheduleUtil.Unit.SECOND);

        double elapsedTime = (System.nanoTime() - startTime) / 1_000_000F;
        LOGGER.info("Ten plugin started in {}ms", elapsedTime);
    }

    @Override
    public void onDisable() {
        LOGGER.info("Shutting down Ten...");
        long startTime = System.nanoTime();

        RepositoryRegistry.INSTANCE.saveRepositoryData();

        // After all data is saved, disconnect from the database
        Ten.DATA_BASE.disconnect();

        double elapsedTime = (System.nanoTime() - startTime) / 1_000_000F;
        LOGGER.info("Ten plugin shut down in {}ms", elapsedTime);
    }

    private boolean connectDataBase() {
        Ten.DATA_BASE = new MySQLDataBase("185.207.164.130:1025", "s7222_RandomKits", "u7222_iuorE1Kflc", "coWnI!@RYq.sUDgQ2=9^V69b");
        try {
            Ten.DATA_BASE.connect();
        } catch (Throwable t) {
            LOGGER.error("Couldn't connect to the database or init tables", t);
            getServer().getPluginManager().disablePlugin(this);

            return false; // Failed
        }

        LOGGER.info("Successfully connected to the database");
        return true; // Success
    }

    public static Ten getInstance() {
        return Ten.INSTANCE;
    }

    public static MySQLDataBase getDATA_BASe() {
        return Ten.DATA_BASE;
    }
}
