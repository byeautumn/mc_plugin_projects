package org.byeautumn.chuachua.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.byeautumn.chuachua.common.LocationVector;

import java.util.List;

public class ChapterConfig {
    private final String worldName;
    private final String name;
    private final int index;
    private final LocationVector exit;
    private final List<LocationVector> spawnLocationVectors;

    public ChapterConfig(int index, String name, String worldName, LocationVector exit, List<LocationVector> spawnLocationVectors) {
        this.index = index;
        this.name = name;
        this.worldName = worldName;
        this.exit = exit;
        this.spawnLocationVectors = spawnLocationVectors;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getName() {
        return name;
    }

    public int getIndex() {
        return index;
    }

    public LocationVector getExit() {
        return exit;
    }

    public List<LocationVector> getSpawnLocationVectors() {
        return spawnLocationVectors;
    }
}
