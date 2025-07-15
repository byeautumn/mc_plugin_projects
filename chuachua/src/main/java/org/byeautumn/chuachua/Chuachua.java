package org.byeautumn.chuachua;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.byeautumn.chuachua.block.BlockListener;
import org.byeautumn.chuachua.command.OperationCommand;
import org.byeautumn.chuachua.custom.ResourcePackListener;
import org.byeautumn.chuachua.game.GameListener;
import org.byeautumn.chuachua.generate.world.pipeline.ChuaWorldConfigAccessor;

import java.util.Objects;

public final class Chuachua extends JavaPlugin {
    public static Chuachua getInstance;
    private String resourcePackURL;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getInstance = this;
        getServer().getPluginManager().registerEvents(new GameListener(),this);
        getServer().getPluginManager().registerEvents(new BlockListener(this),this);
        Objects.requireNonNull(getCommand("cc")).setExecutor(new OperationCommand(this));

        saveDefaultConfig();
        FileConfiguration config = getConfig();
        resourcePackURL = config.getString("resource-pack-url", "");
        getServer().getPluginManager().registerEvents(new ResourcePackListener(resourcePackURL), this);

        ChuaWorldConfigAccessor chuaWorldConfigAccessor = new ChuaWorldConfigAccessor(this);
//        chuaWorldConfigAccessor.loadWorldsOnStartup();
        Universe.loadChuaWorldsToMap(chuaWorldConfigAccessor);
        chuaWorldConfigAccessor.saveConfig();

        getLogger().setLevel(java.util.logging.Level.FINE);
        getLogger().info("YourPlugin has been enabled!");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

    }
}
