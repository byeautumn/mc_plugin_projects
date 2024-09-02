package org.aerial_dad.bedwars_plugin;

import org.aerial_dad.bedwars_plugin.bedwars.commands.Bw_general;
import org.aerial_dad.bedwars_plugin.bedwars.commands.Create_Npc;
import org.aerial_dad.bedwars_plugin.bedwars.commands.Npc_listener;
import org.aerial_dad.bedwars_plugin.bedwars.listeners.ClickToJoinQueue;
import org.aerial_dad.bedwars_plugin.bedwars.listeners.Op_listener;
import org.aerial_dad.bedwars_plugin.bedwars.game.Block_listener2;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class Bedwars_plugin extends JavaPlugin {

    public static Bedwars_plugin getInstance;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getInstance = this;
        getCommand("spawnshop").setExecutor(new Create_Npc(this));
        Material stoneSword = Material.STONE_SWORD;
        Material wool  = Material.WHITE_WOOL;
        ItemStack CostOfStone_Sword = new ItemStack(Material.IRON_INGOT, 4);
        ItemStack CostOfWool = new ItemStack(Material.IRON_INGOT, 4);
        Npc_listener.itemToCostMap.put(stoneSword, CostOfStone_Sword);
        Npc_listener.itemToCostMap.put(wool, CostOfWool);
        getServer().getPluginManager().registerEvents(new Block_listener2(this), this);
        getServer().getPluginManager().registerEvents(new Op_listener(), this );
        getServer().getPluginManager().registerEvents(new ClickToJoinQueue(), this );
        getServer().getPluginManager().registerEvents(new Npc_listener(this), this );
        Bw_general bwCommand = new Bw_general(this);
        getCommand("spawnshop").setExecutor(new Create_Npc(this));
        getCommand("bw").setExecutor(bwCommand);
        getCommand("bedwars").setExecutor(bwCommand);
        List<World> allWorlds = Bukkit.getWorlds();
        System.out.println("Bukkit loading all worlds. World count now is " + allWorlds.size());
        for (World world : allWorlds) {
            System.out.println("Checking world " + world.getName());
            if(isBwWorld(world.getName())) {
                Bw_general.BW_CREATED_MAPS.put(world.getName(), world);
                System.out.println("Loading " + world.getName() + " world to bedwars world list");
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
