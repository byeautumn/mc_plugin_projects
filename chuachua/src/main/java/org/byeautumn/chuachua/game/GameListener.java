package org.byeautumn.chuachua.game;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent; // Import for PlayerQuitEvent
import org.byeautumn.chuachua.Chuachua;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.common.PlayMode;
import org.byeautumn.chuachua.game.firstland.FirstLandWorldConfigAccessor;
import org.byeautumn.chuachua.generate.world.pipeline.ChuaWorld; // Import for ChuaWorld

import java.util.List;

public class GameListener implements Listener {
    private static final Block GAME_ENTRANCE_BLOCK = Universe.getLobby().getBlockAt(24, -59, 14);

    private final FirstLandWorldConfigAccessor configAccessor;
    private final Chuachua plugin;

    public GameListener(Chuachua plugin, FirstLandWorldConfigAccessor configAccessor){
        this.plugin = plugin;
        this.configAccessor = configAccessor;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        if (clickedBlock.getState() instanceof Sign) {
            Sign sign = (Sign) clickedBlock.getState();
            String firstLine = ChatColor.stripColor(sign.getLine(0));

            // Logic to open the GUI if the sign says "[Join Game]"
            if (firstLine.equalsIgnoreCase("[Join Game]")) {
                plugin.getFirstLandMenu().openInventory(event.getPlayer());
                event.setCancelled(true);
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

    @EventHandler
    private void onPlayerQuitEvent(PlayerQuitEvent event) {
        
    }
}