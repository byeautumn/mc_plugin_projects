package org.byeautumn.chuachua.generate.world.pipeline.tree;

import org.bukkit.Location;

public interface TreeGenerator {
    /**
     * Generates a tree at the given location based on the provided configuration.
     *
     * @param location The base location (typically ground level) where the tree should start.
     * @param configure The configuration parameters for tree generation.
     * @return true if the tree was successfully generated, false otherwise (e.g., no space).
     */
    boolean generate(Location location, TreeGenerationConfigure configure);
}
