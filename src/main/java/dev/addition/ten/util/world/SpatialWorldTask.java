package dev.addition.ten.util.world;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.regions.Region;

@FunctionalInterface
public interface SpatialWorldTask {

    /**
     * @param session The session for this thread.
     * @param subRegion The specific chunk of the world this thread must handle.
     */
    void execute(EditSession session, Region subRegion);
}