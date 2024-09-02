package org.aerial_dad.alexplugin;

import org.aerial_dad.alexplugin.Bedwars.scoreboard.Board;
import org.aerial_dad.alexplugin.Sumo.listeners.Block_listener;
import org.aerial_dad.alexplugin.Sumo.listeners.DuelGameListener;
import org.aerial_dad.alexplugin.Sumo.listeners.Player_logon_listener;
import org.aerial_dad.alexplugin.Sumo.commands.*;
import org.aerial_dad.alexplugin.Sumo.common.DuelGame;
import org.aerial_dad.alexplugin.Sumo.common.Global;
import org.aerial_dad.alexplugin.Sumo.listeners.Block_listener;
import org.aerial_dad.alexplugin.Sumo.listeners.DuelGameListener;
import org.aerial_dad.alexplugin.Sumo.listeners.Player_logon_listener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Arrays;
import java.util.List;


public class AlexFirstPlugin extends JavaPlugin {

    private BukkitTask task;
    private BukkitTask task2;

    private SumoGame sumoGame;
    @Override
    public void onEnable() {
        System.out.println("The plugin has started!");

        getConfig().options().copyDefaults();
        saveConfig();

        getServer().getPluginManager().registerEvents(new Player_logon_listener(), this);
        getServer().getPluginManager().registerEvents(new NpcCommand(), this);
        getServer().getPluginManager().registerEvents(new Block_listener(), this);
        getServer().getPluginManager().registerEvents(new Sumo_waiting(), this);
        getCommand("sethubspawn").setExecutor(new Sethub_command(this));
        getCommand("hub").setExecutor(new Hub_command(this));
        getCommand("setlspawn").setExecutor(new SetLspawn(this));
        getCommand("npc").setExecutor(new NpcCommand());
        L_command lCommand = new L_command(this);
        getCommand("l").setExecutor(lCommand);
        getCommand("leave").setExecutor(lCommand);
//      getCommand("play_duels_sumo").setExecutor(new Sumo_lobby());
        List<World> prebuiltDuelGameWorlds = Arrays.asList(Bukkit.getWorld("Sumo_game1"),
                Bukkit.getWorld("Sumo_game2"),
                Bukkit.getWorld("Sumo_game3"));
        for(World world : prebuiltDuelGameWorlds) {
            System.out.println("The existing worlds are:" + Bukkit.getWorlds());
            System.out.println("Sanity checking world " + world);
        }
        Global.loadPrebuiltDuelGameWorlds(prebuiltDuelGameWorlds);
        Global.loadMainLobbyWorld(Bukkit.getWorld("Main_lobby"));
        Global.addDuelGames(prebuiltDuelGameWorlds);
        getServer().getPluginManager().registerEvents(new DuelGameListener(), this);

        task = getServer().getScheduler().runTaskTimer(this, Board.getInstance(), 8, 1 );
        task2 = getServer().getScheduler().runTaskTimer(this, Board.getInstance(), 8, 1 );

    }
    @Override
    public void onDisable() {
        System.out.println("The plugin has been stopped!");

        if (task != null && !task2.isCancelled())
            task2.cancel();

        if (task2 != null && !task2.isCancelled())
            task2.cancel();
    }

}

/**
 * BUGS
 * 1. When someone leaves the game does not know who won.
 * 2. when the game starts or has ended you always get teleported to the platform but there should be no victory of defeat but when the game is on this can happen.
 * 3. When the second player joins the game they get reported into the void until the countdown is finished.
 * 4. when the game ends you join another game, and then you fall off. the player will get teleported back to the old world and it will say defeated.
 */

