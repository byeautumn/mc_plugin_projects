package org.aerial_dad.noodlelegs;

import org.aerial_dad.noodlelegs.game.GameManager;
import org.aerial_dad.noodlelegs.game.GameType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

import static de.tr7zw.nbtapi.NBTType.NBTTagCompound;


public class Block_listener implements Listener {

    private final static String key = "playerplaced";
    private final NoodleLegs plugin;

    public Block_listener(NoodleLegs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){

        Block block = event.getBlockPlaced();  //NBTTagCompound blockNBT =
        World currentWorld = block.getWorld();
        Universe.markPlayerPlacedBlock(currentWorld, block);

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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if(null == clickedBlock) return;

            if(clickedBlock.getType() == Material.STONE) {
//                System.out.println("The oak sign location: " + clickedBlock.getLocation());
                Location location = clickedBlock.getLocation();
                if(location.getX() == 0 &&
                        location.getY() == 68 &&
                        location.getZ() == 0) {
                    Player player = event.getPlayer();
//                    Location newWorldLocation  = new Location(Bukkit.getWorld("orchestra"), 0, 119, 0 );
//                    player.teleport(newWorldLocation);
                    GameType type = GameType.BW_1V1;
                    System.out.println("Accept player " + player.getDisplayName() + " to the game " + type.name() + ".");
                    GameManager.getInstance().queuePlayer(type, player);

                }

            }
        }
    }



}
