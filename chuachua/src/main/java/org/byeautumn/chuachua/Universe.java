package org.byeautumn.chuachua;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.byeautumn.chuachua.common.LocationVector;
import org.byeautumn.chuachua.common.PlayMode;
import org.byeautumn.chuachua.game.firstland.*;
import org.byeautumn.chuachua.generate.world.WorldManager;
import org.byeautumn.chuachua.generate.world.pipeline.*;
import org.byeautumn.chuachua.generate.world.WorldGenerator;
import org.byeautumn.chuachua.player.InventoryDataAccessor;
import org.byeautumn.chuachua.player.PlayerData;
import org.byeautumn.chuachua.player.PlayerDataAccessor;
import org.byeautumn.chuachua.player.PlayerTracker;
import org.byeautumn.chuachua.player.matrix.PlayerActivityMatrix;
import org.byeautumn.chuachua.player.matrix.PlayerNutritionMatrix;
import org.byeautumn.chuachua.player.matrix.PlayerSurvivalMatrix;
import org.byeautumn.chuachua.undo.ActionRecorder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
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

    public static void teleport(Player player, Location toLocation){
        player.teleport(toLocation);
    }

    private static Location getLocation(String worldName, LocationVector vector) {
        return new Location(Bukkit.getWorld(worldName), vector.getX(), vector.getY(), vector.getZ());
    }

    public static void teleportToLobby(Player player, PlayerDataAccessor playerDataAccessor, InventoryDataAccessor inventoryDataAccessor ) {
        Location toLocation = getLocation(LOBBY_WORLD_NAME, LOBBY_SPAWN_LOCATION_VECTOR);
        player.teleport(toLocation);
        player.sendMessage(ChatColor.GREEN + "You were teleported to lobby successfully");
        ItemStack[] inventory = inventoryDataAccessor.loadInventory(player.getUniqueId(), getLobby().getName());
        PlayerData playerData = playerDataAccessor.getPlayerData(player.getUniqueId(), getLobby().getUID(), getLobby().getName());
        player.setGameMode(playerData.getGameMode());
        Universe.getPlayerTracker(player).setPlayMode(playerData.getPlayMode());
        Inventory inventoryPlayer = player.getInventory();
        if(inventory != null){
            inventoryPlayer.setContents(inventory);
        }else {
            inventoryPlayer.clear();
        }
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
        plugin.getLogger().info("Attempting to create or load world: " + worldName);

        if (existingBukkitWorld != null) {
            plugin.getLogger().info("A Bukkit world named '" + worldName + "' is already loaded. Checking if we manage it.");
            ChuaWorld existingChuaWorld = getChuaWorldById(existingBukkitWorld.getUID());

            if (existingChuaWorld != null) {
                plugin.getLogger().info("World '" + worldName + "' is already managed by ChuaChua. Returning existing instance.");
                return existingChuaWorld;
            } else {
                if (worldName.startsWith("First_Land_World_")) {
                    plugin.getLogger().info("Found a loaded First Land World '" + worldName + "' not in our map. Re-wrapping it.");

                    File baseDir = new File(plugin.getDataFolder(), "data");
                    WorldDataAccessor configAccessor = WorldDataAccessor.getInstance();

                    // FIX: Use the actual Bukkit world's UUID to get the data
                    UUID bukkitWorldUUID = existingBukkitWorld.getUID();
                    plugin.getLogger().info("Attempting to get world data for Bukkit's UUID: " + bukkitWorldUUID);
                    WorldData worldData = configAccessor.getWorldData(bukkitWorldUUID);

                    if (worldData != null) {
                        Long storedSeed = worldData.getSeed();
                        ChuaWorld rewrappedWorld = new ChuaWorld(storedSeed, existingBukkitWorld);
                        addChuaWorld(rewrappedWorld);
                        plugin.getLogger().info("Successfully re-wrapped world '" + worldName + "' with stored data.");
                        return rewrappedWorld;
                    } else {
                        plugin.getLogger().warning("Could not find config data for existing First Land World '" + worldName + "'. Aborting creation.");
                        return null;
                    }
                } else {
                    plugin.getLogger().warning("Bukkit world '" + worldName + "' exists but is not a First Land World. Aborting creation.");
                    return null;
                }
            }
        }

        // 2. If not loaded, check if the world folder exists on disk.
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        plugin.getLogger().info("Checking if world folder exists on disk: " + worldFolder.getAbsolutePath());
        if (worldFolder.exists()) {
            plugin.getLogger().info("World folder '" + worldName + "' exists. Attempting to load existing world with provided seed: " + seed);

            World bukkitWorldToLoad = new WorldCreator(worldName).seed(seed).createWorld();

            if (bukkitWorldToLoad != null) {
                plugin.getLogger().info("Successfully loaded Bukkit world '" + worldName + "'. Wrapping as ChuaWorld.");
                ChuaWorld chuaWorld = getChuaWorldById(bukkitWorldToLoad.getUID());
                if (chuaWorld == null) {
                    chuaWorld = new ChuaWorld(seed, bukkitWorldToLoad);
                    addChuaWorld(chuaWorld);
                    plugin.getLogger().info("Successfully wrapped and added to map: " + worldName);
                }
                return chuaWorld;
            } else {
                plugin.getLogger().severe("Failed to load existing world '" + worldName + "' from disk. Returning null.");
                return null;
            }
        }

        // 3. If neither loaded nor exists on disk, generate a new world.
        plugin.getLogger().info("World '" + worldName + "' not found on disk. Starting generation of a new world.");

        Map<Integer, ChunkGenerationStage> stages = initializeGenerationPipeline(seed);
        Logger pluginLogger = plugin.getLogger();

        WorldGenerator worldGenerator = new WorldGenerator(stages, pluginLogger);
        World newBukkitWorld = WorldManager.createWorld(worldName, worldGenerator);

        if (newBukkitWorld != null) {
            plugin.getLogger().info("Successfully created new Bukkit world '" + worldName + "'. Setting up properties.");
            newBukkitWorld.setGameRuleValue("doMobSpawning", "false");
            ChuaWorld chuaWorld = new ChuaWorld(seed, newBukkitWorld);
            addChuaWorld(chuaWorld);
            plugin.getLogger().info("Successfully created and added a BRAND NEW ChuaWorld.");
            return chuaWorld;
        }

        plugin.getLogger().severe("Failed to create new Bukkit world for '" + worldName + "'. Returning null.");
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
     * Deletes a First Land world from the server, including its files and config entry.
     * @param plugin The main plugin instance.
     * @param worldUUID The UUID of the world to delete.
     * @param worldDataAccessor The accessor to the world data.
     * @return true if the world was successfully deleted, false otherwise.
     */
    public static boolean deleteFirstLandWorld(JavaPlugin plugin, UUID worldUUID, WorldDataAccessor worldDataAccessor) {
        // Step 1: Retrieve world data using the new accessor ONCE
        WorldData worldData = worldDataAccessor.getWorldData(worldUUID);
        if (worldData == null) {
            plugin.getLogger().warning("Attempted to delete world with UUID " + worldUUID + " but its data was not found.");
            return false;
        }

        String worldName = worldData.getWorldInternalName();
        UUID ownerUUID = worldData.getOwnerUUID();

        // Step 2: Unload world and handle players
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            world.setAutoSave(false);
            for (Player player : world.getPlayers()) {
                player.teleport(Objects.requireNonNull(Bukkit.getWorlds().get(0)).getSpawnLocation());
                player.sendMessage(ChatColor.RED + "The world you were in has been deleted.");
                plugin.getLogger().info("Teleported " + player.getName() + " from world '" + worldName + "'.");
                Universe.removePlayerConnectedSpecificChuaWorld(player.getUniqueId(), worldUUID);
            }
            Bukkit.unloadWorld(world, false);
            plugin.getLogger().info("Unloaded world '" + worldName + "'.");
        } else {
            plugin.getLogger().info("World '" + worldName + "' (UUID: " + worldUUID + ") is not loaded. Proceeding with file and data deletion.");
        }

        // Step 3: Remove in-memory connections and delete the world's files
        if (ownerUUID != null) {
            Universe.removePlayerConnectedSpecificChuaWorld(ownerUUID, worldUUID);
        }

        // Now the new accessor method handles file deletion and data cleanup
        boolean fileDeletionSuccess = worldDataAccessor.deleteWorld(worldUUID);
        if (!fileDeletionSuccess) {
            plugin.getLogger().severe("Failed to delete world files and data for world '" + worldName + "'.");
            return false;
        }

        // Step 4: Remove the ChuaWorld from the main Universe map
        Universe.removeChuaWorld(Universe.getChuaWorldById(worldUUID));

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
     * Initiates the world creation process for a player-owned world.
     * This now correctly accepts and passes the owner's UUID.
     * @param player The player who is creating the world.
     * @param plugin The main plugin instance.
     * @param worldDataAccessor The accessor for world data.
     * @param friendlyName The friendly name provided by the player.
     * @param ownerUUID The UUID of the player who will own the world.
     */
    public static void createOrConnectExistingWorldWithPlayer(Player player, JavaPlugin plugin, WorldDataAccessor worldDataAccessor, PlayerDataAccessor playerDataAccessor, String friendlyName, UUID ownerUUID) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // First, try to find an unowned world to connect the player to
                UUID unownedWorldUUID = worldDataAccessor.getFirstUnownedWorldUUID();
                System.out.println("unownedWorldUUID: " + unownedWorldUUID);

                if (unownedWorldUUID != null) {
                    // If an unowned world exists, connect the player to it.
                    // The connectUnownedWorldToPlayer method handles moving the file and updating data.
                    worldDataAccessor.connectUnownedWorldToPlayer(
                            unownedWorldUUID,
                            ownerUUID,
                            friendlyName,
                            Collections.singletonList(ownerUUID),
                            plugin
                    );

                    // Teleport the player and update their state after the world is connected
                    Universe.connectPlayerToSpecificWorld(player, plugin, worldDataAccessor, unownedWorldUUID, playerDataAccessor);

                } else {
                    // If no unowned world exists, send an error message and notify operators
                    player.sendMessage(ChatColor.RED + ">> " + ChatColor.AQUA + "There are no available worlds to connect to. Please try again later.");
                    Bukkit.broadcastMessage(ChatColor.RED + "[Universe] " + ChatColor.YELLOW + "Alert: Player " + player.getName() + " attempted to connect to an unowned world, but none are available. An admin needs to pre-generate more worlds.");
                }
            }
        }.runTask(plugin);
    }

    public static void createUnownedWorlds(JavaPlugin plugin, WorldDataAccessor worldDataAccessor, int numWorlds) {
        // Pass null for the player and ownerUUID to signify an unowned world
        WorldGenerationTask generationTask = new WorldGenerationTask(plugin, worldDataAccessor, null, numWorlds, null, null);
        generationTask.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Connects a player to a specific First Land world. This function now uses the WorldDataAccessor
     * to handle all data retrieval.
     *
     * @param player The player to connect.
     * @param plugin The main plugin instance.
     * @param worldUUID The UUID of the world to connect to.
     * @return true if the world was successfully found and connected to, false otherwise.
     */
    public static boolean connectPlayerToSpecificWorld(Player player, JavaPlugin plugin, WorldDataAccessor worldDataAccessor, UUID worldUUID, PlayerDataAccessor playerDataAccessor) {
        plugin.getLogger().info("Attempting to connect player " + player.getName() + " to specific world with UUID: " + worldUUID);

        WorldData worldData = worldDataAccessor.getWorldData(worldUUID);


        if (worldData == null) {
            player.sendMessage(ChatColor.RED + "World not found or data is incomplete.");
            plugin.getLogger().warning("Failed to find world data for UUID: " + worldUUID);
            return false;
        }

        String targetInternalWorldName = worldData.getWorldInternalName();
        long targetSeed = worldData.getSeed();
        ChuaWorld targetChuaWorld = Universe.createWorld(targetSeed, targetInternalWorldName, plugin);

        if (targetChuaWorld == null) {
            player.sendMessage(ChatColor.RED + "Failed to prepare world for connection. It might be corrupted.");
            plugin.getLogger().severe("Failed to load/create world '" + targetInternalWorldName + "' for direct connection. ChuaWorld is null.");
            return false;
        }

        FirstLandGameManager manager = FirstLandGameManager.getInstance();

        FirstLandGame firstLandGame = manager.getOrCreateGame(worldData);

        Universe.setPlayerConnectedChuaWorld(player.getUniqueId(), targetChuaWorld, plugin);

        new BukkitRunnable() {
            @Override
            public void run() {
                // Load the player's data for this world
                PlayerData playerData = playerDataAccessor.getPlayerData(player.getUniqueId(), worldData.getWorldUUID(), worldData.getWorldInternalName());

                Location teleportLocation;
                if (playerData != null && playerData.getLastKnownLogoffWorldUUID() != null) {
                    // If saved data exists, teleport the player to their last known log-off location
                    teleportLocation = new Location(
                            Bukkit.getWorld(playerData.getLastKnownLogoffWorldUUID()),
                            playerData.getLastKnownLogoffX(),
                            playerData.getLastKnownLogoffY(),
                            playerData.getLastKnownLogoffZ(),
                            playerData.getLastKnownLogoffYaw(),
                            playerData.getLastKnownLogoffPitch()
                    );

                    // Apply loaded data
                    player.setHealth(playerData.getHealth());
                    player.setFoodLevel(playerData.getHunger());
                    player.setGameMode(playerData.getGameMode());
                    Universe.getPlayerTracker(player).setPlayMode(playerData.getPlayMode());
                    player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "Your saved data has been loaded for this world.");
                } else {
                    // If no saved data, teleport to the world's spawn location
                    teleportLocation = targetChuaWorld.getWorld().getSpawnLocation();
                    player.sendMessage(ChatColor.RED + ">> " + ChatColor.AQUA + "Warning: No saved data was found for this world. Teleporting to world spawn.");
                    playerData = PlayerData.builder()
                            .playerUUID(player.getUniqueId())
                            .worldUUID(worldUUID)
                            .worldInternalName(targetInternalWorldName)
                            .playMode(PlayMode.UNKNOWN) // Default play mode
                            .gameMode(GameMode.SURVIVAL) // Default game mode
                            .health(20.0)
                            .hunger(20)
                            .playerSurvivalMatrix(PlayerSurvivalMatrix.builder()
                                    .bodyTemp(50.0)
                                    .hydration(100.0)
                                    .playerNutrition(PlayerNutritionMatrix.builder()
                                            .fat(50.0)
                                            .carbohydrates(100.0)
                                            .protein(100.0)
                                            .build())
                                    .build())
                            .playerActivityMatrix(PlayerActivityMatrix.builder()
                                    .fightingAbility(100.0)
                                    .jumpingAbility(100.0)
                                    .miningAbility(100.0)
                                    .swimmingAbility(100.0)
                                    .walkingAbility(100.0)
                                    .build())
                            .lastMatrixUpdateTime(targetChuaWorld.getWorld().getGameTime())
                            .build();
                    playerDataAccessor.savePlayerData(playerData);
                    plugin.getLogger().info("Saving player Data on first connect ");
                }

                // Perform the teleport and final messages
                player.teleport(teleportLocation);
                player.sendMessage(ChatColor.GREEN + "Successfully connected to world: " + worldData.getWorldFriendlyName() + ChatColor.GREEN + "!");
                plugin.getLogger().info("Player " + player.getName() + " directly connected to world " + worldData.getWorldFriendlyName() + " (UUID: " + targetChuaWorld.getID() + ").");
            }
        }.runTask(plugin);

        return true;
    }

    /**
     * Finds the first available unconnected First Land world for a player,
     * or creates a new one if none are available, and then connects the player to it.
     * This method now uses the {@link WorldDataAccessor} for all data operations.
     *
     * @param player The player to connect.
     * @param plugin The main plugin instance.
     * @param worldDataAccessor The accessor for world data.
     * @param friendlyName The desired friendly name for a new world, if created.
     * @return true if the player was successfully connected to a world, false otherwise.
     */
    public static boolean findOrCreateAndConnectFirstLandWorld(Player player, JavaPlugin plugin, WorldDataAccessor worldDataAccessor, String friendlyName) {
        plugin.getLogger().info("Attempting to find or create a First Land world for player " + player.getName() + "...");

        UUID worldUUIDToUse = null;
        String internalWorldNameToUse = null;
        Long seedToUse = null;
        WorldData worldDataToUse = null;
        ChuaWorld createdOrLoadedChuaWorld = null;

        // 1. Prioritize reusing an existing, unconnected world
        plugin.getLogger().info("Searching for an unconnected First Land world...");
        UUID unconnectedWorldUUID = worldDataAccessor.findFirstUnconnectedWorldUUID();

        if (unconnectedWorldUUID != null) {
            // Reuse existing unconnected world
            WorldData worldData = worldDataAccessor.getWorldData(unconnectedWorldUUID);
            if (worldData != null) {
                worldUUIDToUse = worldData.getWorldUUID();
                internalWorldNameToUse = worldData.getWorldInternalName();
                seedToUse = worldData.getSeed();

                // Correctly create an updated WorldData object using the builder pattern
                worldDataToUse = WorldData.builder()
                        .worldUUID(worldUUIDToUse)
                        .worldFriendlyName(friendlyName)
                        .ownerUUID(player.getUniqueId())
                        .worldInternalName(internalWorldNameToUse)
                        .seed(seedToUse)
                        .build();

                // Directly call Universe.createWorld which handles loading if world folder exists
                createdOrLoadedChuaWorld = Universe.createWorld(seedToUse, internalWorldNameToUse, plugin);

                if (createdOrLoadedChuaWorld == null) {
                    player.sendMessage(ChatColor.RED + "Failed to prepare existing world for use. Please try again later.");
                    plugin.getLogger().log(Level.SEVERE, "Failed to load existing Bukkit world: " + internalWorldNameToUse + " (UUID: " + unconnectedWorldUUID + ") for reuse. Check world files/server logs. Deleting config entry.");
                    worldDataAccessor.deleteWorld(unconnectedWorldUUID); // Clean up corrupted entry
                } else {
                    player.sendMessage(ChatColor.GREEN + "Connecting you to an existing unused world...");
                    plugin.getLogger().info("Reusing existing unconnected world (UUID: " + unconnectedWorldUUID + ", Internal: " + internalWorldNameToUse + ") for player " + player.getName());
                    worldDataAccessor.saveWorldData(worldDataToUse); // Save the updated WorldData object
                }
            } else {
                plugin.getLogger().warning("Found unconnected UUID " + unconnectedWorldUUID + " but no corresponding data file. Skipping and creating new world.");
            }
        }

        // 2. Create a brand new world if no unconnected ones were found OR if reuse failed
        if (createdOrLoadedChuaWorld == null) {
            plugin.getLogger().info("No unconnected worlds found or reuse failed. Creating a brand new world for " + player.getName() + ".");
            Random random = new Random();
            seedToUse = random.nextLong();
            // Use the UUID as the internal world name, removing the need for getHighestWorldNumber()
            internalWorldNameToUse = "First_Land_World_" + sendFormattedTimeMessage();

            player.sendMessage(ChatColor.GREEN + "Creating a brand new world for you...");
            plugin.getLogger().info("Creating new world '" + internalWorldNameToUse + "' (UUID: " + worldUUIDToUse + ", Seed: " + seedToUse + ") for player " + player.getName());

            // Create the actual Bukkit world and its ChuaWorld wrapper.
            createdOrLoadedChuaWorld = Universe.createWorld(seedToUse, internalWorldNameToUse, plugin);

            worldUUIDToUse = createdOrLoadedChuaWorld.getID(); // Generate a new UUID for a new world

            if (createdOrLoadedChuaWorld == null) {
                player.sendMessage(ChatColor.RED + "Failed to create your new world. Please try again later.");
                plugin.getLogger().log(Level.SEVERE, "Failed to create Bukkit world for internal name: " + internalWorldNameToUse + " during player creation.");
                return false; // Abort if world creation fails
            }

            // Correctly create a new WorldData object using the builder pattern
            worldDataToUse = WorldData.builder()
                    .worldUUID(worldUUIDToUse)
                    .worldInternalName(internalWorldNameToUse)
                    .worldFriendlyName(friendlyName)
                    .ownerUUID(player.getUniqueId())
                    .seed(seedToUse)
                    .build();
            worldDataAccessor.saveWorldData(worldDataToUse);
        }

        // **COMMON LOGIC FOR BOTH REUSED AND NEWLY CREATED WORLDS**
        if (createdOrLoadedChuaWorld != null) {
            // Connect the player to this world in Universe's in-memory map
            Universe.setPlayerConnectedChuaWorld(player.getUniqueId(), createdOrLoadedChuaWorld, plugin);

            // Teleport the player to the newly created/connected world
            player.teleport(createdOrLoadedChuaWorld.getWorld().getSpawnLocation());
            player.sendMessage(ChatColor.GREEN + "You are now connected to your First Land world: " + friendlyName + ChatColor.GREEN + "!");
            plugin.getLogger().info("Player " + player.getName() + " successfully connected to world " + friendlyName + " (UUID: " + createdOrLoadedChuaWorld.getID() + ").");
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "An unexpected error occurred during world connection. Please report this issue.");
            plugin.getLogger().severe("Unexpected error: createdOrLoadedChuaWorld was null after creation/reuse attempt for player " + player.getName());
            return false;
        }
    }

    public static String getCurrentDateTimeString(String formatPattern) {
        // Create a Date object representing the current date and time
        Date now = new Date();

        // Create a SimpleDateFormat object with the desired format pattern
        // Example patterns:
        // "dd/MM/yyyy HH:mm:ss" -> 23/09/2025 20:30:15
        // "yyyy-MM-dd HH:mm:ss" -> 2025-09-23 20:30:15
        // "MMMM dd, yyyy h:mm a" -> September 23, 2025 8:30 PM
        SimpleDateFormat formatter = new SimpleDateFormat(formatPattern);

        // Format the Date object into a String
        String formattedDateTime = formatter.format(now);

        return formattedDateTime;
    }

    // Example usage in a Bukkit plugin method
    public static String sendFormattedTimeMessage() {
        // Assuming 'player' is a Player object
        // player.sendMessage("Current time: " + currentTime);
        return getCurrentDateTimeString("dd/MM/yyyy HH:mm:ss");
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
     * @param chuaWorld The ChuaWorld instance to remove.
     */
    public static void removeChuaWorld(ChuaWorld chuaWorld) {
        if (chuaWorld != null) {
            UUID_TO_CHUAWORLD.remove(chuaWorld.getID());
        }
    }

//    public static void removePlayerConnectedChuaWorldSpecific(UUID playerUUID, ChuaWorld chuaWorld) {
//        for(){
//
//        }
//
//    }
}
