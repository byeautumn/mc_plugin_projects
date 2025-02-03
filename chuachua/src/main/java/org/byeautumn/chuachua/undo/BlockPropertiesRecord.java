package org.byeautumn.chuachua.undo;

import org.bukkit.block.Block;

public class BlockPropertiesRecord implements ActionRecord{
    private final Block block;
    private final BlockProperties previousProperties;
    private final BlockProperties nextProperties;

    public BlockPropertiesRecord(Block block, BlockProperties previousProperties, BlockProperties nextProperties) {
        this.block = block;
        this.previousProperties = previousProperties;
        this.nextProperties = nextProperties;
    }

    public Block getBlock() {
        return block;
    }

    public BlockProperties getPreviousProperties() {
        return previousProperties;
    }

    public BlockProperties getNextProperties() {
        return nextProperties;
    }
}
