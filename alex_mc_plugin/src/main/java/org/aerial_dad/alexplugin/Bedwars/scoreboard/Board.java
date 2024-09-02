package org.aerial_dad.alexplugin.Bedwars.scoreboard;

import org.aerial_dad.alexplugin.AlexFirstPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class Board  implements Runnable{

    private final static Board instance = new Board();
    private final static String PLUGIN_NAME = "AlexFirstPlugin";


    private Board(){

    }

    @Override
    public void run() {
        for ( Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboard().getObjective(PLUGIN_NAME) != null)
                updateScoreboard(player);
            else
                createNewScoreboard(player);
        }
    }

    private void createNewScoreboard(Player player){
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(PLUGIN_NAME, "tbd");
        Objective health = scoreboard.registerNewObjective(PLUGIN_NAME, "tbd");

        health.setDisplaySlot(DisplaySlot.SIDEBAR);
        health.setDisplayName(ChatColor.DARK_RED + " " + ChatColor.BOLD + "Prototype");

        objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        objective.setDisplayName(ChatColor.DARK_RED + " " + ChatColor.BOLD + "Prototype");

        objective.getScore(ChatColor.WHITE + " ").setScore(8);
        objective.getScore(ChatColor.WHITE + "This is a test ");
        objective.getScore(ChatColor.WHITE + " of the "  + ChatColor.YELLOW + "NATIONAL SCOREBOARD SYSTEM").setScore(7);
        objective.getScore(ChatColor.YELLOW + " (NSS)  ").setScore(6);
        objective.getScore(ChatColor.RED + " ").setScore(5);
        objective.getScore(ChatColor.WHITE + "More info at: ").setScore(4);
        objective.getScore(ChatColor.WHITE + "www.scoreboard.gov. ").setScore(3);
        objective.getScore(ChatColor.WHITE+ " ").setScore(2);
//        objective.getScore(ChatColor.WHITE+ "Players online:" + ChatColor.YELLOW).setScore(1);

        Team team1 = scoreboard.registerNewTeam("team1");
        String teamKey = ChatColor.GOLD.toString();

        team1.addEntry(teamKey);
        team1.setPrefix("Players online: ");
        team1.setSuffix(" 0 ");

        objective.getScore(teamKey).setScore(0);

        player.setScoreboard(scoreboard);
    }

    private void updateScoreboard(Player player){
        Scoreboard scoreboard = player.getScoreboard();
        Team team1 = scoreboard.getTeam("team1");

        team1.setSuffix(ChatColor.YELLOW + " " + (Bukkit.getServer().getOnlinePlayers().size()));
    }

    public static Board getInstance() {
        return instance;
    }
}
