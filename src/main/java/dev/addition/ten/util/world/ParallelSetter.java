package dev.addition.ten.util.world;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

import java.util.concurrent.CompletableFuture;

public class ParallelSetter extends AsyncWorldTaskRunner {

    public static final ParallelSetter INSTANCE = new ParallelSetter();

    private ParallelSetter() {}

    public CompletableFuture<Integer> setRegion(World world, Region region, Pattern pattern, int threads) {
        return runSpatialTask(world, region, threads, (session, subRegion) -> {
            try {
                session.setBlocks(subRegion, pattern);
            } catch (WorldEditException e) {
                throw new IllegalStateException("Failed to set blocks in parallel slice.", e);
            }
        });
    }
}