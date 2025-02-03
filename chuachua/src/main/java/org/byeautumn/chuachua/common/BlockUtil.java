package org.byeautumn.chuachua.common;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.List;

public class BlockUtil {
    public static String getBlockMapKey(Block block) {
        if (null == block) {
            System.err.println("The given Block is null -- BlockUtil::getBlockMapKey.");
            return "";
        }

        StringBuffer sb = new StringBuffer();
        sb.append(block.getWorld().getName()).append(":");
        sb.append(block.getX()).append(",").append(block.getY()).append(",").append(block.getZ());
        return sb.toString();
    }

    public static List<Block> getBlocksBetweenBlocks(Block block1, Block block2) {
        Location loc1 = block1.getLocation();
        Location loc2 = block2.getLocation();

        return LocationUtil.getBlocksBetweenLocations(loc1, loc2);
    }
}
