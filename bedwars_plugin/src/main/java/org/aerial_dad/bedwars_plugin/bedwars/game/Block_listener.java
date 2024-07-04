package org.aerial_dad.bedwars_plugin.bedwars.game;

import org.aerial_dad.bedwars_plugin.bedwars.commands.Bw_general;
import org.aerial_dad.bedwars_plugin.bedwars.game.Constants.PerimeterConstant;
import org.aerial_dad.bedwars_plugin.Bedwars_plugin;
import org.aerial_dad.bedwars_plugin.bedwars.listeners.Op_listener;
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


public class Block_listener implements Listener {

    private final static String key = "playerplaced";
    private final Bedwars_plugin plugin;

    public Block_listener(Bedwars_plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){

        Block block = event.getBlockPlaced();
        FixedMetadataValue metadataValue = new FixedMetadataValue(plugin, true);
        if (Bw_general.bwMode != Bw_general.BW_Mode.EDIT){
            block.setMetadata(key, metadataValue);
//            System.out.println("MetaData has been set to " + block.getType());
        }
        Player player = event.getPlayer();
        if (Bw_general.bwMode != Bw_general.BW_Mode.EDIT){

        plugin.getConfig().get("Bottom of map: ");
        World world = block.getWorld();
        Location location = player.getLocation();
        if (Bw_general.BW_CREATED_MAPS.containsKey(world.getName())){
            if (isInRange(location) == false);
            event.setCancelled(true);
            player.sendMessage("You cannot place blocks beyond this point. ");
        }




        }



    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event){
        Player player = event.getPlayer();
        Block block = event.getBlock();
        GameMode gameMode = player.getGameMode();
        if (Bw_general.bwMode != Bw_general.BW_Mode.EDIT){
            if (gameMode == GameMode.SURVIVAL ||
                    !Op_listener.ops.contains(player.getName()) ){
                if (!block.hasMetadata(key)){
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "You can not break this block");
//                    System.out.println("This block is NOT player-placed so operation canceled. " + block.getType());
//                    System.out.println(player.getDisplayName() + "is trying to break blocks");
//                    System.out.println(player.getName());
                }

            }


        }



    }
private boolean isInRange(Location location) {
    if (location.getX() >= PerimeterConstant.RANGE_LIMIT_X ||
            location.getY() >= PerimeterConstant.RANGE_LIMIT_Y ||
            location.getZ() >= PerimeterConstant.RANGE_LIMIT_Z ||
            location.getX() >= PerimeterConstant.RANGE_LIMIT_X ||
            location.getY() <= -PerimeterConstant.RANGE_LIMIT_Y ||
            location.getZ() <= -PerimeterConstant.RANGE_LIMIT_Z ||
            location.getY() <= -PerimeterConstant.RANGE_LIMIT_Y) {
        return false;
    }
    return true;
}


}
