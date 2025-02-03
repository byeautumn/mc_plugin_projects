package org.byeautumn.chuachua.undo;

import org.bukkit.block.Block;

public class ActionRunner {
    public static void undo(ActionRecord action) {
        if (action instanceof BlockPropertiesRecord) {
            final Block block = ((BlockPropertiesRecord) action).getBlock();
            block.setType(((BlockPropertiesRecord) action).getPreviousProperties().getMeterial());
        }
        else if (action instanceof GenerationRecord) {
            for (BlockPropertiesRecord stateRecord : ((GenerationRecord) action).getBlockPropertiesRecordMap().values()) {
                Block block = stateRecord.getBlock();
                block.setType(stateRecord.getPreviousProperties().getMeterial());
            }
        }
    }

    public static void redo(ActionRecord action) {
        if (action instanceof BlockPropertiesRecord) {
            final Block block = ((BlockPropertiesRecord) action).getBlock();
            block.setType(((BlockPropertiesRecord) action).getNextProperties().getMeterial());
        }
        else if (action instanceof GenerationRecord) {
            for (BlockPropertiesRecord stateRecord : ((GenerationRecord) action).getBlockPropertiesRecordMap().values()) {
                Block block = stateRecord.getBlock();
                block.setType(stateRecord.getNextProperties().getMeterial());
            }
        }
    }
}
