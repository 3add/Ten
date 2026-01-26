package dev.addition.randomkits.util.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.addition.randomkits.player.PlayerDataRepository;
import dev.addition.randomkits.util.persistence.Repository;

import java.util.List;

public class RepositoryRegistry extends Registry<Repository<?, ?>> {

    public static final RepositoryRegistry INSTANCE = new RepositoryRegistry();
    private static final Logger log = LoggerFactory.getLogger(RepositoryRegistry.class);

    private RepositoryRegistry() {
        register(PlayerDataRepository.INSTANCE
        );
    }

    public void initializeAll() {
        List<Repository<?, ?>> repositories = getRegisteredItems();
        repositories.forEach(Repository::initTable);
        log.info("Initialized {} repositories", repositories.size());
    }
}
