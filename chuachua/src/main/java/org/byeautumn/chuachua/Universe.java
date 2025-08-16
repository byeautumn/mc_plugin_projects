package org.byeautumn.chuachua;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin; // IMPORTANT: Add this import
import org.byeautumn.chuachua.common.LocationVector;
import org.byeautumn.chuachua.generate.world.WorldManager;
import org.byeautumn.chuachua.generate.world.pipeline.*; // This imports ChunkGenerationStage, RegionGenerator, TerrainGenerator, BiomeGenerator
import org.byeautumn.chuachua.player.PlayerTracker;
import org.byeautumn.chuachua.undo.ActionRecorder;
import org.byeautumn.chuachua.generate.world.WorldGenerator; // Assuming this is your custom WorldGenerator

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
    public static ChuaWorld getChuaWorld(UUID id) {
        return UUID_TO_CHUAWORLD.getOrDefault(id, null);
    }
    public static void addChuaWorld(ChuaWorld chuaWorld){
        UUID_TO_CHUAWORLD.put(chuaWorld.getID(), chuaWorld);
    }

    public static ChuaWorld createWorld(long createSeed, String worldName) {
        if (null == bukkitWorldSet) {
            loadBukkitWorldSet();
        }

        if (bukkitWorldSet.contains(worldName)) {
            System.out.println("World named '" + worldName + "' exists already. The world creation skipped.");
            return null;
        }

        // --- START OF CORRECTED INSTANTIATION AND MAPPING FOR MULTI-STAGE PIPELINE ---

        Map<Integer, ChunkGenerationStage> chunkGenerationStages = new TreeMap<>();

        // 1. Instantiate each concrete pipeline stage class.
        //    Declare them using their specific interface types for good practice.
        RegionGenerator protoRegionGeneration = new ProtoRegionGeneration(createSeed);
        TerrainGenerator protoTerrainGeneration = new ProtoTerrainGeneration(createSeed); // Constructor now takes only seed
        BiomeGenerator protoBiomeAssignment = new ProtoBiomeAssignment(); // <-- This is your BiomeGenerator

        // 2. Add each stage to the map in the desired execution order.
        //    The WorldGenerator will iterate through this map.
        chunkGenerationStages.put(0, protoRegionGeneration); // First: Generate the region map
        chunkGenerationStages.put(1, protoBiomeAssignment); // Second: Generate the heightmap (reads region map) // <-- Problem here
        chunkGenerationStages.put(2, protoTerrainGeneration);   // Third: Generate biomes (reads region map & heightmap)

        // --- END OF CORRECTED INSTANTIATION AND MAPPING ---

        // Get the logger from your main plugin class (Chuachua).
        Logger pluginLogger;
        try {
            pluginLogger = JavaPlugin.getPlugin(Chuachua.class).getLogger();
        } catch (IllegalStateException e) {
            System.err.println("Plugin 'Chuachua' not yet loaded or not found. Using System.err for logging.");
            pluginLogger = Logger.getLogger("ChuaWorldCreatorFallback");
            pluginLogger.setLevel(Level.WARNING);
        }

        // Pass the chunkGenerationStages map (containing all stages) and the pluginLogger
        // to your custom WorldGenerator.
        World newWorld = WorldManager.createWorld(worldName, new WorldGenerator(chunkGenerationStages, pluginLogger));

        ChuaWorld chuaWorld = new ChuaWorld(createSeed, newWorld);
        Universe.addChuaWorld(chuaWorld);

        newWorld.setGameRuleValue("doMobSpawning", "false");
        return chuaWorld;
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

    private static void loadBukkitWorldSet() {
        bukkitWorldSet = Bukkit.getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.toCollection(HashSet::new));
    }
}