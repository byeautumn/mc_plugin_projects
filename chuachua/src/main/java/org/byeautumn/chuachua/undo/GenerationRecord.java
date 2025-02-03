package org.byeautumn.chuachua.undo;

import org.bukkit.block.Block;
import org.byeautumn.chuachua.common.BlockUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenerationRecord implements ActionRecord{
    private final Map<String, BlockPropertiesRecord> blockPropertiesRecordMap = new HashMap<>();

    private void buildBlockPropertiesRecords(List<Block> blocks, BlockProperties previous, BlockProperties next) {
        for(Block block : blocks) {
            String key = BlockUtil.getBlockMapKey(block);
            if(!this.blockPropertiesRecordMap.containsKey(key)) {
                BlockPropertiesRecord blockPropertiesRecord = new BlockPropertiesRecord(block, previous, next);
                this.blockPropertiesRecordMap.put(key, blockPropertiesRecord);
            }
        }
    }

    public Map<String, BlockPropertiesRecord> getBlockPropertiesRecordMap() {
        return blockPropertiesRecordMap;
    }

    public void addBlockPropertiesRecord(BlockPropertiesRecord record) {
        Block block = record.getBlock();
        String key = BlockUtil.getBlockMapKey(block);
        if(!this.blockPropertiesRecordMap.containsKey(key)) {
            this.blockPropertiesRecordMap.put(key, record);
        }
    }
}
