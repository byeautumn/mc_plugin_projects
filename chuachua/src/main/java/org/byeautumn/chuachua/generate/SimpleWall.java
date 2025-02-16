package org.byeautumn.chuachua.generate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.byeautumn.chuachua.common.LocationUtil;
import org.byeautumn.chuachua.common.LocationVector;
import org.byeautumn.chuachua.io.IOUntil;
import org.byeautumn.chuachua.undo.ActionRecord;
import org.byeautumn.chuachua.undo.BlockProperties;
import org.byeautumn.chuachua.undo.BlockPropertiesRecord;
import org.byeautumn.chuachua.undo.GenerationRecord;

import java.util.ArrayList;
import java.util.List;

public class SimpleWall implements Generable{
    private World world = Bukkit.getWorld("testing_world");
    private final LocationVector pos1;
    private final LocationVector pos2;
    private final int height;
    private Material material = Material.BRICKS;

    public SimpleWall(LocationVector pos1, LocationVector pos2, int height){
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.height = height;
    }

    public List<Block> getBlocks() {
        Location loc1 = LocationUtil.getLocationFromVector(this.world, pos1);
        System.out.println(LocationUtil.printLocation(loc1));
        Location loc2 = LocationUtil.getLocationFromVector(this.world, pos2);
        System.out.println(LocationUtil.printLocation(loc2));

        List<Block> bottomBlocks = LocationUtil.getBlocksBetweenLocations(loc1, loc2);
        List<Block> ret = new ArrayList<>(bottomBlocks);
        for (int level = 1; level < height; ++level) {
            Vector addup = new Vector(0.0, (double)level, 0.0);
            for (Block bottom : bottomBlocks) {
                Vector vector = bottom.getLocation().toVector().clone().add(addup);
                Location loc = new Location(this.world, vector.getX(), vector.getY(), vector.getZ());
                ret.add(loc.getBlock());
            }
        }
        return ret;
    }

    @Override
    public ActionRecord generate(Player player) {
        List<Block> blocks = getBlocks();
        System.out.println("Block number in the list: " + blocks.size());
        GenerationRecord action = new GenerationRecord();
        for(Block block : blocks) {
            if (null == block) {
                System.out.println("Block is null ... ignore");
                continue;
            }
            System.out.println(LocationUtil.printBlock(block));
            System.out.println("Setting material ..." + block.getType());
            BlockPropertiesRecord record = new BlockPropertiesRecord(block, new BlockProperties(block.getBlockData().getAsString()),
                    new BlockProperties(IOUntil.convertMaterialToBlockDataString(this.material)));
            action.addBlockPropertiesRecord(record);
            IOUntil.updateBlockData(player, block, block.getBlockData().getAsString());
        }

        return action;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }
}
