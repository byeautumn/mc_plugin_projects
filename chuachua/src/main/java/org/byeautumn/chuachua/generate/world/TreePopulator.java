package org.byeautumn.chuachua.generate.world;

import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Random;

import org.bukkit.block.data.Orientable;

public class TreePopulator extends BlockPopulator {

    private final long seed;
    private final int MIN_HEIGHT = 40;
    private final int MAX_HEIGHT = 200;
    private final Material CUSTOM_BRANCH_MATERIAL = Material.ORANGE_TERRACOTTA; // Custom block for branches

    public TreePopulator(long seed) {
        this.seed = seed;
    }

    @Override
    public void populate(@NonNull WorldInfo worldInfo, @NonNull Random random, int chunkX, int chunkZ, @NonNull LimitedRegion limitedRegion) {
        if (random.nextInt(20) == 0) {
            int worldChunkStartX = chunkX * 16;
            int worldChunkStartZ = chunkZ * 16;
            int worldChunkEndX = worldChunkStartX + 15;
            int worldChunkEndZ = worldChunkStartZ + 15;

            // Iterate through potential tree locations within the current chunk's local coordinates
            int localX = random.nextInt(16);
            int localZ = random.nextInt(16);

            int worldX = worldChunkStartX + localX;
            int worldZ = worldChunkStartZ + localZ;

            System.out.println("[TreePopulator] Attempting population at chunk: " + chunkX + ", " + chunkZ + ", localX: " + localX + ", localZ: " + localZ);

            // Crucially, check if the world coordinate is within the LimitedRegion
            if (limitedRegion.isInRegion(worldX, worldInfo.getMinHeight(), worldZ)) {
                try {
                    int worldY = limitedRegion.getHighestBlockYAt(localX, localZ);
                    System.out.println("[TreePopulator] Highest Y at localX=" + localX + ", localZ=" + localZ + ", worldY=" + worldY);

                    if (worldY > MIN_HEIGHT && worldY < MAX_HEIGHT && limitedRegion.getType(localX, worldY - 1, localZ) == Material.GRASS_BLOCK) {
                        populateCustomTree(worldInfo, random, worldX, worldY, worldZ, limitedRegion);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("[TreePopulator] Error getting highest Y at localX=" + localX + ", localZ=" + localZ + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("[TreePopulator] Skipping tree generation: Randomly chosen location is outside the LimitedRegion.");
            }
        }
    }

    public void populateCustomTree(@NonNull WorldInfo worldInfo, @NonNull Random random, int worldX, int y, int worldZ, @NonNull LimitedRegion limitedRegion) {
        Material trunkMaterial = Material.DARK_OAK_LOG;
        Material rootMaterial = Material.DARK_OAK_WOOD;
        Material leafMaterial = Material.DARK_OAK_LEAVES;
        Material branchLogMaterial = CUSTOM_BRANCH_MATERIAL; // Custom block for branches
        Material branchSlabMaterial = Material.DARK_OAK_SLAB;
        Material branchFenceMaterial = Material.DARK_OAK_FENCE;

        int localStartX = worldX % 16; // Get local X within the chunk
        int localStartZ = worldZ % 16; // Get local Z within the chunk

        int actualGroundY = y;
        while (actualGroundY > worldInfo.getMinHeight() && isAirInternal(limitedRegion, localStartX, actualGroundY - 1, localStartZ)) {
            actualGroundY--;
        }

        int trunkHeight = random.nextInt(3) + 7;
        int rootDepth = random.nextInt(2) + 3;
        int maxBranchLength = 3; // Reduced branch length

        // Generate Roots (Starting from Actual Ground Level)
        for (int i = 1; i <= rootDepth; i++) {
            generateRootSegment(worldInfo, random, limitedRegion, localStartX, actualGroundY - i, localStartZ, rootMaterial, i == 1);
        }

        // Generate Trunk
        for (int i = 0; i < trunkHeight; i++) {
            generateTrunkSegment(worldInfo, random, limitedRegion, localStartX, actualGroundY + i, localStartZ, trunkMaterial);
        }

        // Generate Horizontal Branches
        int numBranches = random.nextInt(3) + 2;
        for (int i = 0; i < numBranches; i++) {
            int branchY = actualGroundY + random.nextInt(trunkHeight - 3) + 3;
            if (branchY >= worldInfo.getMinHeight() && branchY < MAX_HEIGHT - 1) {
                int direction = random.nextInt(4);
                int length = random.nextInt(2) + 2; // Reduced length
                length = Math.min(length, maxBranchLength);
                generateHorizontalBranch(worldInfo, random, limitedRegion, localStartX, branchY, localStartZ, direction, length, branchLogMaterial, branchSlabMaterial, branchFenceMaterial, leafMaterial);
            }
        }

        // Generate More Substantial Leaf Canopy
        int canopyHeight = random.nextInt(2) + 3;
        for (int yy = actualGroundY + trunkHeight - 1; yy < actualGroundY + trunkHeight + canopyHeight; yy++) {
            generateCanopyLayer(worldInfo, random, limitedRegion, localStartX, yy, localStartZ, canopyHeight, yy - (actualGroundY + trunkHeight - 1), leafMaterial);
        }
    }

    private void generateRootSegment(@NonNull WorldInfo worldInfo, @NonNull Random random, @NonNull LimitedRegion limitedRegion, int localX, int y, int localZ, Material rootMaterial, boolean isFirst) {
        if (y >= worldInfo.getMinHeight() && isInRegionInternal(limitedRegion, localX, y, localZ) && isAirInternal(limitedRegion, localX, y, localZ)) {
            limitedRegion.setType(localX, y, localZ, rootMaterial);
            if (isFirst && random.nextFloat() < 0.4) {
                int offsetX = random.nextInt(3) - 1;
                int offsetZ = random.nextInt(3) - 1;
                if (Math.abs(offsetX) + Math.abs(offsetZ) <= 1) {
                    generateRootSegment(worldInfo, random, limitedRegion, localX + offsetX, y, localZ + offsetZ, rootMaterial, false);
                    int downY = y - 1;
                    while (downY >= worldInfo.getMinHeight() && isInRegionInternal(limitedRegion, localX + offsetX, downY, localZ + offsetZ) && isAirInternal(limitedRegion, localX + offsetX, downY, localZ + offsetZ) && random.nextFloat() < 0.6 && (y - downY) < 3) {
                        limitedRegion.setType(localX + offsetX, downY, localZ + offsetZ, rootMaterial);
                        downY--;
                    }
                }
            }
        }
    }

    private void generateTrunkSegment(@NonNull WorldInfo worldInfo, @NonNull Random random, @NonNull LimitedRegion limitedRegion, int localX, int y, int localZ, Material trunkMaterial) {
        if (y < MAX_HEIGHT && y >= worldInfo.getMinHeight() && isInRegionInternal(limitedRegion, localX, y, localZ)) {
            limitedRegion.setType(localX, y, localZ, trunkMaterial);
            if (random.nextFloat() < 0.2) {
                if (isInRegionInternal(limitedRegion, localX + 1, y, localZ)) limitedRegion.setType(localX + 1, y, localZ, trunkMaterial);
                if (isInRegionInternal(limitedRegion, localX - 1, y, localZ)) limitedRegion.setType(localX - 1, y, localZ, trunkMaterial);
                if (isInRegionInternal(limitedRegion, localX, y, localZ + 1)) limitedRegion.setType(localX, y, localZ + 1, trunkMaterial);
                if (isInRegionInternal(limitedRegion, localX, y, localZ - 1)) limitedRegion.setType(localX, y, localZ - 1, trunkMaterial);
            }
        }
    }

    private void generateHorizontalBranch(@NonNull WorldInfo worldInfo, @NonNull Random random, @NonNull LimitedRegion limitedRegion, int startX, int y, int startZ, int direction, int length, Material branchMaterial, Material slabMaterial, Material fenceMaterial, Material leafMaterial) {
        int branchX = startX;
        int branchZ = startZ;
        boolean usedEndBlock = false;

        for (int i = 0; i < length; i++) {
            int nextX = branchX;
            int nextZ = branchZ;
            if (direction == 0) nextZ--;
            else if (direction == 1) nextX++;
            else if (direction == 2) nextZ++;
            else if (direction == 3) nextX--;

            if (isInRegionInternal(limitedRegion, nextX, y, nextZ) && isAirInternal(limitedRegion, nextX, y, nextZ)) {
                limitedRegion.setType(nextX, y, nextZ, branchMaterial);
                Orientable logData = (Orientable) branchMaterial.createBlockData();
                if (direction == 0 || direction == 2) {
                    logData.setAxis(org.bukkit.Axis.Z);
                } else {
                    logData.setAxis(org.bukkit.Axis.X);
                }
                limitedRegion.setBlockData(nextX, y, nextZ, logData);

                if (i == length - 1 && !usedEndBlock) {
                    if (random.nextBoolean() && isInRegionInternal(limitedRegion, nextX, y, nextZ)) {
                        limitedRegion.setType(nextX, y, nextZ, slabMaterial);
                    } else if (isInRegionInternal(limitedRegion, nextX, y, nextZ)) {
                        limitedRegion.setType(nextX, y, nextZ, fenceMaterial);
                    }
                    generateLeafCluster(worldInfo, random, limitedRegion, nextX, y + (random.nextBoolean() ? 0 : 1), nextZ, leafMaterial);
                    usedEndBlock = true;
                }
                branchX = nextX;
                branchZ = nextZ;
            } else {
                break;
            }
        }
        if (random.nextFloat() < 0.3) {
            int leafX = branchX - (direction == 1 ? 1 : (direction == 3 ? -1 : 0));
            int leafZ = branchZ - (direction == 2 ? 1 : (direction == 0 ? -1 : 0));
            if (isInRegionInternal(limitedRegion, leafX, y + (random.nextBoolean() ? 0 : 1), leafZ)) {
                generateLeafCluster(worldInfo, random, limitedRegion, leafX, y + (random.nextBoolean() ? 0 : 1), leafZ, leafMaterial);
            }
        }
    }

    private void generateCanopyLayer(@NonNull WorldInfo worldInfo, @NonNull Random random, @NonNull LimitedRegion limitedRegion, int centerX, int y, int centerZ, int canopyHeight, int layer, Material leafMaterial) {
        int radius = (int) Math.round((canopyHeight - layer) / 0.8);
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if (x >= 0 && x < 16 && z >= 0 && z < 16) {
                    if (Math.pow(x - centerX, 2) + Math.pow(z - centerZ, 2) <= radius * radius + 0.8 && isInRegionInternal(limitedRegion, x, y, z) && isAirInternal(limitedRegion, x, y, z) && y < MAX_HEIGHT && y >= worldInfo.getMinHeight() && random.nextFloat() < 0.75) {
                        generateLeafCluster(worldInfo, random, limitedRegion, x, y + (random.nextBoolean() ? 0 : 1), z, leafMaterial);
                    }
                }
            }
        }
    }

    private void generateLeafCluster(@NonNull WorldInfo worldInfo, @NonNull Random random, @NonNull LimitedRegion limitedRegion, int centerX, int centerY, int centerZ, Material leafMaterial) {
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int y = centerY - 2; y <= centerY + 2; y++) {
                for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                    if (x >= 0 && x < 16 && z >= 0 && z < 16) {
                        if ((Math.abs(x - centerX) <= 1 && Math.abs(z - centerZ) <= 1 && Math.abs(y - centerY) <= 1) || (Math.abs(x - centerX) + Math.abs(z - centerZ) <= 2 && Math.abs(y - centerY) <= 1) || (Math.abs(y - centerY) <= 2 && Math.abs(x - centerX) <= 1 && Math.abs(z - centerZ) <= 1)) {
                            if (isInRegionInternal(limitedRegion, x, y, z) && isAirInternal(limitedRegion, x, y, z) && y < MAX_HEIGHT && y >= worldInfo.getMinHeight() && random.nextFloat() < 0.8) {
                                limitedRegion.setType(x, y, z, leafMaterial);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isInRegionInternal(@NonNull LimitedRegion region, int x, int y, int z) {
        return region.isInRegion(x, y, z);
    }

    private boolean isAirInternal(@NonNull LimitedRegion region, int x, int y, int z) {
        return region.isInRegion(x, y, z) && region.getType(x, y, z).isAir();
    }
}