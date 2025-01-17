package org.byeautumn.chuachua.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.byeautumn.chuachua.Chuachua;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.common.LocationVector;
import org.byeautumn.chuachua.common.RandomizationUtil;

import java.util.List;
import java.util.Random;

public class Chapter {
    private final ChapterConfig config;
    private final ExitOperator exitOperator;

    public Chapter (ChapterConfig config) {
        this.config = config;
        this.exitOperator = new ExitOperator(this);
        System.out.println("The exit operator will start working in 10 seconds");
        Bukkit.getScheduler().scheduleSyncDelayedTask(Chuachua.getInstance, new Runnable() {
            @Override
            public void run() {
                startExitOperation();
            }
        }, 200L);

    }

    public World getWorld() {
        return Bukkit.getWorld(this.config.getWorldName());
    }

    public Location getExitLocation() {
        LocationVector exitVetor = this.config.getExit();
        return new Location(getWorld(), exitVetor.getX(), exitVetor.getY(), exitVetor.getZ());
    }

    public Location getARandomSpawnLocation() {
        World world = getWorld();
        List<LocationVector> vectors = this.config.getSpawnLocationVectors();
        int locIdx = RandomizationUtil.getRandomIndex(vectors.size());
        if (locIdx >= vectors.size()) {
            System.err.println("The random index " + "(" + locIdx + ") of spawn locations is out of range [0," + (vectors.size() - 1) + "].");
            return null;
        }
        LocationVector pickedVector = vectors.get(locIdx);
        return new Location(world, pickedVector.getX(), pickedVector.getY(), pickedVector.getZ());
    }

    public void spawnPlayer(Player player) {
        Location spawnLocation = getARandomSpawnLocation();
        System.out.println("Player " + player.getDisplayName() + " will be spawned to " + spawnLocation);
        Universe.teleport(player, spawnLocation);
    }

    private void startExitOperation() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(Chuachua.getInstance, new Runnable() {
            @Override
            public void run() {
                System.out.println("Exit operator is trying to take off.");
                exitOperator.takeOff();
            }
        }, 0L, 2000L);
    }

}
