package org.byeautumn.chuachua;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.byeautumn.chuachua.block.BlockListener;
import org.byeautumn.chuachua.command.OperationCommand;
import org.byeautumn.chuachua.custom.ResourcePackListener;
import org.byeautumn.chuachua.game.GameListener;
import org.byeautumn.chuachua.game.firstland.*;
import org.byeautumn.chuachua.generate.world.pipeline.ChuaWorldConfigAccessor;
import org.byeautumn.chuachua.player.InventoryDataAccessor;
import org.byeautumn.chuachua.player.PlayerDataAccessor;
import org.checkerframework.checker.guieffect.qual.UI;

import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler; // Import ConsoleHandler

public final class Chuachua extends JavaPlugin {
    public static Chuachua getInstance; // Made public static as per your code
    private String resourcePackURL;
    private OperationCommand operationCommand; // Declare a field for your command executor
    private FirstLandJoinMenu firstLandJoinMenu; // Declare an instance
    private FirstLandWorldConfigAccessor firstLandWorldConfigAccessor;
    private WorldDataAccessor worldDataAccessor;
    private PlayerDataAccessor playerDataAccessor;
    private InventoryDataAccessor inventoryDataAccessor;


    public static final int MAIN_CONFIG_DEFAULT_MAX_WORLDS = 3;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getInstance = this; // Set the static instance
        getLogger().info("Chuachua plugin is enabling...");

        // --- Start: Programmatic Logging Configuration (Adjusted for less verbosity) ---
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO); // Set root logger to INFO

        // Remove existing ConsoleHandlers to avoid duplicates and ensure our settings apply
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                rootLogger.removeHandler(handler);
            }
        }

        // Add a new ConsoleHandler and set its level to INFO
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.INFO); // Set to INFO to suppress FINE logs from other parts
        rootLogger.addHandler(consoleHandler);
        getLogger().info("Configured ConsoleHandler level to INFO.");


        // Explicitly set the level for your specific biome generation logger to FINE
        // This will ensure the detailed noise output (logged at FINE) is visible.
        Logger protoBiomeLogger = Logger.getLogger("org.byeautumn.chuachua.generate.world.pipeline.ProtoBiomeGeneration");
        protoBiomeLogger.setLevel(Level.FINE); // Set to FINE to allow detailed noise logs
        getLogger().info("Set ProtoBiomeGeneration logger level to FINE.");
        // --- End: Programmatic Logging Configuration ---

        File pluginDataFolder = getDataFolder();

        this.worldDataAccessor = new WorldDataAccessor(new File(pluginDataFolder, "data"));
        this.playerDataAccessor = new PlayerDataAccessor(new File(pluginDataFolder, "data"));
        this.inventoryDataAccessor = new InventoryDataAccessor(new File(getDataFolder(), "data"));

        this.firstLandWorldConfigAccessor = new FirstLandWorldConfigAccessor(this);

        this.firstLandJoinMenu = new FirstLandJoinMenu(this, worldDataAccessor, playerDataAccessor);
        // Initialize your OperationCommand ONCE
        this.operationCommand = new OperationCommand(this, firstLandWorldConfigAccessor, firstLandJoinMenu, worldDataAccessor, playerDataAccessor, inventoryDataAccessor);

        FirstLandWorldNameListener firstLandWorldNameListener = new FirstLandWorldNameListener( playerDataAccessor,this);

        getServer().getPluginManager().registerEvents(firstLandWorldNameListener, this);
        getServer().getPluginManager().registerEvents(firstLandJoinMenu, this);
        getServer().getPluginManager().registerEvents(new GameListener(this, worldDataAccessor, playerDataAccessor, inventoryDataAccessor),this);
        getServer().getPluginManager().registerEvents(new BlockListener(this),this);

        // Register the main /cc command using the single instance
        if (this.getCommand("cc") != null) {
            Objects.requireNonNull(getCommand("cc")).setExecutor(this.operationCommand);
            getLogger().info("Registered command /cc with OperationCommand.");
        } else {
            getLogger().log(Level.WARNING, "Command 'cc' not found in plugin.yml. Please ensure it's defined.");
        }

        saveDefaultConfig();

        // 2. Load the main config
        FileConfiguration mainConfig = getConfig();

        // 3. Add default for max-worlds-per-player in the main config
        // This ensures the key exists with a default if it's missing in config.yml
        mainConfig.addDefault("max-worlds-per-player", MAIN_CONFIG_DEFAULT_MAX_WORLDS);
        mainConfig.options().copyDefaults(true); // Apply defaults
        saveConfig(); // Save the updated config.yml with new default
        resourcePackURL = mainConfig.getString("resource-pack-url", "");
        getServer().getPluginManager().registerEvents(new ResourcePackListener(resourcePackURL), this);

        // Load ChuaWorlds
        ChuaWorldConfigAccessor chuaWorldConfigAccessor = new ChuaWorldConfigAccessor(this);
        Universe.loadChuaWorldsToMap(chuaWorldConfigAccessor, this);
        chuaWorldConfigAccessor.saveConfig();

        //Load FirstLand Worlds
//        FirstLandWorldConfigAccessor firstLandWorldConfigAccessor = new FirstLandWorldConfigAccessor(this);
        Universe.loadFirstLandWorldsToMap(this, firstLandWorldConfigAccessor);
        firstLandWorldConfigAccessor.saveConfig();

        Universe.loadPlayerConnectionsFromConfig(this, firstLandWorldConfigAccessor);
        firstLandWorldConfigAccessor.saveConfig();

        // Register the internal /cc_teleport_internal command using the single instance
        if (this.getCommand("cc_teleport_internal") != null) {
            this.getCommand("cc_teleport_internal").setExecutor(this.operationCommand);
            getLogger().info("Registered internal command /cc_teleport_internal with OperationCommand.");
        } else {
            getLogger().log(Level.WARNING, "Internal command 'cc_teleport_internal' not found in plugin.yml. This command should be defined there for internal use.");
        }

        getLogger().info("Chuachua plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Chuachua plugin is disabling...");
        // Any cleanup code can go here
        getLogger().info("Chuachua plugin has been disabled!");
        firstLandWorldConfigAccessor.saveConfig();
    }
    public FirstLandJoinMenu getFirstLandMenu() {
        return firstLandJoinMenu;
    }
    public FirstLandWorldConfigAccessor getFirstLandWorldConfigAccessor() {
        return this.firstLandWorldConfigAccessor;
    }
}