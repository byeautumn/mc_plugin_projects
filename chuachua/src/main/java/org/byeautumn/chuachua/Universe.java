package org.byeautumn.chuachua;

import com.google.gson.internal.bind.util.ISO8601Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin; // IMPORTANT: Add this import
import org.byeautumn.chuachua.common.LocationVector;
import org.byeautumn.chuachua.game.firstland.FirstLandWorldConfigAccessor;
import org.byeautumn.chuachua.generate.world.WorldManager;
import org.byeautumn.chuachua.generate.world.pipeline.*; // This imports ChunkGenerationStage, RegionGenerator, TerrainGenerator, BiomeGenerator
import org.byeautumn.chuachua.player.PlayerTracker;
import org.byeautumn.chuachua.undo.ActionRecorder;
import org.byeautumn.chuachua.generate.world.WorldGenerator; // Assuming this is your custom WorldGenerator

import java.io.File;
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

    private static Map<UUID, ChuaWorld> UUID_TO_CHUAWORLD = new HashMap<>();

    private static Set<String> bukkitWorldSet;

    private static Map<UUID, ChuaWorld> MAP_OF_PRELOADED_FIRST_LAND_WORLDS = new HashMap<>();

    public static void teleport(Player player, Location toLocation){
        player.teleport(toLocation);
//        player.sendMessage(ChatColor.GREEN + "You were teleported successfully");
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
    public static ChuaWorld getChuaWorldById(UUID id) {
        return UUID_TO_CHUAWORLD.getOrDefault(id, null);
    }

    public static void addChuaWorld(ChuaWorld chuaWorld){
        UUID_TO_CHUAWORLD.put(chuaWorld.getID(), chuaWorld);
    }
    public static void addFirstLandWorld(ChuaWorld chuaWorld){
        MAP_OF_PRELOADED_FIRST_LAND_WORLDS.put(chuaWorld.getID(), chuaWorld);
    }

    public static ChuaWorld createWorld(long createSeed, String worldName) {
        if (isWorldAlreadyLoaded(worldName)) {
            System.out.println("World named '" + worldName + "' exists already. The world creation skipped.");
            return null;
        }

        Map<Integer, ChunkGenerationStage> stages = initializeGenerationPipeline(createSeed);
        Logger pluginLogger = getPluginLogger();

        World newWorld = WorldManager.createWorld(worldName, new WorldGenerator(stages, pluginLogger));

        if (newWorld != null) {
            newWorld.setGameRuleValue("doMobSpawning", "false");
            ChuaWorld chuaWorld = new ChuaWorld(createSeed, newWorld);
            addChuaWorld(chuaWorld);
            return chuaWorld;
        }

        return null;
    }
    private static boolean isWorldAlreadyLoaded(String worldName) {
        // You would need to implement this to check if a world with that name is loaded
        // and handle the `bukkitWorldSet` logic inside.
        return doesWorldExist(worldName);
    }

    private static Map<Integer, ChunkGenerationStage> initializeGenerationPipeline(long createSeed) {
        Map<Integer, ChunkGenerationStage> stages = new TreeMap<>();
        stages.put(0, new ProtoRegionGeneration(createSeed));
        stages.put(1, new ProtoBiomeAssignment());
        stages.put(2, new ProtoTerrainGeneration(createSeed));
        return stages;
    }

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


    public static void loadChuaWorldsToMap(ChuaWorldConfigAccessor accessor){
        List<String> chuaWorlds = accessor.getKnownWorlds();
        System.out.println("getKnowWorlds: " + accessor.getKnownWorlds());
        for (String chuaWorldString : chuaWorlds){
            ChuaWorld chuaWorld = createWorld(accessor.getWorldSeed(chuaWorldString), chuaWorldString);
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

    public static void loadFirstLandWorldsToMap(FirstLandWorldConfigAccessor accessor) {
        List<String> knownWorlds = accessor.getKnownWorlds();
        Logger logger = Bukkit.getLogger();

        logger.info("Loading " + knownWorlds.size() + " worlds from configuration...");
        System.out.println("Known worlds to load: " + knownWorlds);

        for (String worldName : knownWorlds) {
            // Step 1: Check if the world's directory exists
            File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
            if (!worldFolder.exists()) {
                // If the world folder doesn't exist, delete its config section.
                logger.warning("World folder for '" + worldName + "' not found. Deleting config section.");

                ConfigurationSection worldSection = accessor.getWorldConfigSection(worldName);
                if (worldSection != null) {
                    accessor.deleteFirstLandWorldConfigSection(worldSection);
                }
                continue; // Skip loading this world
            }

            // Step 2: If the world folder exists, load the world into the map
            loadSingleChuaWorld(accessor, worldName, logger);
        }

        logger.info("Finished loading worlds. Total worlds in map: " + MAP_OF_PRELOADED_FIRST_LAND_WORLDS.size());
    }

    public static boolean processFirstLandWorldCreation(String worldName, long seed, Player player,
                                                        FirstLandWorldConfigAccessor configAccessor, JavaPlugin plugin) {
        // Check if a world folder with this name already exists before trying to create a new one.
        File worldFolder = new File(plugin.getServer().getWorldContainer(), worldName);
        if (worldFolder.exists()) {
            player.sendMessage(ChatColor.RED + "World folder '" + worldName + "' already exists. Cannot create a new world here.");
            System.err.println("World creation failed for " + worldName + ": Folder already exists.");
            return false;
        }

        ChuaWorld chuaWorld = Universe.createWorld(seed, worldName);

        if (chuaWorld == null) {
            player.sendMessage(ChatColor.RED + "Failed to create world " + worldName);
            System.err.println("World creation failed for " + worldName + ": createWorld returned null.");
            return false;
        }

        World world = chuaWorld.getWorld();

        // This check is still valid for ensuring no duplicate ChuaWorld instances with the same ID,
        // even if names are unique.
        if (Universe.getListOfPreloadedFirstLandById(chuaWorld.getID()) == chuaWorld) {
            System.out.println(ChatColor.RED + "Preloaded worlds have duplicate IDs for: " + worldName + "!");
            return false; // Indicate failure due to ID conflict
        }

        // Add world data to the config.
        configAccessor.addNewWorld(worldName, " ", seed, world.getSpawnLocation());

        Universe.addFirstLandWorld(chuaWorld);
        Bukkit.unloadWorld(world, true);
        player.sendMessage(ChatColor.YELLOW + "'" + worldName + "'" + ChatColor.GREEN + " created successfully");
        return true;
    }

    private static void loadSingleChuaWorld(FirstLandWorldConfigAccessor accessor, String worldName, Logger logger) {
        Long seed = accessor.getWorldSeed(worldName);
        if (seed == null) {
            logger.warning("Skipping world '" + worldName + "'. Seed not found in config.");
            return;
        }

        ChuaWorld chuaWorld = createWorld(seed, worldName);
        if (chuaWorld != null) {
            addFirstLandWorld(chuaWorld);
            logger.info("Successfully loaded world '" + worldName + "'.");
        } else {
            logger.warning("Failed to load world '" + worldName + "'. It may already exist or there was an error.");
        }
    }

    private static void loadBukkitWorldSet() {
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

    public static ChuaWorld getMapOfPreloadedFirstLandWorldsByName(String name){
        for (ChuaWorld world : MAP_OF_PRELOADED_FIRST_LAND_WORLDS.values()) {
            if (world.getWorld().getName().equalsIgnoreCase(name)) {
                return world; // Found a match, return it
            }
        }
        return null;
    }
    public static ChuaWorld getListOfPreloadedFirstLandById(UUID id){
        for (ChuaWorld world : MAP_OF_PRELOADED_FIRST_LAND_WORLDS.values()) {
            if (world.getID().equals(id)) {
                return world; // Found a match, return it
            }
        }
        return null;
    }
    public static int getMapOfPreloadedFirstLandWorldsSize(){
        return MAP_OF_PRELOADED_FIRST_LAND_WORLDS.size();
    }

    /**
     * Deletes a First Land world from the server, including its files and config entry.
     * @param worldName The name of the world to delete.
     * @param player The player who initiated the command.
     * @param configAccessor The accessor to the world configuration.
     * @return true if the world was successfully deleted, false otherwise.
     */
    public static boolean deleteFirstLandWorld(String worldName, Player player, FirstLandWorldConfigAccessor configAccessor) {
        World bukkitWorld = Bukkit.getWorld(worldName);

        // Check if the world is currently loaded and unload it first.
        if (bukkitWorld != null) {
            // Teleport all players out of the world before deleting it.
            if (!bukkitWorld.getPlayers().isEmpty()) {
                World mainWorld = Bukkit.getWorlds().get(0); // Get the main world.
                for (Player p : bukkitWorld.getPlayers()) {
                    p.teleport(mainWorld.getSpawnLocation());
                    p.sendMessage("You were teleported out of " + worldName + " because it is being deleted.");
                }
            }
            // Unload the world from memory.
            if (!Bukkit.unloadWorld(bukkitWorld, false)) {
                player.sendMessage("Failed to unload world '" + worldName + "'. It may be in use.");
                return false;
            }
        }

        // Get the world folder.
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);

        // Delete the folder and its contents.
        if (worldFolder.exists()) {
            boolean deleted = deleteFolder(worldFolder);
            if (deleted) {
                // Now, delete the world's entry from the config.
                configAccessor.deleteFirstLandWorldConfigSection(configAccessor.getWorldConfigSection(worldName));
                return true;
            } else {
                player.sendMessage("Failed to delete world files. Check server permissions.");
                return false;
            }
        } else {
            // If the folder doesn't exist, just remove the config entry.
            // This handles orphaned config entries.
            configAccessor.deleteFirstLandWorldConfigSection(configAccessor.getWorldConfigSection(worldName));
            player.sendMessage("World folder not found, but config entry was deleted.");
            return true;
        }
    }

    /**
     * Recursively deletes a folder and its contents.
     * @param path The File object representing the folder to delete.
     * @return true if the folder was successfully deleted, false otherwise.
     */
    private static boolean deleteFolder(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFolder(file);
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
}

