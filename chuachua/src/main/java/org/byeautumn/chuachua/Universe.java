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
        ItemStack[] inventory = InventoryDataAccessor.getInstance().loadInventory(player.getUniqueId(), getLobby().getName());
        PlayerData playerData = PlayerDataAccessor.getInstance().getPlayerData(player.getUniqueId(), getLobby().getUID(), getLobby().getName());
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

    private static Map<Integer, ChunkGenerationStage> initializeGenerationPipeline(long createSeed) {
        Map<Integer, ChunkGenerationStage> stages = new TreeMap<>();
        stages.put(0, new ProtoRegionGeneration(createSeed));
        stages.put(1, new ProtoBiomeAssignment());
        stages.put(2, new ProtoTerrainGeneration(createSeed));
        return stages;
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

    /**
     * Deletes a First Land world from the server, including its files and config entry.
     * @param plugin The main plugin instance.
     * @param worldUUID The UUID of the world to delete.
     * @return true if the world was successfully deleted, false otherwise.
     */
    public static boolean deleteFirstLandWorld(JavaPlugin plugin, UUID worldUUID) {
        // Step 1: Retrieve world data using the new accessor ONCE
        WorldData worldData = WorldDataAccessor.getInstance().getWorldData(worldUUID);
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
        boolean fileDeletionSuccess = WorldDataAccessor.getInstance().deleteWorld(worldUUID);
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
     * Checks if a given world name is one of the vanilla server worlds.
     * @param worldName The name of the world to check.
     * @return true if the world is a vanilla world, false otherwise.
     */
    public static boolean isVanillaWorld(String worldName) {
        Set<String> vanillaWorlds = new HashSet<>(Arrays.asList("world", "world_nether", "world_the_end"));
        return vanillaWorlds.contains(worldName);
    }


    /**
     * Initiates the world creation process for a player-owned world.
     * This now correctly accepts and passes the owner's UUID.
     * @param player The player who is creating the world.
     * @param plugin The main plugin instance.
     * @param friendlyName The friendly name provided by the player.
     * @param ownerUUID The UUID of the player who will own the world.
     */
    public static void createOrConnectExistingWorldWithPlayer(Player player, JavaPlugin plugin, String friendlyName, UUID ownerUUID) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // First, try to find an unowned world to connect the player to
                UUID unownedWorldUUID = WorldDataAccessor.getInstance().getFirstUnownedWorldUUID();
                System.out.println("unownedWorldUUID: " + unownedWorldUUID);

                if (unownedWorldUUID != null) {
                    // If an unowned world exists, connect the player to it.
                    // The connectUnownedWorldToPlayer method handles moving the file and updating data.
                    WorldDataAccessor.getInstance().connectUnownedWorldToPlayer(
                            unownedWorldUUID,
                            ownerUUID,
                            friendlyName,
                            Collections.singletonList(ownerUUID),
                            plugin
                    );

                    // Teleport the player and update their state after the world is connected
                    Universe.connectPlayerToSpecificWorld(player, plugin, unownedWorldUUID);

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
        WorldGenerationTask generationTask = new WorldGenerationTask(plugin, null, numWorlds, null, null);
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
    public static boolean connectPlayerToSpecificWorld(Player player, JavaPlugin plugin, UUID worldUUID) {
        plugin.getLogger().info("Attempting to connect player " + player.getName() + " to specific world with UUID: " + worldUUID);

        WorldData worldData = WorldDataAccessor.getInstance().getWorldData(worldUUID);


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
                PlayerData playerData = PlayerDataAccessor.getInstance().getPlayerData(player.getUniqueId(), worldData.getWorldUUID(), worldData.getWorldInternalName());

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
                    PlayerDataAccessor.getInstance().savePlayerData(playerData);
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
