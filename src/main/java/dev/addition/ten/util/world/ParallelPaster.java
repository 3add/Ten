package dev.addition.ten.util.world;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;

import java.util.concurrent.CompletableFuture;

public class ParallelPaster extends AsyncWorldTaskRunner {

    public static final ParallelPaster INSTANCE = new ParallelPaster();

    private ParallelPaster() {}

    public CompletableFuture<Integer> pasteLargeSchematic(World world, Clipboard clipboard, BlockVector3 location, int threads) {
        Region pasteRegion = clipboard.getRegion().clone();
        BlockVector3 offset = location.subtract(clipboard.getOrigin());
        pasteRegion.shift(offset);

        return runSpatialTask(world, pasteRegion, threads, (session, subRegion) -> {
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(session)
                    .to(location)
                    .ignoreAirBlocks(true)
                    .build();

            try {
                Operations.complete(operation);
            } catch (WorldEditException e) {
                throw new IllegalStateException("Failed to paste schematic in parallel.", e);
            }
        });
    }
}