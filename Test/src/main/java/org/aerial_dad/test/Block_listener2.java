package org.aerial_dad.test;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.metadata.FixedMetadataValue;


public class Block_listener2 implements Listener {

    private final static String key = "playerplaced";
    private final Test plugin;

    public Block_listener2(Test plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){

        Block block = event.getBlockPlaced();
        FixedMetadataValue metadataValue = new FixedMetadataValue(plugin, true);
        if (Bw_general.bwMode != Bw_general.BW_Mode.EDIT){
            block.setMetadata(key, metadataValue);
            System.out.println("MetaData has been set to " + block.getType());
        }
//        Player player = event.getPlayer();
//        if (Bw_general.bwMode != Bw_general.BW_Mode.EDIT){
//
////        plugin.getConfig().get("Bottom of map: ");
////        World world = block.getWorld();
////        Location location = player.getLocation();
////        if (Bw_general.BW_CREATED_MAPS.containsKey(world.getName())){
////            if (isInRange(location) == false);
//            event.setCancelled(true);
//            player.sendMessage("You cannot place blocks beyond this point. ");
//        }




        }




    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        player.sendMessage("You broke a block!!!!");
        if (block.hasMetadata(key)){
            player.sendMessage( player + " has meta Data!!!" + key);
        }
        if (Bw_general.bwMode != Bw_general.BW_Mode.EDIT) {
                if (!block.hasMetadata(key)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You can not break this block");
                    System.out.println("This block is NOT player-placed so operation canceled. " + block.getType());
                    System.out.println(player.getDisplayName() + "is trying to break blocks");
                    System.out.println(player.getName());
                }



        }


    }


}
