package org.byeautumn.chuachua.undo;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.byeautumn.chuachua.io.IOUntil;

public class ActionRunner {
    public static void undo(ActionRecord action, Player player) {
        if (action instanceof BlockPropertiesRecord) {
            final Block block = ((BlockPropertiesRecord) action).getBlock();
            IOUntil.updateBlockData(player, block, ((BlockPropertiesRecord) action).getPreviousProperties().getBlockDataString());
        }
        else if (action instanceof GenerationRecord) {
            for (BlockPropertiesRecord stateRecord : ((GenerationRecord) action).getBlockPropertiesRecordMap().values()) {
                Block block = stateRecord.getBlock();
                IOUntil.updateBlockData(player, block, stateRecord.getPreviousProperties().getBlockDataString());
            }
        }
    }

    public static void redo(ActionRecord action, Player player) {
        if (action instanceof BlockPropertiesRecord) {
            final Block block = ((BlockPropertiesRecord) action).getBlock();
            IOUntil.updateBlockData(player, block, ((BlockPropertiesRecord) action).getNextProperties().getBlockDataString());
        }
        else if (action instanceof GenerationRecord) {
            for (BlockPropertiesRecord stateRecord : ((GenerationRecord) action).getBlockPropertiesRecordMap().values()) {
                Block block = stateRecord.getBlock();
                IOUntil.updateBlockData(player, block, stateRecord.getNextProperties().getBlockDataString());
            }
        }
    }
}
