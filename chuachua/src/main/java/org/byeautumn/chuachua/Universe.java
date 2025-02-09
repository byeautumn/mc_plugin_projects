package org.byeautumn.chuachua;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.byeautumn.chuachua.common.LocationVector;
import org.byeautumn.chuachua.player.PlayerTracker;
import org.byeautumn.chuachua.undo.ActionRecorder;

import java.util.*;

public class Universe {
    private static final String LOBBY_WORLD_NAME = "world";
    private static final LocationVector LOBBY_SPAWN_LOCATION_VECTOR = new LocationVector(24.0, -60.0, 32.0);

    private static Map<UUID, PlayerTracker> PLAYER_ID_TO_TRACKER_MAP = new HashMap<>();

    private static Map<World, List<Block>> worldToPlayerPlacedBlocks = new HashMap<>();

    private static Map<UUID, ActionRecorder> PLAYER_ID_TO_RECORDER_MAP = new HashMap<>();

    public static void teleport(Player player, Location toLocation){
        player.teleport(toLocation);
        player.sendMessage(ChatColor.GREEN + "You were teleported successfully");
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


}
