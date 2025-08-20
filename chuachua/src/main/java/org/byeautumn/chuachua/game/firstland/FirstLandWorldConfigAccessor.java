package org.byeautumn.chuachua.game.firstland;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.byeautumn.chuachua.Chuachua; // Import your main plugin class to access its static default

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID; // Essential import for UUIDs
import java.util.logging.Level;

public class FirstLandWorldConfigAccessor {

    private final JavaPlugin plugin;
    private final File configFile; // Still manages its own world-data-specific config file
    private final YamlConfiguration config; // Still manages its own world-data-specific config

    public FirstLandWorldConfigAccessor(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "first_land_world_storing.yml");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
        saveDefaultConfig(); // This saveDefaultConfig pertains only to 'first_land_world_storing.yml'
    }

    private void saveDefaultConfig() {
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                config.createSection("worlds");
                config.set("amount", 0); // Initialize the amount counter for world naming
                saveConfig();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create config file: " + configFile, e);
            }
        }
    }

    /**
     * Gets the number of worlds connected to a specific player.
     * @param playerUUIDString The UUID string of the player.
     * @return The count of worlds connected to the player.
     */
    public int getPlayerWorldCount(String playerUUIDString) {
        int count = 0;
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldUUIDKey : worldsSection.getKeys(false)) { // Iterate by World UUID string
                String connectedPlayerUUID = worldsSection.getString(worldUUIDKey + ".connected-player-uuid");
                if (playerUUIDString.equals(connectedPlayerUUID)) {
                    count++;
                }
            }
        }
        return count;
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

    /**
     * Adds a new world entry to the configuration, primarily identified by its World UUID.
     * This method is used both when a **new** First Land world is created,
     * AND when an **existing unconnected world is assigned** to a player (by updating its friendly name and connected player UUID).
     *
     * @param worldUUID The UUID of the world.
     * @param worldName The internal Bukkit name of the world (e.g., First_Land_World_01).
     * @param friendlyName The user-defined friendly name for the world.
     * @param playerUUID The UUID of the player connected to this world (or null if none).
     * @param seed The seed of the world.
     * @param spawnLocation The spawn location of the world.
     */
    public void addNewWorld(UUID worldUUID, String worldName, String friendlyName, UUID playerUUID, long seed, Location spawnLocation) {
        String worldPath = "worlds." + worldUUID.toString(); // Use World UUID as primary key
        boolean isNewEntry = !config.contains(worldPath); // Check if the path already exists

        config.set(worldPath + ".worldname", worldName); // Still store internal Bukkit name
        config.set(worldPath + ".friendly-name", friendlyName);
        config.set(worldPath + ".connected-player-uuid", playerUUID != null ? playerUUID.toString() : "none"); // Store Player UUID string
        config.set(worldPath + ".seed", seed);
        config.set(worldPath + ".spawnlocation.x", spawnLocation.getX());
        config.set(worldPath + ".spawnlocation.y", spawnLocation.getY());
        config.set(worldPath + ".spawnlocation.z", spawnLocation.getZ());
        config.set(worldPath + ".spawnlocation.yaw", spawnLocation.getYaw());
        config.set(worldPath + ".spawnlocation.pitch", spawnLocation.getPitch());
        config.set(worldPath + ".created-at", System.currentTimeMillis());

        // Update the 'amount' based on the highest numerical suffix of "First_Land_World_XX"
        // This logic remains tied to the worldName format, which is fine as a counter for new world generation naming.
        int worldNumber = -1;
        try {
            if (worldName.startsWith("First_Land_World_")) {
                worldNumber = Integer.parseInt(worldName.substring("First_Land_World_".length()));
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {}

        if (worldNumber != -1) {
            if (worldNumber >= config.getInt("amount", 0)) {
                config.set("amount", worldNumber + 1);
            }
        }

        // Enhanced logging
        if (isNewEntry) {
            plugin.getLogger().info("Added NEW world entry for UUID '" + worldUUID + "' with friendly name '" + friendlyName + "'.");
        } else {
            plugin.getLogger().info("UPDATED existing world entry for UUID '" + worldUUID + "' with friendly name '" + friendlyName + "'.");
        }
        saveConfig();
    }

    /**
     * Finds the UUID of the first FirstLandWorld that is not connected to a player.
     * This method is crucial for reusing existing worlds instead of always creating new ones.
     *
     * @return The UUID of an unconnected world, or null if all are connected.
     */
    public UUID findFirstUnconnectedWorldUUID() {
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldUUIDString : worldsSection.getKeys(false)) { // Iterate by UUID string
                String connectedPlayerUUID = worldsSection.getString(worldUUIDString + ".connected-player-uuid");
                if ("none".equals(connectedPlayerUUID) || connectedPlayerUUID == null || connectedPlayerUUID.trim().isEmpty()) {
                    try {
                        return UUID.fromString(worldUUIDString); // Return the UUID object
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID found in config for world key: " + worldUUIDString);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Retrieves a list of all World UUIDs stored in the configuration.
     * @return A list of World UUIDs.
     */
    public List<UUID> getKnownWorldUUIDs() {
        List<UUID> worldUUIDs = new ArrayList<>();
        if (config.isConfigurationSection("worlds")) {
            Set<String> keys = config.getConfigurationSection("worlds").getKeys(false);
            for (String key : keys) {
                try {
                    worldUUIDs.add(UUID.fromString(key));
                }
                catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID found as world key in config: " + key);
                }
            }
        }
        return worldUUIDs;
    }

    /**
     * Checks if a world with the given UUID exists in the configuration.
     * @param worldUUID The UUID of the world.
     * @return true if the world exists in config, false otherwise.
     */
    public boolean worldExistsInConfig(UUID worldUUID) {
        return config.contains("worlds." + worldUUID.toString());
    }

    /**
     * Gets the internal Bukkit world name associated with a given World UUID.
     * @param worldUUID The UUID of the world.
     * @return The internal Bukkit world name, or null if not found.
     */
    public String getWorldName(UUID worldUUID) {
        return config.getString("worlds." + worldUUID.toString() + ".worldname");
    }

    /**
     * Gets the seed associated with a given World UUID.
     * @param worldUUID The UUID of the world.
     * @return The seed, or null if not found.
     */
    public Long getWorldSeed(UUID worldUUID) {
        // Use default of 0L if not found to prevent NullPointerException for primitives
        return config.getLong("worlds." + worldUUID.toString() + ".seed", 0L);
    }

    /**
     * Retrieves the connected Player UUID for a specific world identified by its UUID.
     * @param worldUUID The UUID of the world.
     * @return The UUID of the connected player as a String, or "none" if unconnected.
     */
    public String getConnectedPlayerUUID(UUID worldUUID) {
        String connectedPlayerUUID = config.getString("worlds." + worldUUID.toString() + ".connected-player-uuid");
        return (connectedPlayerUUID != null && !connectedPlayerUUID.isEmpty()) ? connectedPlayerUUID : "none";
    }

    /**
     * Updates the connected player for a specific world identified by its UUID.
     * This method directly uses the world's UUID to update its connected player.
     *
     * @param worldUUID The UUID of the world to update.
     * @param playerUUID The UUID of the player to connect (or null to disconnect).
     */
    public void updateConnectedPlayer(UUID worldUUID, UUID playerUUID) {
        String playerUUIDString = (playerUUID != null) ? playerUUID.toString() : "none";
        if (worldUUID != null) {
            config.set("worlds." + worldUUID.toString() + ".connected-player-uuid", playerUUIDString);
            saveConfig();
            plugin.getLogger().info("Updated player connection for world UUID '" + worldUUID + "' to player UUID '" + playerUUIDString + "'.");
        } else {
            plugin.getLogger().warning("Attempted to update connected player for a null world UUID.");
        }
    }

    /**
     * Retrieves the friendly name for a given World UUID.
     *
     * @param worldUUID The UUID of the world.
     * @return The friendly name of the world, or its internal Bukkit name if no friendly name is found.
     */
    public String getWorldFriendlyName(UUID worldUUID) {
        String friendlyName = config.getString("worlds." + worldUUID.toString() + ".friendly-name");
        if (friendlyName != null && !friendlyName.isEmpty()) {
            return friendlyName;
        }
        // Fallback to the internal world name if friendly name is not set
        return getWorldName(worldUUID); // Get internal name using the new method
    }

    /**
     * Retrieves a list of World UUIDs owned by a specific player (identified by their UUID).
     *
     * @param playerUUID The UUID of the player.
     * @return A list of World UUIDs owned by the player.
     */
    public List<UUID> getPlayerOwnedWorldUUIDs(UUID playerUUID) {
        List<UUID> ownedWorldUUIDs = new ArrayList<>();
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            String playerUUIDString = playerUUID.toString();
            for (String worldUUIDString : worldsSection.getKeys(false)) {
                String connectedPlayerUUID = worldsSection.getString(worldUUIDString + ".connected-player-uuid");
                if (playerUUIDString.equals(connectedPlayerUUID)) {
                    try {
                        ownedWorldUUIDs.add(UUID.fromString(worldUUIDString));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID found in config when getting player owned worlds: " + worldUUIDString);
                    }
                }
            }
        }
        return ownedWorldUUIDs;
    }

    /**
     * Checks if a specific player already owns a world with the given friendly name.
     * This performs a case-insensitive check against existing friendly names.
     * @param playerUUID The UUID of the player.
     * @param friendlyName The friendly name to check.
     * @return true if the player already owns a world with this friendly name, false otherwise.
     */
    public boolean playerOwnsFriendlyName(UUID playerUUID, String friendlyName) {
        if (friendlyName == null || friendlyName.trim().isEmpty()) {
            return false; // Cannot own an empty or null friendly name
        }
        String lowerCaseFriendlyName = friendlyName.toLowerCase();

        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            String playerUUIDString = playerUUID.toString();
            for (String worldUUIDString : worldsSection.getKeys(false)) {
                String connectedPlayerUUID = worldsSection.getString(worldUUIDString + ".connected-player-uuid");
                if (playerUUIDString.equals(connectedPlayerUUID)) {
                    String existingFriendlyName = worldsSection.getString(worldUUIDString + ".friendly-name");
                    if (existingFriendlyName != null && existingFriendlyName.toLowerCase().equals(lowerCaseFriendlyName)) {
                        return true; // Found a duplicate friendly name for this player
                    }
                }
            }
        }
        return false; // No duplicate friendly name found for this player
    }


    /**
     * Gets the spawn location for a world identified by its UUID.
     * Requires the world to be loaded to get a valid World object.
     * @param worldUUID The UUID of the world.
     * @return The Location object, or null if world not loaded or spawn not found.
     */
    public Location getWorldSpawnLocation(UUID worldUUID) {
        String path = "worlds." + worldUUID.toString() + ".spawnlocation";
        if (config.isConfigurationSection(path)) {
            double x = config.getDouble(path + ".x");
            double y = config.getDouble(path + ".y");
            double z = config.getDouble(path + ".z");
            float yaw = (float) config.getDouble(path + ".yaw", 0.0);
            float pitch = (float) config.getDouble(path + ".pitch", 0.0);

            String worldName = getWorldName(worldUUID); // Get internal Bukkit name
            World world = plugin.getServer().getWorld(worldName); // Get Bukkit World object

            if (world != null) {
                return new Location(world, x, y, z, yaw, pitch);
            } else {
                plugin.getLogger().warning("World '" + worldName + "' (UUID: " + worldUUID + ") is not loaded. Cannot get spawn location.");
                return null;
            }
        }
        return null;
    }

    /**
     * Deletes a world entry from the configuration by its UUID.
     * @param worldUUID The UUID of the world to delete.
     */
    public void deleteWorldEntry(UUID worldUUID) {
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            worldsSection.set(worldUUID.toString(), null);
            saveConfig();
        }
    }

    /**
     * Updates the 'amount' key in the config to reflect the highest numbered world.
     * This logic still relies on the internal world name format (First_Land_World_XX)
     * for sequence tracking, while actual storage uses UUIDs.
     */
    public void updateAmountToHighestWorldNumber() {
        int highestNumber = -1;
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldUUIDString : worldsSection.getKeys(false)) {
                String worldName = config.getString("worlds." + worldUUIDString + ".worldname");
                if (worldName != null && worldName.startsWith("First_Land_World_")) {
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
        }
        config.set("amount", highestNumber + 1);
        saveConfig();
    }

    /**
     * Retrieves the current 'amount' of worlds from the configuration file.
     * This value typically represents the next available world number in the sequence
     * for naming newly created worlds (e.g., First_Land_World_XX).
     * @return The current 'amount' value, or 0 if it doesn't exist.
     */
    public int getWorldAmount() {
        return config.getInt("amount", 0);
    }

    /**
     * Retrieves the maximum number of worlds a player is allowed to own from the main plugin's configuration.
     * This setting is now read from the main config.yml.
     * @return The maximum number of worlds, or the default from Chuachua.java if not specified.
     */
    public int getMaxWorldsPerPlayer() {
        // Access the main plugin's config to get the max worlds per player setting
        // We need to ensure Chuachua.getInstance() is not null before using it.
        if (plugin instanceof Chuachua) {
            return plugin.getConfig().getInt("max-worlds-per-player", Chuachua.MAIN_CONFIG_DEFAULT_MAX_WORLDS);
        }
        // Fallback if plugin somehow isn't Chuachua instance (shouldn't happen with proper setup)
        return Chuachua.MAIN_CONFIG_DEFAULT_MAX_WORLDS;
    }

    /**
     * Retrieves the UUID of a ChuaWorld given its internal Bukkit world name.
     * This is useful for looking up worlds by their internal name when you only have the UUIDs in config.
     * @param internalWorldName The internal Bukkit world name (e.g., "First_Land_World_01").
     * @return The UUID of the world, or null if not found.
     */
    public UUID getChuaWorldUUIDByInternalName(String internalWorldName) {
        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldUUIDString : worldsSection.getKeys(false)) {
                String nameInConfig = worldsSection.getString(worldUUIDString + ".worldname");
                if (nameInConfig != null && nameInConfig.equals(internalWorldName)) {
                    try {
                        return UUID.fromString(worldUUIDString);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID found in config for world name '" + internalWorldName + "': " + worldUUIDString);
                    }
                }
            }
        }
        return null;
    }
}