package org.byeautumn.chuachua.generate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.common.LocationVector;

import java.util.ArrayList;
import java.util.List;

public class SimpleFloor {
    private final List<LocationVector> polygon;
    private Material material = Material.STONE_BRICK_SLAB;

    public SimpleFloor(List<LocationVector> polygon, Material material){
        this.polygon = polygon;
        this.material = material;
    }

    public SimpleFloor(List<LocationVector> polygon) {
        this.polygon = polygon;
    }

    public List<Block> getBlocks() {
        List<Block> blocks = new ArrayList<>();
        if (null == this.polygon || this.polygon.isEmpty())
            return blocks;

        World tempWorld = Universe.getLobby();
        LocationVector startVector = this.polygon.get(0);
        Location startLocation = new Location(tempWorld, startVector.getX(), startVector.getY(), startVector.getZ());
//        startLocation.

        return null;
    }

    public Material getMaterial() {
        return material;
    }
}
