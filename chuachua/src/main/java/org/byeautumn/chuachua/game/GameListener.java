package org.byeautumn.chuachua.game;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BlockType;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.common.LocationVector;
import org.byeautumn.chuachua.common.PlayMode;
import org.byeautumn.chuachua.player.PlayerStatus;
import org.byeautumn.chuachua.player.PlayerTracker;

public class GameListener implements Listener {
//    private static final LocationVector GAME_ENTRANCE_LOCATION_VECTOR = new LocationVector(24.0, -58.0, 14.0);
//    private static final Location GAME_ENTRANCE_LOCATION = new Location(Universe.getLobby(),
//            GAME_ENTRANCE_LOCATION_VECTOR.getX(), GAME_ENTRANCE_LOCATION_VECTOR.getY(), GAME_ENTRANCE_LOCATION_VECTOR.getZ());
    private static final Block GAME_ENTRANCE_BLOCK = Universe.getLobby().getBlockAt(24, -59, 14);

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (null == clickedBlock) return;
            if (clickedBlock.getType() == Material.OAK_SIGN) {
                Location location = clickedBlock.getLocation();
                System.out.println("The clicked OAK sign location: " + location);
                if (Universe.areBlocksSame(clickedBlock, GAME_ENTRANCE_BLOCK)) {
                    System.out.println("Player is trying to enter the game.");
                    GameOrganizer.getInstance().guidePlayer(event.getPlayer());
                }
            }
        }
    }

    @EventHandler
    private void onPlayerJoinEvent(PlayerJoinEvent event){
        Player player = event.getPlayer();
        System.out.println("Player " + player.getDisplayName() + " is just going to lobby.");
        Universe.teleportToLobby(player);
        if (!player.isOp()){
            Universe.resetPlayerTracker(player);
        } else {
            player.sendMessage("* " + ChatColor.YELLOW + "You are op-ed and have automatically been set into " +ChatColor.AQUA + "'EDIT'" + ChatColor.YELLOW + " mode.");
            Universe.getPlayerTracker(player).setPlayMode(PlayMode.EDIT);
        }
        player.setGameMode(GameMode.ADVENTURE);
        System.out.println("Game mode is set to ADVENTURE for player " + player.getDisplayName() + ".");
    }

}
