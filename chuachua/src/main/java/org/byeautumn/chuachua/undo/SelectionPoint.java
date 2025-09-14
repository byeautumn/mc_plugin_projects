package org.byeautumn.chuachua.undo;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.UUID;

public class SelectionPoint {
    private final UUID worldUUID;
    private final int x;
    private final int y;
    private final int z;

    public SelectionPoint(Block block) {
        this.worldUUID = block.getWorld().getUID();
        this.x = block.getX();
        this.y = block.getY();
        this.z = block.getZ();
    }

    public Block getBlock() {
        World world = Bukkit.getWorld(worldUUID);
        if (world == null) {
            return null;
        }
        return world.getBlockAt(x, y, z);
    }
}
