package org.byeautumn.chuachua.generate.world;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rotatable;
import org.bukkit.generator.BlockPopulator;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Random;

public class TreeGenerator extends BlockPopulator {

    @Override
    public void populate(@NonNull World world, @NonNull Random random, @NonNull Chunk chunk) {
        if (random.nextDouble() < 0.1) {
            int x = chunk.getX() * 16 + random.nextInt(16);
            int z = chunk.getZ() * 16 + random.nextInt(16);
            int y = world.getHighestBlockYAt(x, z);

            generateConeTree(world, random, x, y, z);
        }
    }

    private void generateConeTree(World world, Random random, int x, int y, int z) {
        int height = random.nextInt(10) + 15;
        int radius = 2;

        for (int i = 0; i < height; i++) {
            Block block = world.getBlockAt(x, y + i, z);
            if (block.getType().isAir()) {
                block.setType(Material.SPRUCE_LOG);
            }

            for (int r = 1; r <= radius; r++) {
                for (int angle = 0; angle < 360; angle += 45) {
                    double rad = Math.toRadians(angle);
                    int offsetX = (int) (r * Math.cos(rad));
                    int offsetZ = (int) (r * Math.sin(rad));
                    Block coneBlock = world.getBlockAt(x + offsetX, y + i - (i * r / (height / 2)), z + offsetZ);
                    if (coneBlock.getType().isAir() && i > height / 4) {
                        coneBlock.setType(Material.SPRUCE_LEAVES);
                    }
                }
            }
            radius = Math.max(0, radius - 1);
        }

        generateBranches(world, random, x, y, z, height);
        generateRoots(world, random, x, y, z);
        generateFences(world, random, x, y, z);
    }

    private void generateBranches(World world, Random random, int x, int y, int z, int height) {
        for (int i = height / 3; i < height - 5; i += 2) {
            if (random.nextDouble() < 0.8) {
                int branchLength = random.nextInt(4) + 2;
                int angle = random.nextInt(360);
                double rad = Math.toRadians(angle);
                int offsetX = (int) (branchLength * Math.cos(rad));
                int offsetZ = (int) (branchLength * Math.sin(rad));

                for (int j = 0; j < branchLength; j++) {
                    Block branchBlock = world.getBlockAt(x + (offsetX * j / branchLength), y + i, z + (offsetZ * j / branchLength));
                    if (branchBlock.getType().isAir()) {
                        branchBlock.setType(Material.SPRUCE_WOOD);
                        BlockData blockData = branchBlock.getBlockData();
                        if (blockData instanceof Rotatable) {
                            Rotatable rotatable = (Rotatable) blockData;
                            if (Math.abs(offsetX) > Math.abs(offsetZ)) {
                                if (offsetX > 0) {
                                    rotatable.setRotation(BlockFace.EAST);
                                } else {
                                    rotatable.setRotation(BlockFace.WEST);
                                }
                            } else {
                                if (offsetZ > 0) {
                                    rotatable.setRotation(BlockFace.SOUTH);
                                } else {
                                    rotatable.setRotation(BlockFace.NORTH);
                                }
                            }
                            branchBlock.setBlockData(rotatable);
                        }
                        if (j == branchLength - 1) {
                            branchBlock.setType(Material.SPRUCE_WOOD);
                        }
                        if (random.nextDouble() < 0.8) {
                            world.getBlockAt(x + (offsetX * j / branchLength), y + i, z + (offsetZ * j / branchLength)).getRelative(BlockFace.UP).setType(Material.SPRUCE_LEAVES);
                        }
                    }
                }
            }
        }
    }

    private void generateRoots(World world, Random random, int x, int y, int z) {
        int rootLength = random.nextInt(3) + 2;
        for (int i = 0; i < 4; i++) {
            int angle = random.nextInt(360);
            double rad = Math.toRadians(angle);
            int offsetX = (int) (rootLength * Math.cos(rad));
            int offsetZ = (int) (rootLength * Math.sin(rad));

            for (int j = 0; j < rootLength; j++) {
                Block rootBlock = world.getBlockAt(x + (offsetX * j / rootLength), y - j, z + (offsetZ * j / rootLength));
                if (rootBlock.getType().isAir() && y - j > 0) {
                    rootBlock.setType(Material.SPRUCE_LOG);
                }
            }
        }
    }

    private void generateFences(World world, Random random, int x, int y, int z) {
        if (random.nextDouble() < 0.2) {
            int fenceRadius = random.nextInt(3) + 3;
            for (int angle = 0; angle < 360; angle += 90) {
                double rad = Math.toRadians(angle);
                int offsetX = (int) (fenceRadius * Math.cos(rad));
                int offsetZ = (int) (fenceRadius * Math.sin(rad));

                Block fenceBlock = world.getBlockAt(x + offsetX, y, z + offsetZ);
                if (fenceBlock.getType().isAir()) {
                    fenceBlock.setType(Material.DARK_OAK_FENCE);
                }
            }
        }
    }
}