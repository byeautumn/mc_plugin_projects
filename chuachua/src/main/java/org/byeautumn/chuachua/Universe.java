package org.byeautumn.chuachua;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
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
import org.byeautumn.chuachua.player.PlayerTracker;
import org.byeautumn.chuachua.undo.ActionRecorder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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

    public static void teleport(Player player, Location toLocation){
        player.teleport(toLocation);
    }

    private static Location getLocation(String worldName, LocationVector vector) {
        return new Location(Bukkit.getWorld(worldName), vector.getX(), vector.getY(), vector.getZ());
    }

    public static void teleportToLobby(Player player) {
        Location toLocation = getLocation(LOBBY_WORLD_NAME, LOBBY_SPAWN_LOCATION_VECTOR);
        player.teleport(toLocation);
        player.sendMessage(ChatColor.GREEN + "You were teleported to lobby successfully");
    }

    public static void resetPlayerTracker(Player player) {
        PlayerTracker playerTracker = getPlayerTracker(player);
        playerTracker.reset();
    }

    public static PlayerTracker getPlayerTracker(Player player){
        if(!PLAYER_ID_TO_TRACKER_MAP.containsKey(player.getUniqueId())) {
            PLAYER_ID_TO_TRACKER_MAP.put(player.getUniqueId(), new PlayerTracker(player));
        }
        return PLAYER_ID_TO_TRACKER_MAP.get(player.getUniqueId());
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
    public static void addChuaWorld(ChuaWorld chuaWorld){
        UUID_TO_CHUAWORLD.put(chuaWorld.getID(), chuaWorld);
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
                // World is loaded in Bukkit but NOT managed by us (e.g., vanilla world or other plugin's world).
                // We should not attempt to "create" a new ChuaWorld on top of it, as it's already a distinct world.
                plugin.getLogger().warning("Attempted to create world '" + worldName + "' but a Bukkit world with that name already exists and is NOT managed by ChuaChua. Aborting creation.");
                return null;
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
                        logger.warning("Seed not found for First Land world " + worldName + ". Cannot create ChuaWorld wrapper during connection load.");
                    }
                }

                // IMPORTANT: Ensure the world is added to UUID_TO_CHUAWORLD if it's new or just wrapped
                // (Already handled by addChuaWorld and getChuaWorldById logic above, good.)
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
        ConfigurationSection worldsSection = configAccessor.getConfig().getConfigurationSection("worlds");
        if (worldsSection == null) {
            plugin.getLogger().info("No First Land worlds found in config to load connections for.");
            return;
        }

        for (String worldUUIDString : worldsSection.getKeys(false)) { // Iterate by World UUID string
            UUID worldUUID;
            try {
                worldUUID = UUID.fromString(worldUUIDString);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid World UUID found in config key: " + worldUUIDString + ". Skipping.");
                continue;
            }

            String connectedPlayerUUIDString = configAccessor.getConnectedPlayerUUID(worldUUID);

            // Check if a player is connected (not "none" or empty) and if the player is online
            if (connectedPlayerUUIDString != null && !connectedPlayerUUIDString.isEmpty() && !"none".equalsIgnoreCase(connectedPlayerUUIDString)) {
                UUID playerUUID;
                try {
                    playerUUID = UUID.fromString(connectedPlayerUUIDString);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid Player UUID found for world " + worldUUIDString + ": " + connectedPlayerUUIDString + ". Skipping.");
                    configAccessor.updateConnectedPlayer(worldUUID, null); // Clear invalid entry
                    continue;
                }

                Player onlinePlayer = Bukkit.getPlayer(playerUUID); // Get online player by UUID
                if (onlinePlayer != null && onlinePlayer.isOnline()) { // Check if player is currently online
                    String worldName = configAccessor.getWorldName(worldUUID); // Get internal name for Bukkit calls
                    if (worldName == null) {
                        plugin.getLogger().warning("Internal name not found for world UUID: " + worldUUID + ". Skipping connection load.");
                        configAccessor.updateConnectedPlayer(worldUUID, null); // Clear potentially invalid connection
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
                                UUID_TO_CHUAWORLD.put(bukkitWorld.getUID(), chuaWorld);
                            } else {
                                plugin.getLogger().warning("Seed not found for world " + worldName + ". Cannot create ChuaWorld wrapper during connection load.");
                            }
                        }

                        if (chuaWorld != null) {
                            PLAYER_UUID_TO_CONNECTED_CHUAWORLD_MAPS.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(chuaWorld);
                            plugin.getLogger().info("Loaded connection: Player " + onlinePlayer.getName() + " -> World " + configAccessor.getWorldFriendlyName(worldUUID) + " (UUID: " + worldUUID + ")");
                        } else {
                            plugin.getLogger().warning("Failed to get or create ChuaWorld for " + worldName + " (UUID: " + worldUUID + ") during load.");
                        }
                    } else {
                        plugin.getLogger().warning("World " + worldName + " (UUID: " + worldUUID + ") associated with " + onlinePlayer.getName() + " could not be loaded during startup.");
                    }
                } else {
                    // Player is not online, clear their connection in config to make the world available
                    plugin.getLogger().info("Player " + connectedPlayerUUIDString + " for world " + worldUUID + " is not online. Resetting world status to 'none'.");
                    configAccessor.updateConnectedPlayer(worldUUID, null); // Use null to set to "none"
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
     * Deletes a First Land world from the server, including its files and config entry.
     * Now operates using World UUID.
     * @param plugin The main plugin instance.
     * @param worldUUID The UUID of the world to delete.
     * @param configAccessor The accessor to the world configuration.
     * @return true if the world was successfully deleted, false otherwise.
     */
    public static boolean deleteFirstLandWorld(JavaPlugin plugin, UUID worldUUID, FirstLandWorldConfigAccessor configAccessor) {
        String worldName = configAccessor.getWorldName(worldUUID);
        if (worldName == null) {
            plugin.getLogger().warning("Attempted to delete world with UUID " + worldUUID + " but its internal name was not found in config.");
            return false;
        }

        World world = Bukkit.getWorld(worldName);

        // Step 1: Handle players currently in the world
        if (world != null) {
            world.setAutoSave(false);
            for (Player player : world.getPlayers()) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                plugin.getLogger().info("Teleported " + player.getName() + " from world '" + worldName + "'.");

                // Clear in-memory connection for this player
                Universe.removePlayerConnectedSpecificChuaWorld(player.getUniqueId(), worldUUID);
            }
            Bukkit.unloadWorld(world, false);
            plugin.getLogger().info("Unloaded world '" + worldName + "'.");
        } else {
            plugin.getLogger().info("World '" + worldName + "' (UUID: " + worldUUID + ") is not loaded. Proceeding with file and config deletion.");
        }

        // Step 2: Clear the connected-player from the config and in-memory map.
        String connectedPlayerUUIDString = configAccessor.getConnectedPlayerUUID(worldUUID);
        if (!"none".equals(connectedPlayerUUIDString) && connectedPlayerUUIDString != null && !connectedPlayerUUIDString.isEmpty()) {
            try {
                UUID playerUUID = UUID.fromString(connectedPlayerUUIDString);
                Universe.removePlayerConnectedChuaWorlds(playerUUID, plugin); // Remove from in-memory map - removed plugin param
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid connected player UUID found for world " + worldUUID + ": " + connectedPlayerUUIDString);
            }
        }
        // Always update config to "none" for this world regardless, as it's being deleted
        configAccessor.updateConnectedPlayer(worldUUID, null);


        // Step 3: Delete the world's folder from the disk.
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        try {
            if (worldFolder.exists()) {
                Files.walk(worldFolder.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(f -> {
                            if (!f.delete()) {
                                plugin.getLogger().warning("Failed to delete file/directory: " + f.getAbsolutePath());
                            }
                        });
                plugin.getLogger().info("Successfully deleted world folder: " + worldFolder.getAbsolutePath());
            } else {
                plugin.getLogger().info("World folder for '" + worldName + "' (UUID: " + worldUUID + ") not found. Skipping file deletion.");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete world folder for '" + worldName + "': " + e.getMessage(), e);
            return false;
        }

        // Step 4: Delete the world's entry from the configuration by its UUID.
        configAccessor.deleteWorldEntry(worldUUID);

        // Step 5: Recalculate the next available world number and save.
        configAccessor.updateAmountToHighestWorldNumber();

        // Step 6: Remove from UUID_TO_CHUAWORLD map
        UUID_TO_CHUAWORLD.remove(worldUUID);

        configAccessor.saveConfig();

        plugin.getLogger().info("Successfully deleted world '" + worldName + "' (UUID: " + worldUUID + ").");
        return true;
    }

    /**
     * Recursively deletes a folder and its contents.
     * @param path The File object representing the folder to delete.
     * @return true if the folder was successfully deleted, false otherwise.
     */
    // This method is redundant with the Files.walk approach above, but keeping if still referenced elsewhere
    private static boolean deleteWorldFolder(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteWorldFolder(file);
                    } else {
                        file.delete();
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
        Set<String> vanillaWorlds = new HashSet<>(Arrays.asList("world", "world_nether", "world_the_end"));
        return vanillaWorlds.contains(worldName);
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

        // Teleport the player
        new BukkitRunnable() {
            @Override
            public void run() {
                player.teleport(targetChuaWorld.getWorld().getSpawnLocation());
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
     * @return true if the player was successfully connected to a world, false otherwise.
     */
    /**
     * Finds the first available unconnected First Land world for a player,
     * or creates a new one if none are available, and then connects the player to it.
     * This method encapsulates the logic previously found in 'connectPlayerToFirstLandWorld'.
     *
     * @param player The player to connect.
     * @param plugin The main plugin instance.
     * @param configAccessor The config accessor for First Land worlds.
     * @return true if the player was successfully connected to a world, false otherwise.
     */
    public static boolean findOrCreateAndConnectFirstLandWorld(Player player, JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor, String friendlyName) {
        plugin.getLogger().info("Attempting to find or create a First Land world for player " + player.getName() + "...");

        // Check if the player already has a connected world
//        ChuaWorld existingPlayerWorld = getPlayerConnectedChuaWorld(player.getUniqueId());
//        if (existingPlayerWorld != null) {
//            player.sendMessage(ChatColor.YELLOW + "You are already connected to a First Land world (" +
//                    configAccessor.getWorldFriendlyName(existingPlayerWorld.getID()) + ChatColor.YELLOW + "). Teleporting you there.");
//            player.teleport(existingPlayerWorld.getWorld().getSpawnLocation());
//            plugin.getLogger().info("Player " + player.getName() + " already connected to " + existingPlayerWorld.getWorld().getName() + ". Teleporting.");
//            return true;
//        }

        UUID worldUUIDToUse = null;
        String internalWorldNameToUse = null;
        Long seedToUse = null;
        ChuaWorld createdOrLoadedChuaWorld = null;

        // 1. Prioritize reusing an existing, unconnected world
        plugin.getLogger().info("Searching for an unconnected First Land world...");
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
//                String friendlyName = configAccessor.getWorldFriendlyName(unconnectedWorldUUID); // Keep existing friendly name
                configAccessor.addNewWorld( // Re-use addNewWorld to update player connection
                        worldUUIDToUse,
                        internalWorldNameToUse,
                        friendlyName, // Keep its existing friendly name
                        player.getUniqueId(),
                        seedToUse,
                        createdOrLoadedChuaWorld.getWorld().getSpawnLocation()
                );
                configAccessor.saveConfig(); // Save changes to config file
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
//            String friendlyName = configAccessor.getWorldFriendlyName(unconnectedWorldUUID); // Keep existing friendly name
            configAccessor.addNewWorld(
                    worldUUIDToUse,
                    internalWorldNameToUse,
                    friendlyName, // Default friendly name to internal name for findOrCreate
                    player.getUniqueId(), // Connect the player who created it
                    seedToUse,
                    createdOrLoadedChuaWorld.getWorld().getSpawnLocation()
            );
            configAccessor.saveConfig(); // Save after adding new world
            configAccessor.updateAmountToHighestWorldNumber(); // Update amount counter and save
        }

        // **COMMON LOGIC FOR BOTH REUSED AND NEWLY CREATED WORLDS**
        if (createdOrLoadedChuaWorld != null) {
            // Connect the player to this world in Universe's in-memory map
            Universe.setPlayerConnectedChuaWorld(player.getUniqueId(), createdOrLoadedChuaWorld, plugin);

            // Teleport the player to the newly created/connected world
            player.teleport(createdOrLoadedChuaWorld.getWorld().getSpawnLocation());
            player.sendMessage(ChatColor.GREEN + "You are now connected to your First Land world: " + configAccessor.getWorldFriendlyName(createdOrLoadedChuaWorld.getID()) + ChatColor.GREEN + "!");
            plugin.getLogger().info("Player " + player.getName() + " successfully connected to world " + configAccessor.getWorldFriendlyName(createdOrLoadedChuaWorld.getID()) + " (UUID: " + createdOrLoadedChuaWorld.getID() + ").");
            return true;
        } else {
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
     * If `chuaWorld` is null, it effectively clears all connections for that player.
     * If `chuaWorld` is not null and not already in the list, it's added.
     *
     */
    public static void setPlayerConnectedChuaWorld(UUID playerUUID, ChuaWorld chuaWorld, JavaPlugin plugin) {
        PLAYER_UUID_TO_CONNECTED_CHUAWORLD_MAPS.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(chuaWorld);
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

                return;
            }
        }
    }

//    public static void removePlayerConnectedChuaWorldSpecific(UUID playerUUID, ChuaWorld chuaWorld) {
//        for(){
//
//        }
//
//    }
}
