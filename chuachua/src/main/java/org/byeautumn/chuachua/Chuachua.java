package org.byeautumn.chuachua;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.byeautumn.chuachua.block.BlockListener;
import org.byeautumn.chuachua.command.OperationCommand;
import org.byeautumn.chuachua.game.GameListener;

import java.util.Objects;

public final class Chuachua extends JavaPlugin {
    public static Chuachua getInstance;
    @Override
    public void onEnable() {
        // Plugin startup logic
        getInstance = this;
        getServer().getPluginManager().registerEvents(new GameListener(),this);
        getServer().getPluginManager().registerEvents(new BlockListener(this),this);
        Objects.requireNonNull(getCommand("cc")).setExecutor(new OperationCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
