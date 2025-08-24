package org.byeautumn.chuachua;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.byeautumn.chuachua.common.LocationVector;
import org.byeautumn.chuachua.game.firstland.FirstLandWorldConfigAccessor;
import org.byeautumn.chuachua.generate.world.WorldManager;
import org.byeautumn.chuachua.generate.world.pipeline.*;
import org.byeautumn.chuachua.generate.world.WorldGenerator;
import org.byeautumn.chuachua.player.PlayerDataCommon;
import org.byeautumn.chuachua.player.PlayerTracker;
import org.byeautumn.chuachua.undo.ActionRecorder;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Universe {
    private static final String LOBBY_WORLD_NAME = "world";
    private static final LocationVector LOBBY_SPAWN_LOCATION_VECTOR = new LocationVector(24.0, -60.0, 32.0);

    private static Map<UUID, PlayerTracker> PLAYER_ID_TO_TRACKER_MAP = new HashMap<>();
    private static Map<World, List<Block>> worldToPlayerPlacedBlocks = new HashMap<>();
    private static Map<UUID, ActionRecorder> PLAYER_ID_TO_RECORDER_MAP = new HashMap<>();

    // Consolidated map for all active ChuaWorld instances, keyed by Bukkit World UID
    private static final Map<UUID, ChuaWorld> UUID_TO_CHUAWORLD = new HashMap<>();

    // Map to track which player is connected to which ChuaWorld (Player UUID -> ChuaWorld)
    private static final Map<UUID, List<ChuaWorld>> PLAYER_UUID_TO_CONNECTED_CHUAWORLD_MAPS = new HashMap<>();

    // Used by doesWorldExist, still relying on names for native Bukkit world checks
    private static Set<String> bukkitWorldSet;

    private static File playerDataDirectory;
    private static JavaPlugin pluginInstance;

    public static void teleport(Player player, Location toLocation){
        player.teleport(toLocation);
    }

    private static Location getLocation(String worldName, LocationVector vector) {
        return new Location(Bukkit.getWorld(worldName), vector.getX(), vector.getY(), vector.getZ());
    }

    public static void teleportToLobby(Player player, FirstLandWorldConfigAccessor configAccessor) { // Added configAccessor parameter
        Location toLocation = getLocation(LOBBY_WORLD_NAME, LOBBY_SPAWN_LOCATION_VECTOR);
        teleportPlayerWithInventoryManagement(player, toLocation, pluginInstance, configAccessor); // Pass configAccessor
        player.sendMessage(ChatColor.GREEN + "You were teleported to lobby successfully");
    }

    /**
     * Gets the PlayerTracker for a given player. If it doesn't exist, it creates one
     * and attempts to load their PlayerDataCommon from a JSON file.
     * @param player The Bukkit player.
     * @return The PlayerTracker for the player.
     */
    public static PlayerTracker getPlayerTracker(Player player){
        // The computeIfAbsent method is ideal here. It will create a new PlayerTracker
        // only if one doesn't already exist for the player's UUID.
        return PLAYER_ID_TO_TRACKER_MAP.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerTracker newTracker = new PlayerTracker(player);
            // Attempt to load PlayerDataCommon from file
            File playerFile = new File(playerDataDirectory, uuid.toString() + ".json");
            try {
                PlayerDataCommon loadedData = PlayerDataCommon.loadFromJsonFile(playerFile.getAbsolutePath());
                if (loadedData != null) {
                    newTracker.setPlayerDataCommon(loadedData);
                    pluginInstance.getLogger().info("Loaded PlayerDataCommon for " + player.getName() + " from " + playerFile.getName());
                } else {
                    pluginInstance.getLogger().info("No existing PlayerDataCommon found for " + player.getName() + ". Creating new default data.");
                    // If no file exists, newTracker retains its default PlayerDataCommon, which is fine.
                }
            } catch (IOException | JSONException e) {
                pluginInstance.getLogger().log(Level.SEVERE, "Error loading PlayerDataCommon for " + player.getName() + " from " + playerFile.getName(), e);
                // In case of error, the PlayerTracker will retain its default new PlayerDataCommon.
                // This prevents server crashes due to corrupted player files.
            }
            return newTracker;
        });
    }

    /**
     * Resets the player's tracker and immediately saves the new default state to the JSON file.
     * @param player The player whose tracker to reset.
     */
    public static void resetPlayerTracker(Player player) {
        PlayerTracker playerTracker = getPlayerTracker(player);
        playerTracker.reset();
        // After resetting, immediately save the new default state to persist the changes.
        savePlayerCommonData(player.getUniqueId());
    }

    public static World getLobby() {
        return Bukkit.getWorld(LOBBY_WORLD_NAME);
    }

    public static void markPlayerPlacedBlock(World world, Block block) {
        if (!worldToPlayerPlacedBlocks.containsKey(world)) {
            worldToPlayerPlacedBlocks.put(world, new ArrayList<>());
        }
        worldToPlayerPlacedBlocks.get(world).add(block);
    }

    public static boolean areBlocksSame(Block block1, Block block2) {
        if (null == block1 || null == block2) {
            return false;
        }
        Location location1 = block1.getLocation();
        Location location2 = block2.getLocation();

        return block1.getType() == block2.getType() && areLocationsIdentical(location1, location2);
    }

    public static boolean areLocationsIdentical(Location location1, Location location2) {
        if (null == location1 || null == location2) {
            return false;
        }
        World world1 = location1.getWorld();
        World world2 = location2.getWorld();
        if (null == world1 || null == world2) {
            return false;
        }
        if (world1.getName().equals(world2.getName())) {
            double distance = location2.distance(location1);
            return distance <= 0.00001;
        }

        return false;
    }

    public static ActionRecorder getActionRecorder(Player player) {
        if (!PLAYER_ID_TO_RECORDER_MAP.containsKey(player.getUniqueId())) {
            PLAYER_ID_TO_RECORDER_MAP.put(player.getUniqueId(), new ActionRecorder());
        }

        return PLAYER_ID_TO_RECORDER_MAP.get(player.getUniqueId());
    }

    /**
     * Retrieves a ChuaWorld object by its associated Bukkit World's UID.
     * This method now consistently uses `UUID_TO_CHUAWORLD`.
     *
     * @param id The UUID of the Bukkit World.
     * @return The ChuaWorld instance, or null if not found.
     */
    public static ChuaWorld getChuaWorldById(UUID id) {
        return UUID_TO_CHUAWORLD.get(id);
    }

    /**
     * Adds a ChuaWorld object to the active in-memory map.
     * This method now consistently uses `UUID_TO_CHUAWORLD`.
     *
     * @param chuaWorld The ChuaWorld instance to add.
     */
    public static void addChuaWorld(ChuaWorld chuaWorld) {
        UUID_TO_CHUAWORLD.put(chuaWorld.getID(), chuaWorld);
    }

    public static CompletableFuture<ChuaWorld> createWorldAsync(long seed, String worldName, JavaPlugin plugin) {
        CompletableFuture<ChuaWorld> future = new CompletableFuture<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                // This block runs on the Main Server Thread
                // This is where the actual world creation (which is blocking and uses your generator) occurs.
                ChuaWorld resultWorld = createWorld(seed, worldName, plugin); // Call the synchronous createWorld
                future.complete(resultWorld); // Complete the future with the result
            }
        }.runTask(plugin); // Schedule to run on the main thread

        return future;
    }

    /**
     * Creates a new Bukkit World and wraps it in a ChuaWorld object.
     * This method handles the world generation pipeline.
     *
     * @param seed The seed for the new world.
     * @param worldName The name for the new world.
     * @param plugin The main JavaPlugin instance for logging.
     * @return The newly created ChuaWorld, or null if creation failed.
     */
    public static ChuaWorld createWorld(long seed, String worldName, JavaPlugin plugin) {
        // 1. Check if the world is already loaded in Bukkit and managed by our plugin
        World existingBukkitWorld = Bukkit.getWorld(worldName);
        if (existingBukkitWorld != null) {
            ChuaWorld existingChuaWorld = getChuaWorldById(existingBukkitWorld.getUID());

            if (existingChuaWorld != null) {
                // World is loaded AND managed by us. Nothing to do, return it.
                plugin.getLogger().info("World '" + worldName + "' (UUID: " + existingBukkitWorld.getUID() + ") is already loaded and managed. Reusing existing ChuaWorld.");
                return existingChuaWorld;
            } else {
                // --- NEW FIX ---
                // World is loaded, but not in our in-memory map. Let's check if it's a world we created.
                if (worldName.startsWith("First_Land_World_")) {
                    plugin.getLogger().info("Found a loaded First Land World '" + worldName + "' not in our map. Re-wrapping it as a ChuaWorld.");
                    // Assume it's one of ours, load the needed data from the config
                    // and add it to our internal map to fix the discrepancy.
                    FirstLandWorldConfigAccessor configAccessor = new FirstLandWorldConfigAccessor(plugin);
                    Long storedSeed = configAccessor.getWorldSeed(existingBukkitWorld.getUID());

                    if (storedSeed != null) {
                        ChuaWorld rewrappedWorld = new ChuaWorld(storedSeed, existingBukkitWorld);
                        addChuaWorld(rewrappedWorld);
                        return rewrappedWorld;
                    } else {
                        plugin.getLogger().warning("Could not find config data for existing First Land World '" + worldName + "'. Aborting creation to prevent data loss.");
                        return null;
                    }
                } else {
                    // World is loaded in Bukkit but NOT managed by us (e.g., vanilla world or other plugin's world).
                    plugin.getLogger().warning("Attempted to create world '" + worldName + "' but a Bukkit world with that name already exists and is NOT managed by ChuaChua. Aborting creation.");
                    return null;
                }
            }
        }

        // 2. If not loaded, check if the world folder exists on disk (meaning it's unloaded but exists)
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            plugin.getLogger().info("World folder '" + worldName + "' exists on disk, but world is not loaded. Attempting to load existing world with provided seed: " + seed);
            // Use WorldCreator to load the existing world. If the world data on disk is valid,
            // Bukkit will load it. The seed is passed for consistency but primarily applies to new generation.
            World bukkitWorldToLoad = new WorldCreator(worldName).seed(seed).createWorld();

            if (bukkitWorldToLoad != null) {
                // World is now loaded. Create ChuaWorld wrapper if not already in our map.
                ChuaWorld chuaWorld = getChuaWorldById(bukkitWorldToLoad.getUID());
                if (chuaWorld == null) {
                    chuaWorld = new ChuaWorld(seed, bukkitWorldToLoad);
                    addChuaWorld(chuaWorld);
                    plugin.getLogger().info("Successfully loaded existing world '" + worldName + "' (UUID: " + bukkitWorldToLoad.getUID() + ") and wrapped into ChuaWorld.");
                }
                return chuaWorld;
            } else {
                plugin.getLogger().severe("Failed to load existing world '" + worldName + "' from disk, even though its folder exists. Check server console for Bukkit errors related to world loading.");
                return null; // Failed to load existing world
            }
        }

        // 3. If neither loaded nor exists on disk, then it's truly a new world to be generated.
        plugin.getLogger().info("World '" + worldName + "' does not exist on disk or loaded. Starting creation of a BRAND NEW world with seed: " + seed);

        Map<Integer, ChunkGenerationStage> stages = initializeGenerationPipeline(seed);
        Logger pluginLogger = plugin.getLogger(); // Use the passed plugin's logger

        WorldGenerator worldGenerator = new WorldGenerator(stages, pluginLogger);
        World newBukkitWorld = WorldManager.createWorld(worldName, worldGenerator);

        if (newBukkitWorld != null) {
            newBukkitWorld.setGameRuleValue("doMobSpawning", "false");
            ChuaWorld chuaWorld = new ChuaWorld(seed, newBukkitWorld);
            addChuaWorld(chuaWorld); // Adds to UUID_TO_CHUAWORLD
            plugin.getLogger().info("Successfully created BRAND NEW ChuaWorld for: " + worldName + " (UUID: " + newBukkitWorld.getUID() + ").");
            return chuaWorld;
        }

        plugin.getLogger().severe("Failed to create Bukkit world for '" + worldName + "'.");
        return null;
    }

    // Kept for backward compatibility with existing usage of worldName in some helper checks
    private static boolean isWorldAlreadyLoaded(String worldName) {
        return Bukkit.getWorld(worldName) != null;
    }

    private static Map<Integer, ChunkGenerationStage> initializeGenerationPipeline(long createSeed) {
        Map<Integer, ChunkGenerationStage> stages = new TreeMap<>();
        stages.put(0, new ProtoRegionGeneration(createSeed));
        stages.put(1, new ProtoBiomeAssignment());
        stages.put(2, new ProtoTerrainGeneration(createSeed));
        return stages;
    }

    // This method is no longer strictly necessary if all logging goes through a passed plugin instance,
    // but kept for compatibility.
    private static Logger getPluginLogger() {
        try {
            return JavaPlugin.getPlugin(Chuachua.class).getLogger();
        } catch (IllegalStateException e) {
            System.err.println("Plugin 'Chuachua' not yet loaded or not found. Using System.err for logging.");
            Logger fallbackLogger = Logger.getLogger("ChuaWorldCreatorFallback");
            fallbackLogger.setLevel(Level.WARNING);
            return fallbackLogger;
        }
    }

    /**
     * Loads all known ChuaWorlds from the provided accessor's configuration into the active in-memory map.
     * This method is a general utility for loading any type of ChuaWorld.
     * This replaces `loadChuaWorldsToMap`.
     *
     * @param accessor The FirstLandWorldConfigAccessor to read world data from.
     */
    public static void loadAllChuaWorldsIntoActiveMap(FirstLandWorldConfigAccessor accessor){
        List<UUID> knownWorldUUIDs = accessor.getKnownWorldUUIDs(); // Use UUIDs
        Logger logger = getPluginLogger(); // Use the plugin's logger
        logger.info("Loading " + knownWorldUUIDs.size() + " known ChuaWorlds from config into memory...");

        for (UUID worldUUID : knownWorldUUIDs){ // Iterate by UUID
            String worldName = accessor.getWorldName(worldUUID); // Get internal name for Bukkit calls
            if (worldName == null) {
                logger.warning("Skipping world with UUID '" + worldUUID + "'. Internal name not found in config.");
                continue;
            }

            World bukkitWorld = Bukkit.getWorld(worldName);
            if (bukkitWorld == null) {
                Long seed = accessor.getWorldSeed(worldUUID);
                if (seed != null) {
                    bukkitWorld = new WorldCreator(worldName).seed(seed).createWorld();
                }
            }

            if (bukkitWorld != null) {
                ChuaWorld chuaWorld = getChuaWorldById(bukkitWorld.getUID());
                if (chuaWorld == null) {
                    Long seed = accessor.getWorldSeed(worldUUID); // Get seed for ChuaWorld constructor
                    if (seed != null) {
                        chuaWorld = new ChuaWorld(seed, bukkitWorld);
                        addChuaWorld(chuaWorld); // Add to UUID_TO_CHUAWORLD
                        logger.info("Wrapped existing Bukkit world '" + worldName + "' (UUID: " + worldUUID + ") into ChuaWorld and added to active map.");
                    } else {
                        logger.warning("Seed not found for world " + worldName + " (UUID: " + worldUUID + "). Cannot create ChuaWorld wrapper during load.");
                    }
                }
            } else {
                logger.warning("World '" + worldName + "' (UUID: " + worldUUID + ") could not be loaded or found. Skipping in-memory loading.");
            }
        }
        logger.info("Finished loading all known ChuaWorlds into memory. Total in active map: " + UUID_TO_CHUAWORLD.size());
    }

    // This method seems redundant or for an older accessor, keeping for now
    public static void loadChuaWorldsToMap(ChuaWorldConfigAccessor accessor, JavaPlugin plugin){
        List<String> chuaWorlds = accessor.getKnownWorlds();
        System.out.println("getKnowWorlds: " + accessor.getKnownWorlds());
        for (String chuaWorldString : chuaWorlds){
            // Calls createWorld with plugin
            ChuaWorld chuaWorld = createWorld(accessor.getWorldSeed(chuaWorldString), chuaWorldString, plugin);
            System.out.println("accessor.getWorldSeed(chuaWorldString: " + accessor.getWorldSeed(chuaWorldString));
            if (null == chuaWorld) {
                System.out.println("World creation for '" + chuaWorldString + "' skipped.");
                continue;
            } else {
                System.out.println("World created " + chuaWorldString);
            }

            addChuaWorld(chuaWorld);
            System.out.println("UUID_TO_CHUAWORLD: " + UUID_TO_CHUAWORLD);
        }
    }

    /**
     * Loads First Land worlds from the configuration into the active in-memory map.
     * This method specifically loads worlds managed by FirstLandWorldConfigAccessor.
     *
     * @param plugin The main plugin instance.
     * @param configAccessor The FirstLandWorldConfigAccessor to read world data from.
     */
    public static void loadFirstLandWorldsToMap(JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor) {
        List<UUID> knownWorldUUIDs = configAccessor.getKnownWorldUUIDs();
        Logger logger = plugin.getLogger();

        logger.info("Loading " + knownWorldUUIDs.size() + " First Land worlds from configuration into memory...");

        for (UUID worldUUID : knownWorldUUIDs) {
            String worldName = configAccessor.getWorldName(worldUUID);
            if (worldName == null) {
                logger.warning("Skipping First Land world with UUID '" + worldUUID + "'. Internal name not found in config.");
                continue;
            }

            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (!worldFolder.exists()) {
                logger.warning("World folder for First Land world '" + worldName + "' (UUID: " + worldUUID + ") not found. Deleting config entry.");
                configAccessor.deleteWorldEntry(worldUUID); // Delete invalid entry
                continue;
            }

            // Attempt to load the world into Bukkit and wrap it in ChuaWorld
            World bukkitWorld = Bukkit.getWorld(worldName);
            if (bukkitWorld == null) {
                Long seed = configAccessor.getWorldSeed(worldUUID);
                if (seed != null) {
                    bukkitWorld = new WorldCreator(worldName).seed(seed).createWorld();
                }
            }

            if (bukkitWorld != null) {
                ChuaWorld chuaWorld = getChuaWorldById(bukkitWorld.getUID());
                if (chuaWorld == null) {
                    Long seed = configAccessor.getWorldSeed(worldUUID);
                    if (seed != null) {
                        chuaWorld = new ChuaWorld(seed, bukkitWorld);
                        UUID_TO_CHUAWORLD.put(bukkitWorld.getUID(), chuaWorld);
                    } else {
                        logger.warning("Seed not found for world " + worldName + ". Cannot create ChuaWorld wrapper during connection load.");
                    }
                }
            } else {
                logger.warning("First Land world '" + worldName + "' (UUID: " + worldUUID + ") could not be loaded. Skipping.");
            }
        }
        logger.info("Finished loading First Land worlds. Total in active map: " + UUID_TO_CHUAWORLD.size());
    }

    /**
     * Loads player-to-ChuaWorld connections from the configuration file into the in-memory map.
     * This method should be called during plugin startup to restore active connections.
     *
     * @param plugin The main plugin instance.
     * @param configAccessor The FirstLandWorldConfigAccessor to read world data from.
     */
    public static void loadPlayerConnectionsFromConfig(JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor) {
        // Iterate through all world UUIDs cached in the accessor for better performance
        for (UUID worldUUID : configAccessor.getKnownWorldUUIDs()) {
            // Get the connected player's UUID directly from the cache
            UUID connectedPlayerUUID = configAccessor.getConnectedPlayerUUID(worldUUID);

            // Check if a player is connected to this world
            if (connectedPlayerUUID != null) {
                Player onlinePlayer = Bukkit.getPlayer(connectedPlayerUUID);

                // If the player is online, we load the world and establish the connection
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    String worldName = configAccessor.getWorldName(worldUUID);
                    if (worldName == null) {
                        plugin.getLogger().warning("Internal name not found for world UUID: " + worldUUID + ". Skipping connection load.");
                        configAccessor.updateConnectedPlayer(worldUUID, null);
                        continue;
                    }

                    World bukkitWorld = Bukkit.getWorld(worldName);
                    if (bukkitWorld == null) {
                        Long seed = configAccessor.getWorldSeed(worldUUID);
                        if (seed != null) {
                            bukkitWorld = new WorldCreator(worldName).seed(seed).createWorld();
                        }
                    }

                    if (bukkitWorld != null) {
                        ChuaWorld chuaWorld = Universe.getChuaWorldById(bukkitWorld.getUID());
                        if (chuaWorld == null) {
                            Long seed = configAccessor.getWorldSeed(worldUUID);
                            if (seed != null) {
                                chuaWorld = new ChuaWorld(seed, bukkitWorld);
                                Universe.addChuaWorld(chuaWorld); // Use the centralized method
                            } else {
                                plugin.getLogger().warning("Seed not found for world " + worldName + ". Cannot create ChuaWorld wrapper during connection load.");
                            }
                        }

                        if (chuaWorld != null) {
                            // Add to the map of connected worlds for the player
                            Universe.getPlayerToWorldsMap().computeIfAbsent(connectedPlayerUUID, k -> new ArrayList<>()).add(chuaWorld);
                            plugin.getLogger().info("Loaded connection: Player " + onlinePlayer.getName() + " -> World " + configAccessor.getWorldFriendlyName(worldUUID));
                        } else {
                            plugin.getLogger().warning("Failed to get or create ChuaWorld for " + worldName + " during load.");
                        }
                    } else {
                        plugin.getLogger().warning("World " + worldName + " associated with " + onlinePlayer.getName() + " could not be loaded during startup.");
                    }
                } else {
                    // Player is not online, so we clear their connection in config to make the world available
                    plugin.getLogger().info("Player " + connectedPlayerUUID + " for world " + worldUUID + " is not online. Resetting world status to 'none'.");
                    configAccessor.updateConnectedPlayer(worldUUID, null);
                }
            }
        }
    }


    private static void loadBukkitWorldSet() {
        // This method is for basic native Bukkit world existence checks if needed.
        bukkitWorldSet = Bukkit.getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public static boolean doesWorldExist(String worldName){
        if (null == bukkitWorldSet) {
            loadBukkitWorldSet();
        }
        return bukkitWorldSet.contains(worldName);
    }

    /**
     * Unloads a world, teleports players to the lobby, removes the world from
     * all in-memory caches, deletes its config entry, and permanently deletes its folder.
     * This is a critical and irreversible operation.
     * This method must be called on the main server thread.
     * @param plugin The main plugin instance.
     * @param worldUUIDToDelete The UUID of the world to delete.
     * @param configAccessor The accessor for the world configuration.
     * @return true if the deletion was successful, false otherwise.
     */
    public static boolean deleteFirstLandWorld(JavaPlugin plugin, UUID worldUUIDToDelete, FirstLandWorldConfigAccessor configAccessor) {
        String internalWorldName = configAccessor.getWorldName(worldUUIDToDelete);
        if (internalWorldName == null) {
            plugin.getLogger().warning("Attempted to delete a world (UUID: " + worldUUIDToDelete + ") that does not exist in the configuration.");
            return false;
        }

        // 1. Unload the world gracefully, teleporting players out first
        World worldToDelete = Bukkit.getWorld(internalWorldName);
        if (worldToDelete != null) {
            // Teleport players to the lobby before unloading the world
            for (Player p : worldToDelete.getPlayers()) {
                // --- MODIFIED: Use inventory management teleport for players leaving this world and pass configAccessor ---
                teleportPlayerWithInventoryManagement(p, getLocation(LOBBY_WORLD_NAME, LOBBY_SPAWN_LOCATION_VECTOR), plugin, configAccessor);
                p.sendMessage(ChatColor.RED + "The world you were in is being deleted. You have been moved to the lobby.");
            }
            if (!Bukkit.unloadWorld(worldToDelete, true)) {
                plugin.getLogger().log(Level.SEVERE, "Failed to unload world '" + internalWorldName + "' prior to deletion. Aborting file deletion to prevent data corruption.");
                return false;
            }
        } else {
            plugin.getLogger().info("World '" + internalWorldName + "' was not loaded, proceeding with file and config deletion.");
        }

        // 2. Remove from in-memory caches
        ChuaWorld chuaWorld = getChuaWorldById(worldUUIDToDelete);
        if (chuaWorld != null) {
            removeChuaWorld(chuaWorld);
        }

        // 3. Delete from configuration file and cache
        configAccessor.deleteWorldEntry(worldUUIDToDelete);

        // 4. Delete the world folder
        File worldFolder = new File(Bukkit.getWorldContainer(), internalWorldName);
        try {
            if (deleteWorldFolder(worldFolder)) {
                plugin.getLogger().info("Successfully deleted world folder: " + worldFolder.getAbsolutePath());
                return true;
            } else {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete world folder: " + worldFolder.getAbsolutePath());
                return false;
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "An I/O error occurred while deleting world folder: " + worldFolder.getAbsolutePath(), e);
            return false;
        }
    }

    /**
     * Recursively deletes a directory and its contents.
     * @param path The file or directory to delete.
     * @return true if successful, false otherwise.
     * @throws IOException if an I/O error occurs.
     */
    private static boolean deleteWorldFolder(File path) throws IOException {
        if (!path.exists()) return true;

        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!deleteWorldFolder(file)) {
                        return false;
                    }
                }
            }
        }
        return path.delete();
    }

    /**
     * Checks if a given world name is one of the vanilla server worlds.
     * @param worldName The name of the world to check.
     * @return true if the world is a vanilla world, false otherwise.
     */
    public static boolean isVanillaWorld(String worldName) {
        return worldName.equalsIgnoreCase("world") || worldName.equalsIgnoreCase("world_nether") || worldName.equalsIgnoreCase("world_the_end");
    }

    /**
     * Creates a new First Land world with a specified friendly name and connects the player to it.
     * This method is called when a player wants to create a world with a custom name.
     *
     * @param player The player initiating the world creation.
     * @param plugin The main plugin instance.
     * @param configAccessor The configuration accessor for world data.
     * @param friendlyName The user-defined friendly name for the new world.
     * @return true if the world was successfully created and the player connected, false otherwise.
     */
    public static boolean createAndConnectNewWorld(Player player, JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor, String friendlyName) {
        player.sendMessage(org.bukkit.ChatColor.YELLOW + "Creating a new First Land world for you...");

        // Determine the next internal world name (e.g., First_Land_World_00, _01, etc.)
        int worldNumber = configAccessor.getWorldAmount(); // Gets the current 'amount' which is the next available number
        String formattedNumber = String.format("%02d", worldNumber); // Ensures two digits (e.g., 00, 01, 02)
        String newWorldName = "First_Land_World_" + formattedNumber;

        // Generate a random seed for the new world
        Random random = new Random();
        long seed = random.nextLong();

        // Create the actual Bukkit world and its ChuaWorld wrapper
        ChuaWorld targetChuaWorld = Universe.createWorld(seed, newWorldName, plugin); // Pass plugin here
        if (targetChuaWorld == null) {
            player.sendMessage(org.bukkit.ChatColor.RED + "Failed to create a new First Land world for you.");
            return false;
        }

        // Add the new world's details to the plugin's configuration file using UUID
        configAccessor.addNewWorld(targetChuaWorld.getID(), newWorldName, friendlyName, player.getUniqueId(), seed, targetChuaWorld.getWorld().getSpawnLocation());

        // Update the in-memory map to reflect that this player is now connected to this new world
        Universe.setPlayerConnectedChuaWorld(player.getUniqueId(), targetChuaWorld, plugin);
        configAccessor.saveConfig();

        // Schedule the player teleportation to happen on the main server thread
        new BukkitRunnable() {
            @Override
            public void run() {
                player.teleport(targetChuaWorld.getWorld().getSpawnLocation());
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Welcome to your new First Land world: " + friendlyName + "!");
            }
        }.runTask(plugin);

        return true;
    }

    /**
     * Overloaded method to create a new First Land world with a default friendly name
     * and connect the player to it. The friendly name will be the internal world name.
     *
     * @param player The player initiating the world creation.
     * @param plugin The main plugin instance.
     * @param configAccessor The configuration accessor for world data.
     * @return true if the world was successfully created and the player connected, false otherwise.
     */
    public static boolean createAndConnectNewWorld(Player player, JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor) {
        // Determine the next internal world name, which will also serve as the default friendly name
        int worldNumber = configAccessor.getWorldAmount();
        String formattedNumber = String.format("%02d", worldNumber);
        String newWorldName = "First_Land_World_" + formattedNumber;

        // Call the primary overloaded method, passing the generated name as the friendly name
        return createAndConnectNewWorld(player, plugin, configAccessor, newWorldName);
    }

    public static void createOrConnectExistingWorldWithPlayer(Player player, JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor, String friendlyName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Universe.findOrCreateAndConnectFirstLandWorld(player, plugin, configAccessor, friendlyName);
            }
        }.runTask(plugin);
    }


    public static boolean connectPlayerToSpecificWorld(Player player, JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor, String worldIdentifier, UUID worldUuid) {
        plugin.getLogger().info("Attempting to connect player " + player.getName() + " to specific world: " + worldIdentifier);

        UUID targetWorldUUID = null;
        String targetInternalWorldName = null;
        Long targetSeed = null;

        // Try to parse worldIdentifier as a UUID first
        try {
            targetWorldUUID = UUID.fromString(worldIdentifier);
            // If it's a valid UUID, get details from config
            if (configAccessor.worldExistsInConfig(targetWorldUUID)) {
                targetInternalWorldName = configAccessor.getWorldName(targetWorldUUID);
                targetSeed = configAccessor.getWorldSeed(targetWorldUUID);
            } else {
                targetWorldUUID = null; // Reset if UUID is valid but not found in config
            }
        } catch (IllegalArgumentException e) {
            // Not a valid UUID, assume it's an internal world name
            targetInternalWorldName = worldIdentifier;
            targetWorldUUID = configAccessor.getChuaWorldUUIDByInternalName(targetInternalWorldName);
            if (targetWorldUUID != null) {
                targetSeed = configAccessor.getWorldSeed(targetWorldUUID);
            }
        }

        if (targetInternalWorldName == null || targetWorldUUID == null || targetSeed == null) {
            player.sendMessage(ChatColor.RED + "World '" + worldIdentifier + "' not found or data is incomplete.");
            plugin.getLogger().warning("Failed to find world data for identifier: " + worldIdentifier);
            return false;
        }

        // Now that we have a valid UUID, internal name, and seed from config, try to load/create the Bukkit world
        ChuaWorld targetChuaWorld = Universe.createWorld(targetSeed, targetInternalWorldName, plugin);

        if (targetChuaWorld == null) {
            player.sendMessage(ChatColor.RED + "Failed to prepare world '" + worldIdentifier + "' for connection. It might be corrupted.");
            plugin.getLogger().severe("Failed to load/create world '" + worldIdentifier + "' for direct connection. ChuaWorld is null.");
            return false;
        }

        // Ensure the player is connected to this world in memory and config
        Universe.setPlayerConnectedChuaWorld(player.getUniqueId(), targetChuaWorld, plugin);
        configAccessor.updateConnectedPlayer(targetChuaWorld.getID(), player.getUniqueId());

        new BukkitRunnable() {
            @Override
            public void run() {
                // --- MODIFIED: Use new inventory management teleport and pass configAccessor ---
                teleportPlayerWithInventoryManagement(player, targetChuaWorld.getWorld().getSpawnLocation(), plugin, configAccessor);
                // Get friendly name from configAccessor for display
                player.sendMessage(ChatColor.GREEN + "Successfully connected to world: " + configAccessor.getWorldFriendlyName(targetChuaWorld.getID()) + ChatColor.GREEN + "!");
                plugin.getLogger().info("Player " + player.getName() + " directly connected to world " + configAccessor.getWorldFriendlyName(targetChuaWorld.getID()) + " (UUID: " + targetChuaWorld.getID() + ").");
            }
        }.runTask(plugin);

        return true;
    }

    /**
     * Finds the first available unconnected First Land world for a player,
     * or creates a new one if none are available, and then connects the player to it.
     * This method encapsulates the logic previously found in 'connectPlayerToFirstLandWorld'.
     *
     * @param player The player to connect.
     * @param plugin The main plugin instance.
     * @param configAccessor The config accessor for First Land worlds.
     * @param friendlyName The user-defined friendly name for the new world.
     * @return true if the player was successfully connected to a world, false otherwise.
     */
    public static boolean findOrCreateAndConnectFirstLandWorld(Player player, JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor, String friendlyName) {
        plugin.getLogger().info("Attempting to find or create a First Land world for player " + player.getName() + "...");

        UUID worldUUIDToUse = null;
        String internalWorldNameToUse = null;
        Long seedToUse = null;
        ChuaWorld createdOrLoadedChuaWorld = null;

        // 1. Prioritize reusing an existing, unconnected world
        plugin.getLogger().info("Searching for an unconnected First Land world... in the Universe class");
        UUID unconnectedWorldUUID = configAccessor.findFirstUnconnectedWorldUUID();

        if (unconnectedWorldUUID != null) {
            // Reuse existing unconnected world
            worldUUIDToUse = unconnectedWorldUUID;
            internalWorldNameToUse = configAccessor.getWorldName(unconnectedWorldUUID);
            seedToUse = configAccessor.getWorldSeed(unconnectedWorldUUID);

            // Directly call Universe.createWorld which handles loading if world folder exists
            createdOrLoadedChuaWorld = Universe.createWorld(seedToUse, internalWorldNameToUse, plugin);

            if (createdOrLoadedChuaWorld == null) {
                player.sendMessage(ChatColor.RED + "Failed to prepare existing world for use. Please try again later.");
                plugin.getLogger().log(Level.SEVERE, "Failed to load existing Bukkit world: " + internalWorldNameToUse + " (UUID: " + unconnectedWorldUUID + ") for reuse. Check world files/server logs. Deleting config entry.");
                configAccessor.deleteWorldEntry(unconnectedWorldUUID); // Clean up corrupted entry
                // Fall through to new world creation attempt
            } else {
                player.sendMessage(ChatColor.GREEN + "Connecting you to an existing unused world...");
                plugin.getLogger().info("Reusing existing unconnected world (UUID: " + unconnectedWorldUUID + ", Internal: " + internalWorldNameToUse + ") for player " + player.getName());
                // World is prepared, now update its connection in config
                configAccessor.addNewWorld( // Re-use addNewWorld to update player connection
                        worldUUIDToUse,
                        internalWorldNameToUse,
                        friendlyName, // Keep its existing friendly name (or update it, depending on desired behavior)
                        player.getUniqueId(),
                        seedToUse,
                        createdOrLoadedChuaWorld.getWorld().getSpawnLocation() // Use getWorld()
                );
                configAccessor.saveConfig(); // Save changes to config file
                configAccessor.updateAmountToHighestWorldNumber(); // Update amount counter and save
            }
        }

        // 2. Create a brand new world if no unconnected ones were found OR if reuse failed
        if (createdOrLoadedChuaWorld == null) {
            plugin.getLogger().info("No unconnected worlds found or reuse failed. Creating a brand new world for " + player.getName() + ".");
            Random random = new Random();
            seedToUse = random.nextLong();
            worldUUIDToUse = UUID.randomUUID(); // Generate a new UUID for a new world

            // Get the next sequential internal world name
            String formattedNumber = String.format("%02d", configAccessor.getWorldAmount());
            internalWorldNameToUse = "First_Land_World_" + formattedNumber;

            player.sendMessage(ChatColor.GREEN + "Creating a brand new world for you...");
            plugin.getLogger().info("Creating new world '" + internalWorldNameToUse + "' (UUID: " + worldUUIDToUse + ", Seed: " + seedToUse + ") for player " + player.getName());

            // Create the actual Bukkit world and its ChuaWorld wrapper.
            createdOrLoadedChuaWorld = Universe.createWorld(seedToUse, internalWorldNameToUse, plugin); // Pass plugin

            if (createdOrLoadedChuaWorld == null) {
                player.sendMessage(ChatColor.RED + "Failed to create your new world. Please try again later.");
                plugin.getLogger().log(Level.SEVERE, "Failed to create Bukkit world for internal name: " + internalWorldNameToUse + " during player creation.");
                return false; // Abort if world creation fails
            }

            // Add the new world's details to the plugin's configuration file using UUID.
            configAccessor.addNewWorld(
                    worldUUIDToUse,
                    internalWorldNameToUse,
                    friendlyName, // Default friendly name to internal name for findOrCreate
                    player.getUniqueId(), // Connect the player who created it
                    seedToUse,
                    createdOrLoadedChuaWorld.getWorld().getSpawnLocation() // Use getWorld()
            );
            configAccessor.saveConfig(); // Save after adding new world
            configAccessor.updateAmountToHighestWorldNumber(); // Update amount counter and save
        }

        // **COMMON LOGIC FOR BOTH REUSED AND NEWLY CREATED WORLDS**
        if (createdOrLoadedChuaWorld != null) {
            // Connect the player to this world in Universe's in-memory map
            Universe.setPlayerConnectedChuaWorld(player.getUniqueId(), createdOrLoadedChuaWorld, plugin);

            final ChuaWorld finalCreatedOrLoadedChuaWorld = createdOrLoadedChuaWorld; // Fix for effectively final variable

            new BukkitRunnable() {
                @Override
                public void run() {
                    // --- MODIFIED: Use new inventory management teleport and pass configAccessor ---
                    teleportPlayerWithInventoryManagement(player, finalCreatedOrLoadedChuaWorld.getWorld().getSpawnLocation(), plugin, configAccessor);
                    player.sendMessage(ChatColor.GREEN + "You are now connected to your First Land world: " + friendlyName + ChatColor.GREEN + "!");
                    plugin.getLogger().info("Player " + player.getName() + " successfully connected to world " + friendlyName + " (UUID: " + finalCreatedOrLoadedChuaWorld.getID() + ").");
                }
            }.runTask(plugin);
            return true;
        } else {
            // This case should ideally not be reached if previous logic is sound, but it's a good fallback
            player.sendMessage(ChatColor.RED + "An unexpected error occurred during world connection. Please report this issue.");
            plugin.getLogger().severe("Unexpected error: createdOrLoadedChuaWorld was null after creation/reuse attempt for player " + player.getName());
            return false;
        }
    }


    /**
     * Gets the ChuaWorld(s) connected to a specific player from the in-memory map.
     * @param playerUUID The UUID of the player.
     * @return A list of ChuaWorld instances the player is connected to, or an empty list if none.
     */
    public static List<ChuaWorld> getPlayerConnectedChuaWorld(UUID playerUUID) {
        return PLAYER_UUID_TO_CONNECTED_CHUAWORLD_MAPS.getOrDefault(playerUUID, Collections.emptyList());
    }

    /**
     * Sets or adds a ChuaWorld to the list of worlds a player is connected to in the in-memory map.
     * This method ensures the list exists and adds the world, allowing multiple connections if desired.
     *
     * @param playerUUID The UUID of the player.
     * @param chuaWorld The ChuaWorld instance to connect.
     * @param plugin The main plugin instance for logging.
     */
    public static void setPlayerConnectedChuaWorld(UUID playerUUID, ChuaWorld chuaWorld, JavaPlugin plugin) {
        // Ensure the list exists and add the world. Note: This allows multiple connections.
        PLAYER_UUID_TO_CONNECTED_CHUAWORLD_MAPS.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(chuaWorld);
        // Do not use chuaWorld.getFriendlyName() here, as ChuaWorld does not have it.
        // Log with internal name instead or retrieve friendly name if configAccessor is available.
        plugin.getLogger().info("Player " + Bukkit.getOfflinePlayer(playerUUID).getName() + " connected to ChuaWorld: " + chuaWorld.getWorld().getName());
    }

    /**
     * Removes a player's connection from the in-memory map.
     * @param playerUUID The UUID of the player to remove the connection for.
     */
    public static void removePlayerConnectedChuaWorlds(UUID playerUUID, JavaPlugin plugin) {
        PLAYER_UUID_TO_CONNECTED_CHUAWORLD_MAPS.remove(playerUUID);
        plugin.getLogger().info("Player " + playerUUID + " removed all their connected worlds from memory.");
    }


    public static void removePlayerConnectedSpecificChuaWorld(UUID playerUUID, UUID worldUUID){
        List<ChuaWorld> chuaWorlds = PLAYER_UUID_TO_CONNECTED_CHUAWORLD_MAPS.get(playerUUID);
        // Check if chuaWorlds is null before trying to iterate over it
        if(chuaWorlds == null){
            return; // No worlds to remove the player from, so we can exit the method.
        }

        Iterator<ChuaWorld> iterator = chuaWorlds.iterator();
        while (iterator.hasNext()) {
            ChuaWorld chuaWorld = iterator.next();
            if (chuaWorld.getID().equals(worldUUID)) {
                iterator.remove();
                return; // Assuming a player is connected to a specific world only once in this list
            }
        }
    }

    /**
     * Returns the map that tracks which player is connected to which ChuaWorld(s).
     * @return A Map from Player UUID to a List of ChuaWorld instances.
     */
    public static Map<UUID, List<ChuaWorld>> getPlayerToWorldsMap() {
        return PLAYER_UUID_TO_CONNECTED_CHUAWORLD_MAPS;
    }

    /**
     * Removes a ChuaWorld from the in-memory map of managed worlds.
     * This is called when a world is unloaded or deleted to free up memory.
     * It also ensures any player connections to this world are removed.
     * @param chuaWorld The ChuaWorld instance to remove.
     */
    public static void removeChuaWorld(ChuaWorld chuaWorld) {
        if (chuaWorld != null) {
            UUID_TO_CHUAWORLD.remove(chuaWorld.getID());
            // Also remove any player connections to this specific world
            PLAYER_UUID_TO_CONNECTED_CHUAWORLD_MAPS.forEach((playerUUID, worlds) ->
                    worlds.removeIf(world -> world.getID().equals(chuaWorld.getID()))
            );
            // Remove players from the map entirely if they no longer have any connected worlds
            PLAYER_UUID_TO_CONNECTED_CHUAWORLD_MAPS.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }
    }


    /**
     * Initializes the player data storage directory. Should be called on plugin enable.
     * @param plugin The main plugin instance.
     */
    public static void setupPlayerDataStorage(JavaPlugin plugin) {
        pluginInstance = plugin;
        playerDataDirectory = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataDirectory.exists()) {
            playerDataDirectory.mkdirs();
            plugin.getLogger().info("Created player data directory: " + playerDataDirectory.getAbsolutePath());
        }
    }

    /**
     * Saves a player's PlayerDataCommon to a JSON file.
     * This should typically be called when a player quits or periodically.
     * @param playerUUID The UUID of the player whose data to save.
     */
    public static void savePlayerCommonData(UUID playerUUID) {
        PlayerTracker tracker = PLAYER_ID_TO_TRACKER_MAP.get(playerUUID);
        if (tracker != null) {
            PlayerDataCommon data = tracker.getPlayerDataCommon();
            File playerFile = new File(playerDataDirectory, playerUUID.toString() + ".json");
            try {
                data.saveToJsonFile(playerFile.getAbsolutePath());
                pluginInstance.getLogger().info("Saved PlayerDataCommon for " + Bukkit.getOfflinePlayer(playerUUID).getName() + " to " + playerFile.getName());
            } catch (IOException | JSONException e) {
                pluginInstance.getLogger().log(Level.SEVERE, "Error saving PlayerDataCommon for " + Bukkit.getOfflinePlayer(playerUUID).getName() + " to " + playerFile.getName(), e);
            }
        }
    }

    /**
     * Saves all currently loaded player data. Should be called on plugin disable.
     */
    public static void saveAllPlayerCommonData() {
        if (playerDataDirectory == null) {
            pluginInstance.getLogger().warning("PlayerData directory not initialized. Cannot save all player data.");
            return;
        }
        pluginInstance.getLogger().info("Saving all active PlayerDataCommon instances...");
        for (Map.Entry<UUID, PlayerTracker> entry : PLAYER_ID_TO_TRACKER_MAP.entrySet()) {
            UUID playerUUID = entry.getKey();
            PlayerDataCommon data = entry.getValue().getPlayerDataCommon();
            File playerFile = new File(playerDataDirectory, playerUUID.toString() + ".json");
            try {
                data.saveToJsonFile(playerFile.getAbsolutePath());
                // Using offline player name for logging as player might be offline when this is called (e.g. on server stop)
                pluginInstance.getLogger().info("Saved PlayerDataCommon for " + Bukkit.getOfflinePlayer(playerUUID).getName() + " to " + playerFile.getName());
            } catch (IOException | JSONException e) {
                pluginInstance.getLogger().log(Level.SEVERE, "Error saving PlayerDataCommon for " + Bukkit.getOfflinePlayer(playerUUID).getName() + " to " + playerFile.getName(), e);
            }
        }
        pluginInstance.getLogger().info("Finished saving all active PlayerDataCommon instances.");
    }
    /**
     * Teleports a player to a new location, handling per-world inventory saving and loading.
     * This is the core method for moving players between any worlds (First Land or lobby).
     * @param player The player to teleport.
     * @param toLocation The destination location.
     * @param plugin The main plugin instance.
     * @param configAccessor The FirstLandWorldConfigAccessor to retrieve friendly names.
     */
    public static void teleportPlayerWithInventoryManagement(Player player, Location toLocation, JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor) {
        PlayerTracker playerTracker = getPlayerTracker(player);
        PlayerDataCommon playerData = playerTracker.getPlayerDataCommon();

        // 1. Save current inventory for the world the player is LEAVING
        World fromWorld = player.getWorld();
        ChuaWorld fromChuaWorld = getChuaWorldById(fromWorld.getUID()); // Check if it's a managed ChuaWorld

        if (fromChuaWorld != null) {
            // Only save inventory if leaving a managed FirstLand world
            playerData.captureBukkitInventoryForWorld(player, fromChuaWorld.getID());
            plugin.getLogger().info("Saved inventory for player " + player.getName() + " in world " + configAccessor.getWorldFriendlyName(fromChuaWorld.getID()));
        }

        // 2. Perform the actual teleport
        player.teleport(toLocation);

        // 3. Load inventory for the world the player is ENTERING
        World toWorld = toLocation.getWorld();
        ChuaWorld toChuaWorld = getChuaWorldById(toWorld.getUID()); // Check if it's a managed ChuaWorld

        if (toChuaWorld != null) {
            // If entering a managed FirstLand world, load its specific inventory
            playerData.applyBukkitInventoryForWorld(player, toChuaWorld.getID());
            plugin.getLogger().info("Loaded inventory for player " + player.getName() + " in world " + configAccessor.getWorldFriendlyName(toChuaWorld.getID()));
        } else {
            // If entering an unmanaged world (like the lobby), clear inventory or apply a default.
            if (fromChuaWorld != null) { // If leaving a managed world to an unmanaged one
                player.getInventory().clear();
                player.updateInventory();
                plugin.getLogger().info("Player " + player.getName() + " entered unmanaged world " + toWorld.getName() + ", cleared inventory.");
            }
        }

        // Always save player common data after inventory changes to ensure persistence
        savePlayerCommonData(player.getUniqueId());
    }



//    public static void removePlayerConnectedChuaWorldSpecific(UUID playerUUID, ChuaWorld chuaWorld) {
//        for(){
//
//        }
//
//    }
}
