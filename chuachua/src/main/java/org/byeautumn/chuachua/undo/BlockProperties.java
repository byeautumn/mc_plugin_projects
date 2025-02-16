package org.byeautumn.chuachua.undo;

import org.bukkit.Material;

public class BlockProperties {
    public final static String BLOCK_DATA_STRING_AIR = "minecraft:air";
    private final String blockDataString;
    public BlockProperties(String blockDataString) {
        this.blockDataString = blockDataString;
    }

    public String getBlockDataString() { return blockDataString; }
}
