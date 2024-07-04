package org.aerial_dad.alexplugin.Sumo.listeners;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class Player_logon_listener implements Listener {


    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        event.setFoodLevel(20);
        event.setCancelled(true); // Cancel hunger depletion
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        player.sendMessage(ChatColor.AQUA + player.getDisplayName() + " has joined the server!");
        player.performCommand("l");
        player.setGameMode(GameMode.ADVENTURE);
        player.setInvulnerable(true);

    }
}
