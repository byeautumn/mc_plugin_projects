package org.byeautumn.chuachua.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;
import org.apache.commons.io.FileUtils;

public class WorldManager {

    public static World createWorld(String worldName, ChunkGenerator generator) {
        WorldCreator creator = new WorldCreator(worldName);

        if (generator != null) {
            creator.generator(generator); // Set a custom chunk generator (see below)
        }

        World world = Bukkit.createWorld(creator);
        if (world != null) {
            System.out.println("World '" + worldName + "' created successfully!");
            return world;
        } else {
            System.out.println("Failed to create world '" + worldName + "'.");
            return null;
        }
    }

    public static void unloadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, true); // Save chunks before unloading
            System.out.println("World '" + worldName + "' unloaded.");
        }
    }

    public static void deleteWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false); // Don't save chunks if deleting
            try {
                File worldFolder = new File(Bukkit.getServer().getWorldContainer(), worldName);
                FileUtils.deleteDirectory(worldFolder); // Requires Apache Commons IO
                System.out.println("World '" + worldName + "' deleted.");
            } catch (java.io.IOException ex) {
                System.err.println("Error deleting world folder: " + ex.getMessage());
            }
        }
    }

}
