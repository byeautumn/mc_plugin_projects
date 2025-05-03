package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class ChuaWorldConfigAccessor {

    private final JavaPlugin plugin;
    private final File configFile;
    private final YamlConfiguration config;

    public ChuaWorldConfigAccessor(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "world_storing.yml");
        // Ensure the directory exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        saveDefaultConfig(); // Create the file if it doesn't exist
    }

    private void saveDefaultConfig() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile(); // Create the file
                // Set a default section, so the file is not empty
                config.createSection("worlds");
                saveConfig();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create config file: " + configFile, e);
            }
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, e);
        }
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public void addNewWorld(String worldName, long seed, Location spawnLocation) {
        String worldPath = "worlds." + worldName;
        config.set(worldPath + ".worldname", worldName); // Redundant, but keeps format consistent
        config.set(worldPath + ".seed", seed);
        config.set(worldPath + ".spawnlocation.x", spawnLocation.getX());
        config.set(worldPath + ".spawnlocation.y", spawnLocation.getY());
        config.set(worldPath + ".spawnlocation.z", spawnLocation.getZ());
        config.set(worldPath + ".spawnlocation.yaw", spawnLocation.getYaw());
        config.set(worldPath + ".spawnlocation.pitch", spawnLocation.getPitch());

        saveConfig();
    }

    public List<String> getKnownWorlds() {
        List<String> worldList = new ArrayList<>();
        if (config.isConfigurationSection("worlds")) { // Check if the section exists
            Set<String> keys = config.getConfigurationSection("worlds").getKeys(false);
            worldList.addAll(keys);
        }
        return worldList;
    }

    public boolean worldExistsInConfig(String worldName) {
        return config.contains("worlds." + worldName);
    }

    public Long getWorldSeed(String worldName) {
        return config.getLong("worlds." + worldName + ".seed");
    }

    public Location getWorldSpawnLocation(String worldName) {
        String path = "worlds." + worldName + ".spawnlocation";
        if (config.isConfigurationSection(path)) {
            double x = config.getDouble(path + ".x");
            double y = config.getDouble(path + ".y");
            double z = config.getDouble(path + ".z");
            float yaw = (float) config.getDouble(path + ".yaw", 0.0);
            float pitch = (float) config.getDouble(path + ".pitch", 0.0);
            // Need to get the World object to create a Location
            World world = plugin.getServer().getWorld(worldName);
            if (world != null) {
                return new Location(world, x, y, z, yaw, pitch);
            } else {
                plugin.getLogger().warning("World '" + worldName + "' is not loaded. Cannot get spawn location.");
                return null;
            }
        }
        return null;
    }

    public void loadWorldsOnStartup() {
        List<String> knownWorlds = getKnownWorlds();
        for (String worldName : knownWorlds) {
            if (plugin.getServer().getWorld(worldName) == null) { // Check if world is loaded
                Long seed = getWorldSeed(worldName);
                WorldCreator creator = new WorldCreator(worldName);
                if (seed != null) {
                    creator.seed(seed);
                }
                World world = creator.createWorld();
                if (world != null) {
                    Location spawn = getWorldSpawnLocation(worldName);
                    if (spawn != null) {
                        world.setSpawnLocation(spawn);
                        plugin.getLogger().info("Loaded world '" + worldName + "'.");
                    } else {
                        plugin.getLogger().warning("Could not set spawn location for world '" + worldName + "'.");
                    }
                } else {
                    plugin.getLogger().warning("Failed to load world '" + worldName + "'.");
                }
            }
        }
    }
}

