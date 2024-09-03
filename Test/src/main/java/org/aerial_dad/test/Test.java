package org.aerial_dad.test;

import org.aerial_dad.test.mechanics.FireBallMechanic;
import org.aerial_dad.test.shop.Create_Npc;
import org.aerial_dad.test.shop.Npc_listener;
import org.aerial_dad.test.shop.ShopConfig;
import org.bukkit.plugin.java.JavaPlugin;

public final class Test extends JavaPlugin {

    public static Test getInstance;



    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println(" Plugin has started!!!!!!!!!!!!!!____________");
        getInstance = this;
        ShopConfig.getInstance().load();

        getServer().getPluginManager().registerEvents(new Npc_listener(this), this);
        getServer().getPluginManager().registerEvents(new Block_listener2(this), this);
        getServer().getPluginManager().registerEvents(new FireBallMechanic(), this);
        getCommand("spawnshop").setExecutor(new Create_Npc(this));
        Bw_general bwCommand = new Bw_general(this);
        getCommand("bw").setExecutor(bwCommand);
        getCommand("bedwars").setExecutor(bwCommand);

//        Material wool  = Material.WHITE_WOOL;
//        Material wood = Material.OAK_PLANKS;
//        ItemStack costOfWood = new ItemStack(Material.GOLD_INGOT, 4);
//        ItemStack costOfWool = new ItemStack(Material.IRON_INGOT, 4);
//        Npc_listener.itemToCostMap.put(wool, costOfWool);
//        Npc_listener.itemToCostMap.put(wood, costOfWood);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("Plugin has ended!!!!!!!!!!!!!!!!!!______________");
    }
}
