package org.aerial_dad.noodlelegs;

import org.aerial_dad.noodlelegs.game.*;
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

import java.util.List;

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
        if (block.hasMetadata(key)){
            player.sendMessage( player + " has meta Data!!!" + key);
        }
        if (Bw_general.bwMode != Bw_general.BW_Mode.EDIT) {
            if (!block.hasMetadata(key)) {
                boolean isBedBlock = false;
                PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                Game game = playerTracker.getCurrentGame();
                if (null != game && game.getStatus() == GameStatus.INGAME) {
                    List<Block> bedBlocks = game.getBedBlocks();
                    for (Block bed : bedBlocks) {
                        if (Universe.areBlocksSame(bed, block)) {
                            isBedBlock = true;
                        }
                    }
                }
                if (isBedBlock) {
                    Team team = playerTracker.getCurrentTeam();
                    if (team != null) {
                        if (Universe.areBlocksSame(team.getBed(), block)) {
                            System.out.println("Player '" + player.getDisplayName() + "' is trying to hit their own bed.");
                            player.sendMessage("You cannot break your own bed.");
                        } else {
                            for (Team otherTeam : game.getTeams()) {
                                Block bed = otherTeam.getBed();
                                if (Universe.areBlocksSame(bed, block)) {
                                    otherTeam.reportBedBroken();
                                    System.out.println("The bed of '" + otherTeam.getName() + "' is destroyed.");
                                } else {
                                    otherTeam.displayTitle("The bed of '" + otherTeam.getName() + "' is destroyed.", "");
                                }
                            }
                            return;
                        }
                    } else {
                        System.err.println("Team is null when game status is INGAME.");
                    }
                }
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
