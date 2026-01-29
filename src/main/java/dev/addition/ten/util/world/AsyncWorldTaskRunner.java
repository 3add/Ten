package dev.addition.ten.util.world;

import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public abstract class AsyncWorldTaskRunner {

    protected CompletableFuture<Integer> runSpatialTask(World world, Region totalRegion, int threadCount, SpatialWorldTask task) {
        BlockVector3 min = totalRegion.getMinimumPoint();
        BlockVector3 max = totalRegion.getMaximumPoint();

        int totalWidth = max.x() - min.x() + 1;
        int sliceWidth = (int) Math.ceil((double) totalWidth / threadCount);

        @SuppressWarnings("unchecked")
        CompletableFuture<Integer>[] futures = (CompletableFuture<Integer>[]) new CompletableFuture[threadCount];

        for (int i = 0; i < threadCount; i++) {
            int startX = min.x() + (i * sliceWidth);
            int endX = Math.min(startX + sliceWidth - 1, max.x());

            CompletableFuture<Integer> sliceFuture = new CompletableFuture<>();
            futures[i] = sliceFuture;

            if (startX > max.x()) {
                sliceFuture.complete(null);
                continue;
            }

            CuboidRegion subRegion = new CuboidRegion(
                    world,
                    BlockVector3.at(startX, min.y(), min.z()),
                    BlockVector3.at(endX, max.y(), max.z())
            );

            TaskManager.taskManager().async(() -> {
                try (EditSession session = WorldEdit.getInstance().newEditSession(world)) {
                    session.setMask(new RegionMask(subRegion));
                    task.execute(session, subRegion);
                    session.flushQueue();

                    sliceFuture.complete(session.getBlockChangeCount());
                } catch (Throwable t) {
                    sliceFuture.completeExceptionally(t);
                }
            });
        }

        return CompletableFuture.allOf(futures)
                .thenApply(v -> Arrays.stream(futures)
                                .map(CompletableFuture::join)
                                .filter(java.util.Objects::nonNull)
                                .mapToInt(Integer::intValue)
                                .sum());
    }
}