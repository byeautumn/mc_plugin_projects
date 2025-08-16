package org.byeautumn.chuachua.generate.world.pipeline.tree;

import org.bukkit.Material;

/**
 * Interface for common configuration parameters across different tree generation algorithms.
 */
public interface TreeGenerationConfigure {
    /**
     * Gets the Material to be used for the tree's log/trunk.
     * @return The log Material.
     */
    Material getLogMaterial();

    /**
     * Gets the Material to be used for the tree's leaves.
     * @return The leaf Material.
     */
    Material getLeafMaterial();

    // You can add more common configuration methods here if they apply to all tree types
    // For example:
    // int getMinHeight();
    // int getMaxHeight();
}
