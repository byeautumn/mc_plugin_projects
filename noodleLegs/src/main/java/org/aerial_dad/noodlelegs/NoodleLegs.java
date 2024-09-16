package org.aerial_dad.noodlelegs;

import org.aerial_dad.noodlelegs.game.GameCommand;
import org.aerial_dad.noodlelegs.game.NoodleListener;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class NoodleLegs extends JavaPlugin {

    public static NoodleLegs getInstance;

    @Override
    public void onEnable() {

        getInstance = this;
        ShopConfig.getInstance().load();

        Noodle_Legs_commands noodleLegsCommands = new Noodle_Legs_commands();
        getCommand("nl").setExecutor(noodleLegsCommands);
        getCommand("noodlelegs").setExecutor(noodleLegsCommands);
        getCommand("spawnnpc").setExecutor(new SpawnNpc(this));
        getCommand("exit").setExecutor(new GameCommand());
        getCommand("shop").setExecutor(new SpawnNpc(this));
        getCommand("bw").setExecutor(new Bw_general(this));
        getCommand("bedwars").setExecutor(new Bw_general(this));
        getServer().getPluginManager().registerEvents(new Npc_listener(this), this);
        getServer().getPluginManager().registerEvents(new Block_listener(this), this);
        getServer().getPluginManager().registerEvents(new NoodleListener(), this);
        // Plugin startup logic

        // Load custom worlds
        World orchestraWorld = Bukkit.getWorld("orchestra");
        System.out.println("orchestra world: " + orchestraWorld);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
