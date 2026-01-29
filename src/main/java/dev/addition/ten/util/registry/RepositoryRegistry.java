package dev.addition.ten.util.registry;

import dev.addition.ten.auction.AuctionListingRepository;
import dev.addition.ten.player.PlayerDataRepository;
import dev.addition.ten.util.ScheduleUtil;
import dev.addition.ten.util.persistence.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

// It should be noted that there is noticeable desync between the in-memory data and the persistent storage (a database) data,
// on server stop this desync is resolved, the server also syncs data every 5m (see below).
// This is true for all repositories on the server.
public class RepositoryRegistry extends Registry<Repository<?, ?>> {

    public static final RepositoryRegistry INSTANCE = new RepositoryRegistry();
    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryRegistry.class);

    private RepositoryRegistry() {
        register(
                PlayerDataRepository.INSTANCE,
                AuctionListingRepository.INSTANCE
        );
    }

    public void initializeAll() {
        long startTime = System.currentTimeMillis();

        List<Repository<?, ?>> repositories = getRegisteredItems();
        repositories.forEach(Repository::initTable);

        long endTime = System.currentTimeMillis();
        LOGGER.info("Initialized {} repositories' table in {} ms", repositories.size(), (endTime - (float) startTime));
    }

    public void loadRepositoryData() {
        long startTime = System.currentTimeMillis();

        List<Repository<?, ?>> repositories = getRegisteredItems();
        repositories.forEach(Repository::syncStorageToMemory);

        long endTime = System.currentTimeMillis();
        LOGGER.info("Loaded {} repositories synchronously in {} ms", repositories.size(), (endTime - (float) startTime));
    }

    public void saveRepositoryData() {
        long startTime = System.currentTimeMillis();

        List<Repository<?, ?>> repositories = getRegisteredItems();
        repositories.forEach(Repository::syncMemoryToStorage);

        long endTime = System.currentTimeMillis();
        LOGGER.info("Saved {} repositories synchronously in {} ms", repositories.size(), (endTime - (float) startTime));
    }

    /**
     * Starts the auto-save cycle.
     *
     * @param interval      The rest period after ALL repos are saved (e.g., 5)
     * @param localInterval The delay between each individual repo save (e.g., 30)
     * @param unit          The time unit (Minutes for interval, Seconds for local)
     */
    public void startAutoSyncingData(int interval, int localInterval, ScheduleUtil.Unit unit) {
        // Initial delay before starting the first cycle
        ScheduleUtil.scheduleAsync(() -> runDataSyncCycle(0, interval, localInterval, unit), interval, unit);
    }

    private void runDataSyncCycle(int repoIndex, int interval, int localInterval, ScheduleUtil.Unit unit) {
        List<Repository<?, ?>> repos = RepositoryRegistry.INSTANCE.getRegisteredItems();

        if (repoIndex >= repos.size()) {
            LOGGER.info("Data-sync cycle complete. Next data sync cycle in {} {}", interval, unit.name().toLowerCase() + "s");
            ScheduleUtil.scheduleAsync(() -> runDataSyncCycle(0, interval, localInterval, unit), interval, unit);
            return;
        }

        Repository<?, ?> currentRepo = repos.get(repoIndex);

        // Perform the save for the current repository
        ScheduleUtil.scheduleAsync(() -> {
            long startTime = System.currentTimeMillis();
            currentRepo.syncMemoryToStorage();
            long endTime = System.currentTimeMillis();
            LOGGER.info("Synced {}'s data in in {} ms", currentRepo.getClass().getSimpleName(), (endTime - startTime));

            // schedule the next repository save after localInterval
            runDataSyncCycle(repoIndex + 1, interval, localInterval, unit);
        });
    }
}
