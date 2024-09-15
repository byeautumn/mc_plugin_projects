package org.aerial_dad.noodlelegs;

import org.aerial_dad.noodlelegs.game.PlayerTracker;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Universe {
    private static final String LOBBY_WORLD_NAME = "world";

    private static Map<UUID, PlayerTracker> PLAYER_ID_TO_TRACKER_MAP = new HashMap<>();

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

    public static World getLobby() {
        return Bukkit.getWorld(LOBBY_WORLD_NAME);
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



}
