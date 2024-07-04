package org.aerial_dad.alexplugin.Bedwars.listeners;

import org.aerial_dad.alexplugin.AlexFirstPlugin;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;


public class Block_listener implements Listener {
    private final static String key = "playerplaced";
    private final static AlexFirstPlugin plugin = new AlexFirstPlugin();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Block block = event.getBlockPlaced();
        FixedMetadataValue metadataValue = new FixedMetadataValue(plugin, true);
        block.setMetadata(key, metadataValue);
        System.out.println("MetaData has been set to" + block);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        Block block = event.getBlock();
        if (!block.hasMetadata(key)){
            event.setCancelled(true);
        }

    }
}
