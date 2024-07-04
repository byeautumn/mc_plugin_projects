package org.aerial_dad.bedwars_plugin.bedwars.listeners;

import org.aerial_dad.bedwars_plugin.bedwars.game.Constants.GateConstant;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;


public class ClickToJoinQueue implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if(null == clickedBlock) return;

            if(clickedBlock.getType() == Material.OAK_SIGN) {
//                System.out.println("The oak sign location: " + clickedBlock.getLocation());
                Location location = clickedBlock.getLocation();
                if (location.getWorld() == GateConstant.BEDWARS_GATE_LOCATION &&
                        location.getX() == GateConstant.BEDWARS_GATE_LOCATION.getX() &&
                        location.getY() == GateConstant.BEDWARS_GATE_LOCATION.getY() &&
                        location.getZ() == GateConstant.BEDWARS_GATE_LOCATION.getZ()){
                    Player player = event.getPlayer();
                    System.out.println("Player is trying to enter Bedwars world: " + player.getDisplayName());

                }


            }
        }
    }

}
