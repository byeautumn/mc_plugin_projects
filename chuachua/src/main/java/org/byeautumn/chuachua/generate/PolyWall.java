package org.byeautumn.chuachua.generate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.byeautumn.chuachua.common.BlockUtil;
import org.byeautumn.chuachua.common.LocationUtil;
import org.byeautumn.chuachua.undo.ActionRecord;
import org.byeautumn.chuachua.undo.BlockProperties;
import org.byeautumn.chuachua.undo.BlockPropertiesRecord;
import org.byeautumn.chuachua.undo.GenerationRecord;

import java.util.ArrayList;
import java.util.List;

public class PolyWall implements Generable{
    private World world = Bukkit.getWorld("testing_world");
    private final List<Block> polySelectedBlocks;
    private final int height;
    private Material material = Material.BRICKS;

    public PolyWall(List<Block> polySelectedBlocks, int height) {
        this.polySelectedBlocks = polySelectedBlocks;
        this.height = height;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    private List<Block> getBlocks() {
        List<Block> allBlocks = new ArrayList<>();
        System.out.println("The number of selected blocks: " + this.polySelectedBlocks.size());
        for (int idx = 0; idx < this.polySelectedBlocks.size(); ++idx) {
            Block block1 = this.polySelectedBlocks.get(idx);
            int nextBlockIdx = idx == this.polySelectedBlocks.size() - 1 ? 0 : idx + 1;
            Block block2 = this.polySelectedBlocks.get(nextBlockIdx);
            List<Block> bottomBlocks = BlockUtil.getBlocksBetweenBlocks(block1, block2);
            allBlocks.addAll(bottomBlocks);
            for (int level = 1; level < height; ++level) {
                Vector addup = new Vector(0.0, (double)level, 0.0);
                for (Block bottom : bottomBlocks) {
                    Vector vector = bottom.getLocation().toVector().clone().add(addup);
                    Location loc = new Location(this.world, vector.getX(), vector.getY(), vector.getZ());
                    allBlocks.add(loc.getBlock());
                }
            }
        }

        return allBlocks;
    }

    @Override
    public ActionRecord generate() {
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
            BlockPropertiesRecord record = new BlockPropertiesRecord(block, new BlockProperties(block.getType()), new BlockProperties(this.material));
            action.addBlockPropertiesRecord(record);
            block.setType(this.material);
        }

        return action;
    }
}
