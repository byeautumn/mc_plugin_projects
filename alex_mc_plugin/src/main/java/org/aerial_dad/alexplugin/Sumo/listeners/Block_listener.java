package org.aerial_dad.alexplugin.Sumo.listeners;

import org.aerial_dad.alexplugin.Sumo.common.DuelGame;
import org.aerial_dad.alexplugin.Sumo.common.Global;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class Block_listener implements Listener {
    private static double SUMO_GATE_LOCATION_X = 20.0;
    private static double SUMO_GATE_LOCATION_Y = 1.0;
    private static double SUMO_GATE_LOCATION_Z = 1.0;

    public void onBlockBreak(BlockBreakEvent event){

        Player player = event.getPlayer();
        Block block = event.getBlock();

        if( player.hasPermission( "permission.admin")) {
            player.sendMessage("Wow " + ChatColor.YELLOW + player.getDisplayName() + " you can break blocks!");

        }else{
            if(block == null){
                player.sendMessage(ChatColor.RED + "hmm we seem to be having issues try rejoining.");

            }else{
                Material material = block.getType();
                if(material != Material.BLUE_WOOL && material != Material.RED_WOOL) {
                    event.setCancelled(true);
                    player.sendMessage("You can only break player placed blocks");




                }
            }

        }


    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if(null == clickedBlock) return;

            if(clickedBlock.getType() == Material.OAK_SIGN) {
//                System.out.println("The oak sign location: " + clickedBlock.getLocation());
                Location location = clickedBlock.getLocation();
                if(location.getX() == SUMO_GATE_LOCATION_X &&
                    location.getY() == SUMO_GATE_LOCATION_Y &&
                    location.getZ() == SUMO_GATE_LOCATION_Z) {
                    Player player = event.getPlayer();
                    System.out.println("Player is trying to enter sumo world: " + player.getDisplayName());

                    DuelGame game = Global.getNextOpenGame();
                    System.out.println("The next open game: " + game);
                    if(game != null &&
                            !game.isFull() &&
                            game.getGameState() != DuelGame.DuelGameState.TERMINATION_SCHEDULED) {
                        System.out.println("The next game picked is " + game.getWorld().getName());
                        game.admit(player);
                    }
                    else {
                        System.err.println("Server is full, the play cannot be admitted to a game: " + player.getDisplayName());
                    }

                }

            }
        }
    }
}
