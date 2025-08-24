package org.byeautumn.chuachua.game.firstland;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.byeautumn.chuachua.Chuachua; // Import your main plugin class to access its static default
import org.byeautumn.chuachua.common.LocationVector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class FirstLandWorldConfigAccessor {

    private final JavaPlugin plugin;
    private final File configFile; // Still manages its own world-data-specific config file
    private final YamlConfiguration config; // Still manages its own world-data-specific config
    private final Object fileLock = new Object();

    private final Map<UUID, UUID> worldUUIDToPlayerUUIDMap = new ConcurrentHashMap<>();
    private final Set<UUID> unconnectedWorldUUIDs = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> worldUUIDToFriendlyNameMap = new ConcurrentHashMap<>();
    private final Map<UUID, String> worldUUIDToInternalNameMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> worldUUIDToSeedMap = new ConcurrentHashMap<>();
    private final Map<UUID, Location> worldUUIDToSpawnLocationMap = new ConcurrentHashMap<>();
    private final Set<Integer> existingWorldNumbers = ConcurrentHashMap.newKeySet();

    private final Map<String, UUID> internalNameToWorldUUIDMap = new ConcurrentHashMap<>(); // Added for lookup by internal name


    private int amount = 0;
    private int maxWorldsPerPlayer = 3;

    public FirstLandWorldConfigAccessor(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "first_land_world_storing.yml");
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        saveDefaultConfig();
        this.config = YamlConfiguration.loadConfiguration(configFile);
        this.loadCacheFromConfig();
    }

    /**
     * Populates the in-memory cache from the config file on startup.
     * This method is only called once when the plugin enables.
     */
    private void loadCacheFromConfig() {
        // Clear caches before loading
        worldUUIDToPlayerUUIDMap.clear();
        unconnectedWorldUUIDs.clear();
        worldUUIDToFriendlyNameMap.clear();
        worldUUIDToInternalNameMap.clear();
        worldUUIDToSeedMap.clear();
        worldUUIDToSpawnLocationMap.clear();
        internalNameToWorldUUIDMap.clear(); // Clear the new map
        existingWorldNumbers.clear();

        ConfigurationSection worldsSection = config.getConfigurationSection("worlds");
        if (worldsSection != null) {
            for (String worldUUIDString : worldsSection.getKeys(false)) {
                try {
                    UUID worldUUID = UUID.fromString(worldUUIDString);
                    String connectedPlayerUUIDString = worldsSection.getString(worldUUIDString + ".connected-player-uuid");
                    String friendlyName = worldsSection.getString(worldUUIDString + ".friendly-name");
                    String worldName = worldsSection.getString(worldUUIDString + ".worldname");
                    Long seed = worldsSection.getLong(worldUUIDString + ".seed", 0L);
                    Location spawnLocation = (Location) worldsSection.get(worldUUIDString + ".spawnlocation");

                    // Populate caches
                    if (worldName != null) {
                        worldUUIDToInternalNameMap.put(worldUUID, worldName);
                        internalNameToWorldUUIDMap.put(worldName, worldUUID); // Populate new map
                        if (worldName.startsWith("First_Land_World_")) {
                            try {
                                existingWorldNumbers.add(Integer.parseInt(worldName.substring("First_Land_World_".length())));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    if (friendlyName != null) {
                        worldUUIDToFriendlyNameMap.put(worldUUID, friendlyName);
                    }
                    worldUUIDToSeedMap.put(worldUUID, seed);
                    if (spawnLocation != null) {
                        worldUUIDToSpawnLocationMap.put(worldUUID, spawnLocation);
                    }
                    if ("none".equals(connectedPlayerUUIDString) || connectedPlayerUUIDString == null || connectedPlayerUUIDString.trim().isEmpty()) {
                        unconnectedWorldUUIDs.add(worldUUID);
                    } else {
                        worldUUIDToPlayerUUIDMap.put(worldUUID, UUID.fromString(connectedPlayerUUIDString));
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID found in config for world key: " + worldUUIDString);
                }
            }
        }
        this.amount = config.getInt("amount", 0);
        // this.maxWorldsPerPlayer = plugin.getConfig().getInt("max-worlds-per-player", 3); // Should ideally come from main plugin config
    }

    private void saveDefaultConfig() {
        synchronized (fileLock) {
            if (!configFile.exists()) {
                try {
                    configFile.createNewFile();
                    YamlConfiguration tempConfig = new YamlConfiguration();
                    tempConfig.createSection("worlds");
                    tempConfig.set("amount", 0);
                    tempConfig.set("maxWorldsPerPlayer", 3);
                    tempConfig.save(configFile);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create config file: " + configFile, e);
                }
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
        // Iterate through the cached map for performance
        UUID playerUUID = UUID.fromString(playerUUIDString);
        for (UUID worldUUID : worldUUIDToPlayerUUIDMap.keySet()) {
            if (worldUUIDToPlayerUUIDMap.get(worldUUID).equals(playerUUID)) {
                count++;
            }
        }
        return count;
    }

    public void saveConfig() {
        synchronized (fileLock) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, e);
            }
        }
    }

    public YamlConfiguration getConfig() {
        // Synchronize on read access as well
        synchronized (fileLock) {
            return config;
        }
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
        synchronized (fileLock) {
            String worldPath = "worlds." + worldUUID.toString();
            boolean isNewEntry = !config.contains(worldPath);

            // Update in-memory caches
            worldUUIDToInternalNameMap.put(worldUUID, worldName);
            internalNameToWorldUUIDMap.put(worldName, worldUUID); // Update new map
            worldUUIDToFriendlyNameMap.put(worldUUID, friendlyName);
            worldUUIDToSeedMap.put(worldUUID, seed);
            worldUUIDToSpawnLocationMap.put(worldUUID, spawnLocation);

            if (playerUUID != null) {
                worldUUIDToPlayerUUIDMap.put(worldUUID, playerUUID);
                unconnectedWorldUUIDs.remove(worldUUID);
            } else {
                worldUUIDToPlayerUUIDMap.remove(worldUUID);
                unconnectedWorldUUIDs.add(worldUUID);
            }

            // Update cached world numbers
            if (worldName.startsWith("First_Land_World_")) {
                try {
                    existingWorldNumbers.add(Integer.parseInt(worldName.substring("First_Land_World_".length())));
                } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {}
            }

            // Update file data
            config.set(worldPath + ".worldname", worldName);
            config.set(worldPath + ".friendly-name", friendlyName);
            config.set(worldPath + ".connected-player-uuid", playerUUID != null ? playerUUID.toString() : "none");
            config.set(worldPath + ".seed", seed);
            config.set(worldPath + ".spawnlocation", spawnLocation);
            config.set(worldPath + ".created-at", System.currentTimeMillis());

            if (isNewEntry) {
                plugin.getLogger().info("Added NEW world entry for UUID '" + worldUUID + "'.");
            } else {
                plugin.getLogger().info("UPDATED existing world entry for UUID '" + worldUUID + "'.");
            }
        }
    }
    /**
     * Finds the UUID of the first FirstLandWorld that is not connected to a player.
     * This method is crucial for reusing existing worlds instead of always creating new ones.
     *
     * @return The UUID of an unconnected world, or null if all are connected.
     */

    public UUID findFirstUnconnectedWorldUUID() {
        if (!unconnectedWorldUUIDs.isEmpty()) {
            return unconnectedWorldUUIDs.iterator().next();
        }
        return null;
    }


    /**
     * Retrieves a list of all World UUIDs stored in the configuration.
     * @return A list of World UUIDs.
     */
    public List<UUID> getKnownWorldUUIDs() {
        return new ArrayList<>(worldUUIDToInternalNameMap.keySet());
    }

    /**
     * Checks if a world with the given UUID exists in the configuration.
     * @param worldUUID The UUID of the world.
     * @return true if the world exists in config, false otherwise.
     */

    public boolean worldExistsInConfig(UUID worldUUID) {
        return worldUUIDToInternalNameMap.containsKey(worldUUID);
    }

    /**
     * Gets the internal Bukkit world name associated with a given World UUID.
     * @param worldUUID The UUID of the world.
     * @return The internal Bukkit world name, or null if not found.
     */
    public String getWorldName(UUID worldUUID) {
        return worldUUIDToInternalNameMap.get(worldUUID);
    }


    /**
     * Gets the seed associated with a given World UUID.
     * @param worldUUID The UUID of the world.
     * @return The seed, or null if not found.
     */
    public Long getWorldSeed(UUID worldUUID) {
        return worldUUIDToSeedMap.getOrDefault(worldUUID, 0L);
    }

    /**
     * Retrieves the connected Player UUID for a specific world identified by its UUID.
     * @param worldUUID The UUID of the world.
     * @return The UUID of the connected player, or null if unconnected.
     */
    public UUID getConnectedPlayerUUID(UUID worldUUID) {
        return worldUUIDToPlayerUUIDMap.get(worldUUID);
    }


    /**
     * Updates the connected player for a specific world identified by its UUID.
     * This method directly uses the world's UUID to update its connected player.
     *
     * @param worldUUID The UUID of the world to update.
     * @param playerUUID The UUID of the player to connect (or null to disconnect).
     */
    public void updateConnectedPlayer(UUID worldUUID, UUID playerUUID) {
        synchronized (fileLock) {
            String playerUUIDString = (playerUUID != null) ? playerUUID.toString() : "none";
            if (worldUUID != null && worldUUIDToInternalNameMap.containsKey(worldUUID)) {
                config.set("worlds." + worldUUID.toString() + ".connected-player-uuid", playerUUIDString);

                if (playerUUID != null) {
                    worldUUIDToPlayerUUIDMap.put(worldUUID, playerUUID);
                    unconnectedWorldUUIDs.remove(worldUUID);
                } else {
                    worldUUIDToPlayerUUIDMap.remove(worldUUID);
                    unconnectedWorldUUIDs.add(worldUUID);
                }
                saveConfig();
                plugin.getLogger().info("Updated player connection for world UUID '" + worldUUID + "'.");
            } else {
                plugin.getLogger().warning("Attempted to update connected player for a null or unknown world UUID.");
            }
        }
    }

    /**
     * Retrieves the friendly name for a given World UUID.
     *
     * @param worldUUID The UUID of the world.
     * @return The friendly name of the world, or its internal Bukkit name if no friendly name is found.
     */

    public String getWorldFriendlyName(UUID worldUUID) {
        return worldUUIDToFriendlyNameMap.get(worldUUID);
    }

    /**
     * Retrieves a list of World UUIDs owned by a specific player (identified by their UUID).
     *
     * @param playerUUID The UUID of the player.
     * @return A list of World UUIDs owned by the player.
     */

    public List<UUID> getPlayerOwnedWorldUUIDs(UUID playerUUID) {
        List<UUID> ownedWorlds = new ArrayList<>();
        for (Map.Entry<UUID, UUID> entry : worldUUIDToPlayerUUIDMap.entrySet()) {
            if (entry.getValue().equals(playerUUID)) {
                ownedWorlds.add(entry.getKey());
            }
        }
        return ownedWorlds;
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
            return false;
        }
        for (Map.Entry<UUID, UUID> entry : worldUUIDToPlayerUUIDMap.entrySet()) {
            if (entry.getValue().equals(playerUUID)) {
                String existingFriendlyName = worldUUIDToFriendlyNameMap.get(entry.getKey());
                if (existingFriendlyName != null && existingFriendlyName.equalsIgnoreCase(friendlyName)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Finds and returns a set of all world numbers from the config file.
     * This is a helper method used by other methods that need to check for
     * available world numbers.
     * @return A Set of all integers used as world numbers.
     */
    public Set<Integer> getExistingWorldNumbers() {
        return existingWorldNumbers;
    }

    /**
     * Finds the lowest available integer for a new First Land world.
     * This method now uses the getExistingWorldNumbers() helper.
     * @return The lowest available integer for a new world.
     */
    public int findLowestAvailableWorldNumber() {
        int nextNumber = 0;
        while (existingWorldNumbers.contains(nextNumber)) {
            nextNumber++;
        }
        return nextNumber;
    }


    /**
     * Gets the spawn location for a world identified by its UUID.
     * Requires the world to be loaded to get a valid World object.
     * @param worldUUID The UUID of the world.
     * @return The Location object, or null if world not loaded or spawn not found.
     */
    public Location getWorldSpawnLocation(UUID worldUUID) {
        // Return the cached Location object directly
        return worldUUIDToSpawnLocationMap.get(worldUUID);
    }


    /**
     * Deletes a world entry from the configuration by its UUID.
     * @param worldUUID The UUID of the world to delete.
     */
    public void deleteWorldEntry(UUID worldUUID) {
        synchronized (fileLock) {
            config.set("worlds." + worldUUID.toString(), null);

            // Remove from all in-memory caches
            worldUUIDToPlayerUUIDMap.remove(worldUUID);
            unconnectedWorldUUIDs.remove(worldUUID);
            worldUUIDToFriendlyNameMap.remove(worldUUID);
            String internalName = worldUUIDToInternalNameMap.remove(worldUUID);
            if (internalName != null) {
                internalNameToWorldUUIDMap.remove(internalName);
                if (internalName.startsWith("First_Land_World_")) {
                    try {
                        existingWorldNumbers.remove(Integer.parseInt(internalName.substring("First_Land_World_".length())));
                    } catch (NumberFormatException | StringIndexOutOfBoundsException ignored) {}
                }
            }
            worldUUIDToSeedMap.remove(worldUUID);
            worldUUIDToSpawnLocationMap.remove(worldUUID);

            saveConfig();
            plugin.getLogger().info("Deleted world entry for UUID '" + worldUUID + "'.");
        }
    }

    /**
     * Updates the 'amount' field in the config to the highest existing world number + 1.
     * This ensures that new worlds always get a unique sequential internal name.
     */
    public void updateAmountToHighestWorldNumber() {
        synchronized (fileLock) {
            int highestNumber = -1;
            for (Integer num : existingWorldNumbers) {
                if (num > highestNumber) {
                    highestNumber = num;
                }
            }
            // Set amount to one higher than the highest found number, or 0 if no worlds exist
            this.amount = highestNumber + 1;
            config.set("amount", this.amount);
            saveConfig();
            plugin.getLogger().info("Updated world amount to: " + this.amount);
        }
    }

    /**
     * Retrieves the current `amount` value, which typically represents the next available world number.
     * @return The current amount of worlds.
     */
    public int getWorldAmount() {
        synchronized (fileLock) {
            return this.amount; // Return cached amount for speed
        }
    }

    /**
     * Retrieves the maximum number of worlds a player is allowed to own from the main plugin's configuration.
     * @return The maximum number of worlds, or the default from Chuachua.java if not specified.
     */
    public int getMaxWorldsPerPlayer() {
        if (plugin instanceof Chuachua) {
            // Assuming 'Chuachua' is your main plugin class that has this config
            return plugin.getConfig().getInt("max-worlds-per-player", 3);
        }
        return 3; // Default fallback
    }

    /**
     * Gets the World UUID associated with a given internal Bukkit world name.
     * This is useful for looking up worlds by their internal name.
     * @param internalWorldName The internal Bukkit name of the world.
     * @return The UUID of the world, or null if not found.
     */
    public UUID getChuaWorldUUIDByInternalName(String internalWorldName) {
        return internalNameToWorldUUIDMap.get(internalWorldName);
    }
}