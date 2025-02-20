package org.aerial_dad.noodlelegs;

import org.aerial_dad.noodlelegs.game.Game;
import org.aerial_dad.noodlelegs.game.GameLauncher;
import org.aerial_dad.noodlelegs.game.GameTracker;
import org.aerial_dad.noodlelegs.game.PlayerTracker;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Universe {
    private static final String LOBBY_WORLD_NAME = "world";

    private static Map<UUID, PlayerTracker> PLAYER_ID_TO_TRACKER_MAP = new HashMap<>();

    private static Map<String, GameTracker> GAME_NAME_TO_TRACKER_MAP = new HashMap<>();

    private static Map<World, List<Block>> worldToPlayerPlacedBlocks = new HashMap<>();

    public static void teleport(Player player, Location toLocation){

        player.teleport(toLocation);
        player.sendMessage(ChatColor.GREEN + "You were teleported successfully");

    }

    public static World copyWorld(World originalWorld, String newWorldName) {
        copyFileStructure(originalWorld.getWorldFolder(), new File(Bukkit.getWorldContainer(), newWorldName));
        new WorldCreator(newWorldName).createWorld();
        return Bukkit.getWorld(newWorldName);
    }

    public static boolean doesWorldExist(String worldName) {
        File worldDir = new File(Bukkit.getWorldContainer(), worldName);
        return worldDir.exists();
    }

    public static PlayerTracker getPlayerTracker(Player player){
        if(!PLAYER_ID_TO_TRACKER_MAP.containsKey(player.getUniqueId())) {
            PLAYER_ID_TO_TRACKER_MAP.put(player.getUniqueId(), new PlayerTracker(player));
        }
        return PLAYER_ID_TO_TRACKER_MAP.get(player.getUniqueId());
    }

    public static void trackGame(GameLauncher gameLauncher, Game game) {
        GAME_NAME_TO_TRACKER_MAP.put(game.getName(), new GameTracker(gameLauncher, game));
    }

    public static World getLobby() {
        return Bukkit.getWorld(LOBBY_WORLD_NAME);
    }
    public static void softResetWorld(World world) {
        System.out.println("Softly resetting world " + world.getName() + " since game is over.");
        List<Block> blocksToBeRemoved = worldToPlayerPlacedBlocks.get(world);
        System.out.println("Removing player placed blocks: " + (null == blocksToBeRemoved ? 0 : blocksToBeRemoved.size()));
        if (null != blocksToBeRemoved) {
            for (Block block : blocksToBeRemoved) {
                block.setType(Material.AIR);
            }
        }
        worldToPlayerPlacedBlocks.remove(world);
        System.out.println("Softly resetting finished.");
    }

    public static void markPlayerPlacedBlock(World world, Block block) {
        if (!worldToPlayerPlacedBlocks.containsKey(world)) {
            worldToPlayerPlacedBlocks.put(world, new ArrayList<>());
        }
        worldToPlayerPlacedBlocks.get(world).add(block);
    }

    private static void copyFileStructure(File source, File target){
        try {
            ArrayList<String> ignore = new ArrayList<>(Arrays.asList("uid.dat", "session.lock"));
            if(!ignore.contains(source.getName())) {
                if(source.isDirectory()) {
                    if(!target.exists())
                        if (!target.mkdirs())
                            throw new IOException("Couldn't create world directory!");
                    String files[] = source.list();
                    for (String file : files) {
                        File srcFile = new File(source, file);
                        File destFile = new File(target, file);
                        copyFileStructure(srcFile, destFile);
                    }
                } else {
                    InputStream in = Files.newInputStream(source.toPath());
                    OutputStream out = Files.newOutputStream(target.toPath());
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0)
                        out.write(buffer, 0, length);
                    in.close();
                    out.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean deleteWorld(World world) {
        if (null == world) { // || !doesWorldExist(world.getName())) {
            System.err.println("The given world is either null or not existing.");
            return false;
        }
        try {
            Bukkit.unloadWorld(world.getName(), false);
            deleteFolder(world.getWorldFolder());
        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.err.println("Failed to delete world '" + world.getName() + "'.");
            return false;
        }
        System.out.println("World '" + world.getName() + "' is deleted.");
        return true;
    }

    private static void deleteFolder(File folder) throws IOException {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else
                {
                    file.delete();
                }
            }
        }
        folder.delete();

    }
//    private static void deleteUnloadedWorlds() {
//        for (World world : Bukkit.getWorlds()) {
//            // Unload the world (save changes)
//            Bukkit.unloadWorld(world, true);
//
//
//            // Get the world folder
//            File worldFolder = world.getWorldFolder();
//
//            // Delete the world folder
//            try {
//                deleteFolder(worldFolder);
//            } catch (IOException e) {
//                getLogger().log(Level.SEVERE, "Error deleting world folder: " + e.getMessage());
//            }
//        }
//    }

    public static Collection<GameTracker> getAllGameTrackers() {
        return GAME_NAME_TO_TRACKER_MAP.values();
    }

    public static GameTracker getGameTracker(String gameName) {
        return GAME_NAME_TO_TRACKER_MAP.get(gameName);
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
}
