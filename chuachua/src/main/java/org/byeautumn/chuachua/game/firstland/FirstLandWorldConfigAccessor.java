package org.byeautumn.chuachua.game.firstland;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.byeautumn.chuachua.generate.world.pipeline.ChuaWorld;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class FirstLandWorldConfigAccessor {
    private final JavaPlugin plugin;
    private final File configFile;
    private final YamlConfiguration config;

    public FirstLandWorldConfigAccessor(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "first_land_world_storing.yml");
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

    public void addNewWorld(String worldName, String playerName, long seed, Location spawnLocation) {
        String worldPath = "worlds." + worldName;
        config.set(worldPath + ".worldname", worldName);
        config.set(worldPath + ".connected-player", playerName);
        config.set(worldPath + ".seed", seed);
        config.set(worldPath + ".spawnlocation.x", spawnLocation.getX());
        config.set(worldPath + ".spawnlocation.y", spawnLocation.getY());
        config.set(worldPath + ".spawnlocation.z", spawnLocation.getZ());
        config.set(worldPath + ".spawnlocation.yaw", spawnLocation.getYaw());
        config.set(worldPath + ".spawnlocation.pitch", spawnLocation.getPitch());

        // Get the integer number from the world name.
        int worldNumber = -1;
        try {
            worldNumber = Integer.parseInt(worldName.substring("First_Land_World_".length()));
        } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {
            // This will ignore cases where the world name is not in the expected format.
        }

        // Only update the amount if the new world has a number greater than the current amount.
        // This is the key fix for your problem.
        if (worldNumber != -1) {
            if (worldNumber >= config.getInt("amount", 0)) {
                config.set("amount", worldNumber + 1);
            }
        }

        saveConfig();
    }

    public int getWorldAmount() {
        return config.getInt("amount", 0);
    }

    public void incrementWorldAmount(int amountToAdd) {
        int currentAmount = getWorldAmount();
        config.set("amount", currentAmount + amountToAdd);
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

    public Player getConnectedPlayer(ConfigurationSection configurationSection) {
        String playerName = configurationSection.getString("connected-player");

        if (playerName == null || playerName.isEmpty()) {
            return null; // Return null if no player name is found
        }

        return Bukkit.getPlayer(playerName);
    }

    // An example of how you might use this
    public ConfigurationSection getWorldConfigSection(String worldName) {
        return config.getConfigurationSection("worlds." + worldName);
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

    public void deleteFirstLandWorldConfigSection(ConfigurationSection firstLandSection) {
        if (firstLandSection != null) {
            ConfigurationSection parent = firstLandSection.getParent();
            if (parent != null) {
                String key = firstLandSection.getName();
                parent.set(key, null);
                saveConfig();
                // Call the new method to update the amount
                updateAmountToHighestWorldNumber();
            } else {
                System.err.println("Cannot delete a top-level configuration section directly.");
            }
        }
    }

    private void updateAmountToHighestWorldNumber() {
        int highestNumber = -1;
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldName : worldsSection.getKeys(false)) {
                try {
                    int number = Integer.parseInt(worldName.substring("First_Land_World_".length()));
                    if (number > highestNumber) {
                        highestNumber = number;
                    }
                } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {
                    // Ignore improperly named worlds
                }
            }
        }
        config.set("amount", highestNumber + 1);
        saveConfig();
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
