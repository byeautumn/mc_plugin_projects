package org.aerial_dad.bedwars_plugin;

import org.aerial_dad.bedwars_plugin.bedwars.commands.Bw_general;
import org.aerial_dad.bedwars_plugin.bedwars.listeners.Op_listener;
import org.aerial_dad.bedwars_plugin.bedwars.game.Block_listener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class Bedwars_plugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new Block_listener(this), this);
        getServer().getPluginManager().registerEvents(new Op_listener(), this );
        Bw_general bwCommand = new Bw_general(this);
        getCommand("bw").setExecutor(bwCommand);
        getCommand("bedwars").setExecutor(bwCommand);
        List<World> allWorlds = Bukkit.getWorlds();
        System.out.println("Bukkit loading all worlds. World count now is " + allWorlds.size());
        for (World world : allWorlds) {
            System.out.println("Checking world " + world.getName());
            if(isBwWorld(world.getName())) {
                Bw_general.BW_CREATED_MAPS.put(world.getName(), world);
                System.out.println("Loading " + world.getName() + " world to bedwar world list");
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private boolean isBwWorld(String worldName) {
        if(null == worldName || worldName.length() <= 3) {
            return false;
        }
        String bwPrefix = "bw_";
        return worldName.toLowerCase().startsWith(bwPrefix);
    }
}
