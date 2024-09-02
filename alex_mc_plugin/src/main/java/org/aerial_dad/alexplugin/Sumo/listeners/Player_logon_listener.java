package org.aerial_dad.alexplugin.Sumo.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class Player_logon_listener implements Listener {

    private final static String PLUGIN_NAME = "AlexFirstPlugin";


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
        if (player.getScoreboard().getObjective(PLUGIN_NAME) != null)
            updateScoreboard(player);
        else
            createNewScoreboard(player);
    }


//        Location spawnLocation = new Location(Bukkit.getServer().getWorld("world"), 100, 64, 0); // Replace with your desired location
//        Entity npc = spawnLocation.getWorld().spawnEntity(spawnLocation, EntityType.PLAYER);
//        npc.

    private void createNewScoreboard(Player player){
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective showHealth = scoreboard.registerNewObjective(PLUGIN_NAME, "tbd");
        showHealth.setDisplaySlot(DisplaySlot.BELOW_NAME);
        showHealth.setDisplayName("");
//        showHealth.getScore(player.getHealth() + "❤");

        Team team1 = scoreboard.registerNewTeam("team1");
        String teamKey = ChatColor.GOLD.toString();

        team1.addEntry(teamKey);
        team1.setPrefix(ChatColor.RED + "❤");
        team1.setSuffix("20");

        showHealth.getScore(teamKey).setScore(0);

        player.setScoreboard(scoreboard);



    }

    private void updateScoreboard(Player player){
        Scoreboard scoreboard = player.getScoreboard();
        Team team1 = scoreboard.getTeam("team1");

        team1.setSuffix(ChatColor.YELLOW + " " + (player.getHealth()));
    }


}
