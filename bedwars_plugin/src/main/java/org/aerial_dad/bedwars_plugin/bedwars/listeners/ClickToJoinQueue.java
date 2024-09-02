package org.aerial_dad.bedwars_plugin.bedwars.listeners;

import org.aerial_dad.bedwars_plugin.bedwars.commands.Bw_general;
import org.aerial_dad.bedwars_plugin.bedwars.game.BwGame;
import org.aerial_dad.bedwars_plugin.bedwars.game.BwPlayer;
import org.aerial_dad.bedwars_plugin.bedwars.game.BwWorld;
import org.aerial_dad.bedwars_plugin.bedwars.game.Constants.GameConfig;
import org.aerial_dad.bedwars_plugin.bedwars.game.Constants.GateConstant;
import org.aerial_dad.bedwars_plugin.bedwars.game.GameManager;
import org.aerial_dad.bedwars_plugin.bedwars.game.Teams.BwTeam;
import org.aerial_dad.bedwars_plugin.bedwars.game.Teams.JoinQueue;
import org.aerial_dad.bedwars_plugin.bedwars.game.Teams.TeamManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;



public class ClickToJoinQueue implements Listener {


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if(null == clickedBlock) {
                System.out.println("The sign does not exist");
                return;
            }
            if(clickedBlock.getType() == Material.OAK_SIGN) {
                System.out.println("The oak sign location: " + clickedBlock.getLocation());
                Location location = clickedBlock.getLocation();
                if (location.getX() == GateConstant.BEDWARS_GATE_LOCATION_X &&
                        location.getY() == GateConstant.BEDWARS_GATE_LOCATION_Y &&
                        location.getZ() == GateConstant.BEDWARS_GATE_LOCATION_Z){

                    BwPlayer player = (BwPlayer) event.getPlayer();
//                    System.out.println(player.getPlayer() + "Clicked on sign at: " + GateConstant.BEDWARS_GATE_LOCATION);
                    System.out.println("Player is trying to enter Bedwars world: " + player.getBwName());
                    JoinQueue.doAddPlayerQueue(player);
                    if (TeamManager.checkEnoughPlayers(Bw_general.PLAYER_WAITING_QUEUE, 1, 2)){
                        GameManager.createGame(
                                Bw_general.BW_CREATED_MAPS.get( "Bw_Airshow" ),
                                Bw_general.PLAYER_WAITING_QUEUE,
                                GameConfig.toBuilder().
                                        sizePerTeam(1).
                                        teamCount(2).
                                        build()
                        );
                    }

                }


            }
        }
    }

}
