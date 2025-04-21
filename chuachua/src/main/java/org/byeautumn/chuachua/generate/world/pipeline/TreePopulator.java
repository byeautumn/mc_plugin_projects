package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.Axis;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Random;
import java.util.logging.Logger;

public class TreePopulator extends BlockPopulator {

    private final long seed;
    private final int MIN_HEIGHT = 40;
    private final int MAX_HEIGHT = 200;
    private final Material CUSTOM_BRANCH_MATERIAL = Material.DARK_OAK_LOG;
    private final Logger logger = Logger.getLogger("TreePopulator");

    public TreePopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(@NonNull World world, @NonNull Random random, @NonNull Chunk chunk) {
        logger.info("[TreePopulator] populate() called for chunk: " + chunk.getX() + ", " + chunk.getZ());

        if (random.nextInt(3) == 0) { // Reduced tree generation chance
            int numTrees = 1; // Generate only 1 tree per chunk
            logger.info("[TreePopulator] Generating " + numTrees + " tree in this chunk.");

            int worldX = chunk.getX() * 16 + random.nextInt(16);
            int worldZ = chunk.getZ() * 16 + random.nextInt(16);
            int worldY = world.getHighestBlockYAt(worldX, worldZ);

            Material blockBelow = chunk.getBlock(worldX & 15, worldY - 1, worldZ & 15).getType();

            if (worldY > MIN_HEIGHT && worldY < MAX_HEIGHT && (blockBelow == Material.GRASS_BLOCK || blockBelow == Material.STONE)) {
                if (random.nextInt(10) == 0) { // Even lower chance for pine tree
                    logger.info("[TreePopulator] Generating pine tree at: " + worldX + ", " + worldY + ", " + worldZ);
                    generatePineTree(world, random, worldX, worldY, worldZ);
                } else {
                    logger.info("[TreePopulator] Generating custom tree at: " + worldX + ", " + worldY + ", " + worldZ);
                    populateCustomTree(world, random, worldX, worldY, worldZ, chunk);
                }
            }
        } else {
            logger.info("[TreePopulator] Tree generation not triggered by random chance.");
        }
    }

    public void populateCustomTree(@NonNull World world, @NonNull Random random, int worldX, int y, int worldZ, @NonNull Chunk chunk) {
        Material trunkMaterial = Material.DARK_OAK_LOG;
        Material leafMaterial = Material.DARK_OAK_LEAVES;
        Material branchLogMaterial = CUSTOM_BRANCH_MATERIAL;

        int actualGroundY = y;
        while (actualGroundY > world.getMinHeight() && world.getBlockAt(worldX, actualGroundY - 1, worldZ).getType().isAir()) {
            actualGroundY--;
        }

        int trunkHeight = random.nextInt(5) + 10;
        int maxBranchLength = 5;

        for (int i = 0; i < trunkHeight; i++) {
            generateTrunkSegment(world, random, worldX, actualGroundY + i, worldZ, trunkMaterial);
        }

        int numBranches = random.nextInt(4) + 3;
        for (int i = 0; i < numBranches; i++) {
            int branchY = actualGroundY + random.nextInt(trunkHeight - 3) + 3;
            if (branchY >= world.getMinHeight() && branchY < MAX_HEIGHT - 1) {
                int direction = random.nextInt(4);
                int length = random.nextInt(3) + 3;
                length = Math.min(length, maxBranchLength);
                generateHorizontalBranch(world, random, worldX, branchY, worldZ, direction, length, branchLogMaterial, leafMaterial);
            }
        }

        int canopyHeight = random.nextInt(3) + 4;
        for (int yy = actualGroundY + trunkHeight - 1; yy < actualGroundY + trunkHeight + canopyHeight; yy++) {
            generateCanopyLayer(world, random, worldX, yy, worldZ, canopyHeight, yy - (actualGroundY + trunkHeight - 1), leafMaterial);
        }
    }

    private void generatePineTree(@NonNull World world, @NonNull Random random, int worldX, int y, int worldZ) {
        Material trunkMaterial = Material.SPRUCE_LOG;
        Material leafMaterial = Material.SPRUCE_LEAVES;

        int height = random.nextInt(8) + 12; // Even taller

        for (int yy = y; yy < y + height; yy++) {
            world.getBlockAt(worldX, yy, worldZ).setType(trunkMaterial);
        }

        // Start the cone a few blocks above the ground
        int coneStart = y + height / 3;

        for (int yy = coneStart; yy < y + height + 3; yy++) {
            // Adjusted radius calculation for very narrow cone
            double heightRatio = (double) (yy - coneStart) / (height + 3 - coneStart);
            int radius = (int) Math.max(1, 1 - (int) (heightRatio * 1)); // Very narrow base and linear taper

            for (int xx = worldX - radius; xx <= worldX + radius; xx++) {
                for (int zz = worldZ - radius; zz <= worldZ + radius; zz++) {
                    // Modified condition for vertical emphasis
                    if (Math.pow(xx - worldX, 2) + Math.pow(zz - worldZ, 2) <= radius * radius && world.getBlockAt(xx, yy, zz).getType().isAir()) {
                        world.getBlockAt(xx, yy, zz).setType(leafMaterial);
                    }
                }
            }
        }
    }

    private void generateTrunkSegment(@NonNull World world, @NonNull Random random, int worldX, int y, int worldZ, Material trunkMaterial) {
        if (y < MAX_HEIGHT && y >= world.getMinHeight()) {
            world.getBlockAt(worldX, y, worldZ).setType(trunkMaterial);
            if (random.nextFloat() < 0.2) {
                world.getBlockAt(worldX + 1, y, worldZ).setType(trunkMaterial);
                world.getBlockAt(worldX - 1, y, worldZ).setType(trunkMaterial);
                world.getBlockAt(worldX, y, worldZ + 1).setType(trunkMaterial);
                world.getBlockAt(worldX, y, worldZ - 1).setType(trunkMaterial);
            }
        }
    }

    private void generateHorizontalBranch(@NonNull World world, @NonNull Random random, int startX, int y, int startZ, int direction, int length, Material branchMaterial, Material leafMaterial) {
        int branchX = startX;
        int branchZ = startZ;

        Vector dirVector = new Vector(0, 0, 0);
        Axis axis = Axis.X;

        if (direction == 0) {
            dirVector = new Vector(0, 0, -1);
            axis = Axis.Z;
        } else if (direction == 1) {
            dirVector = new Vector(1, 0, 0);
            axis = Axis.X;
        } else if (direction == 2) {
            dirVector = new Vector(0, 0, 1);
            axis = Axis.Z;
        } else if (direction == 3) {
            dirVector = new Vector(-1, 0, 0);
            axis = Axis.X;
        }

        for (int i = 0; i < length; i++) {
            int nextX = branchX + dirVector.getBlockX();
            int nextZ = branchZ + dirVector.getBlockZ();

            if (world.getBlockAt(nextX, y, nextZ).getType().isAir()) {
                Block block = world.getBlockAt(nextX, y, nextZ);
                block.setType(branchMaterial);
                Orientable orientable = (Orientable) block.getBlockData();
                orientable.setAxis(axis);
                block.setBlockData(orientable);

                branchX = nextX;
                branchZ = nextZ;
            } else {
                break;
            }
        }

        int leafX = branchX + dirVector.getBlockX();
        int leafZ = branchZ + dirVector.getBlockZ();
        generateLeafCluster(world, random, leafX, y + (random.nextBoolean() ? 0 : 1), leafZ, leafMaterial);
    }

    private void generateCanopyLayer(@NonNull World world, @NonNull Random random, int centerX, int y, int centerZ, int canopyHeight, int layer, Material leafMaterial) {
        int radius = (int) Math.round((canopyHeight - layer) / 0.8);
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if (Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2) <= radius * radius && world.getBlockAt(x, y, z).getType().isAir() && y < MAX_HEIGHT && y >= world.getMinHeight() && random.nextFloat() < 0.75) {
                    generateLeafCluster(world, random, x, y + (random.nextBoolean() ? 0 : 1), z, leafMaterial);
                }
            }
        }
    }

    private void generateLeafCluster(@NonNull World world, @NonNull Random random, int centerX, int centerY, int centerZ, Material leafMaterial) {
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int y = centerY - 2; y <= centerY + 2; y++) {
                for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                    if ((Math.abs(x - centerX) <= 1 && Math.abs(z - centerZ) <= 1 && Math.abs(y - centerY) <= 1) || (Math.abs(x - centerX) + Math.abs(z - centerZ) <= 2 && Math.abs(y - centerY) <= 1) || (Math.abs(y - centerY) <= 2 && Math.abs(x - centerX) <= 1 && Math.abs(z - centerZ) <= 1)) {
                        if (world.getBlockAt(x, y, z).getType().isAir() && y < MAX_HEIGHT && y >= world.getMinHeight() && random.nextFloat() < 0.8) {
                            world.getBlockAt(x, y, z).setType(leafMaterial);
                        }
                    }
                }
            }
        }
    }
}