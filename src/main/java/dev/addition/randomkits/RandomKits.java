package dev.addition.randomkits;

import dev.addition.randomkits.util.registry.CommandRegistry;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.addition.randomkits.util.persistence.MySQLDataBase;
import dev.addition.randomkits.util.registry.ListenerRegistry;
import dev.addition.randomkits.util.registry.RepositoryRegistry;

public final class RandomKits extends JavaPlugin {

    private static final Logger log = LoggerFactory.getLogger(RandomKits.class);
    private static RandomKits instance;
    private static MySQLDataBase dataBase;

    @Override
    public void onEnable() {
        log.info("Starting up RandomKits plugin...");
        long startTime = System.nanoTime();

        RandomKits.instance = this;

        // If connecting failed, disable plugin
        if (!connectDataBase()) getServer().getPluginManager().disablePlugin(this);

        ListenerRegistry.INSTANCE.registerAll(this);
        RepositoryRegistry.INSTANCE.initializeAll();
        CommandRegistry.INSTANCE.registerAll();

        double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000D;
        log.info("RandomKits plugin started in {}s", elapsedTime);
    }

    @Override
    public void onDisable() {
        log.info("Shutting down RandomKits plugin...");
        long startTime = System.nanoTime();

        RandomKits.dataBase.disconnect();

        double elapsedTime = (System.nanoTime() - startTime) / 1_000_000_000D;
        log.info("RandomKits plugin shut down in {}s", elapsedTime);
    }

    private boolean connectDataBase() {
        RandomKits.dataBase = new MySQLDataBase("185.207.164.130:1025", "s7222_RandomKits", "u7222_iuorE1Kflc", "coWnI!@RYq.sUDgQ2=9^V69b");
        try {
            RandomKits.dataBase.connect();
        } catch (Throwable t) {
            log.error("Couldn't connect to the database or init tables", t);
            getServer().getPluginManager().disablePlugin(this);

            return false; // Failed
        }

        log.info("Successfully connected to the database");
        return true; // Success
    }

    public static RandomKits getInstance() {
        return RandomKits.instance;
    }

    public static MySQLDataBase getDataBase() {
        return RandomKits.dataBase;
    }
}
