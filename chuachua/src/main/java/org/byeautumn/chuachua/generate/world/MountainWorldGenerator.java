package org.byeautumn.chuachua.generate.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.bukkit.block.data.Orientable;

import java.awt.Color;
import java.util.Random;
import java.util.logging.Logger;

import org.byeautumn.chuachua.noise.Perlin;

public class MountainWorldGenerator extends ChunkGenerator {

    private final long seed;
    private final static int SNOW_LINE_ELEVATION = 170;
    private final static int MIN_HEIGHT = 40;
    private final static int MAX_HEIGHT = 200;
    private final float mountainIntensity = 0.3f; // Drastically reduced intensity
    private final float mountainThreshold = 0.6f;
    private final float maskScale = 0.004f;
    private final int maskOctaves = 6;
    private final float maskPersistence = 0.5f;
    private final float baseScale = 0.002f;
    private final float epsilon = 0.1f;
    private final int blendStart = MAX_HEIGHT - 50; // Blend much earlier
    private final float blendFactorStrength = 0.8f; // Blend more strongly
    private final float highFrequencyScale = 0.015f;
    private final float highFrequencyIntensity = 8.0f;
    private final int highFrequencyOctaves = 4;
    private final float highFrequencyPersistence = 0.6f;
    private final Perlin topDetailNoise;
    private final float topDetailScale = 0.01f;
    private final float topDetailThreshold = 0.8f;

    private final Perlin perlin;
    private final Perlin mountainMaskNoise;
    private final Perlin highFrequencyNoise;
    private final BiomeGenerator biomeGenerator;
    private final Logger logger;

    public MountainWorldGenerator(long seed) {
        this.seed = seed;
        this.perlin = new Perlin(this.seed);
        this.mountainMaskNoise = new Perlin(this.seed + 1);
        this.highFrequencyNoise = new Perlin(this.seed + 2);
        this.topDetailNoise = new Perlin(this.seed + 5);
        this.biomeGenerator = new BiomeGenerator(this.seed);
        this.logger = Logger.getLogger("MountainWorldGenerator");
    }

    @Override
    public ChunkData generateChunkData(@NonNull World world, @NonNull Random random, int x, int z, @NonNull BiomeGrid biome) {
        ChunkData chunkData = createChunkData(world);

        for (int xx = 0; xx < 16; xx++) {
            for (int zz = 0; zz < 16; zz++) {
                int worldX = x * 16 + xx;
                int worldZ = z * 16 + zz;

                // Calculate base height
                float baseHeightValue = (
                        perlin.getHeight((worldX + epsilon) * baseScale, worldZ * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f) * 1.25f +
                                perlin.getHeight(worldX * baseScale, (worldZ + epsilon) * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f) +
                                perlin.getHeight((worldX - epsilon) * baseScale, worldZ * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f) +
                                perlin.getHeight(worldX * baseScale, (worldZ - epsilon) * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f) +
                                perlin.getHeight(worldX * baseScale, worldZ * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f)
                ) / 5.25f;
                int height = (int) baseHeightValue; // Renamed to 'height' as it's the ground level

                // Sample the mountain mask noise
                float rawMaskValue = mountainMaskNoise.layeredPerlin((float) worldX * maskScale, (float) worldZ * maskScale, maskOctaves, maskPersistence);
                float maskValue = (rawMaskValue + 1.0f) / 2.0f;

                // Calculate initial amplification
                float amplification = 1.0f;
                float falloffRange = 0.2f;
                if (maskValue > mountainThreshold) {
                    float normalizedMountainValue = (float) ((maskValue - mountainThreshold) / (1.0f - mountainThreshold));
                    amplification = 1.0f + normalizedMountainValue * mountainIntensity;
                } else if (maskValue > mountainThreshold - falloffRange) {
                    float blend = (maskValue - (mountainThreshold - falloffRange)) / falloffRange;
                    float intensityAtThreshold = ((mountainThreshold > 0 ? (mountainThreshold - mountainThreshold) / (1.0f - mountainThreshold) : 0)) * mountainIntensity;
                    amplification = 1.0f + blend * intensityAtThreshold;
                }

                float finalAmplification = amplification;

                // Blend amplification as height approaches MAX_HEIGHT
                if (height > blendStart) {
                    float blendFactor = (float) (height - blendStart) / (MAX_HEIGHT - blendStart);
                    blendFactor = Math.min(1.0f, Math.max(0.0f, blendFactor));
                    float smoothBlend = blendFactor * blendFactor * (3 - 2 * blendFactor);
                    finalAmplification = amplification * (1.0f - smoothBlend * blendFactorStrength);
                }

                int finalHeight = (int) (height * finalAmplification);

                // Apply more significant top detail
                if (finalHeight >= MAX_HEIGHT - 10) { // Start detail a bit lower
                    float detailValue = topDetailNoise.layeredPerlin(worldX * topDetailScale, worldZ * topDetailScale, 3, 0.7f);
                    float detailInfluence = detailValue * 8.0f; // Increase multiplier
                    finalHeight = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, (int) (MAX_HEIGHT + detailInfluence)));
                } else {
                    finalHeight = Math.max(MIN_HEIGHT, finalHeight);
                }

                Color biomeColor = biomeGenerator.getBiomeFromFactors(
                        (int) (biomeGenerator.getTemperature(worldX, finalHeight) * 255),
                        (int) (biomeGenerator.getAltitude(worldX, worldZ) * 255),
                        (int) (biomeGenerator.getHydration(worldX, worldZ) * 255),
                        worldX,
                        worldZ
                );
                Material blockMaterial = colorToMinecraftMaterial(biomeColor);
                Biome currentBiome = (finalHeight >= SNOW_LINE_ELEVATION) ? Biome.SNOWY_PLAINS : Biome.PLAINS;
                biome.setBiome(xx, zz, currentBiome);

                for (int yy = world.getMinHeight(); yy < finalHeight; yy++) {
                    chunkData.setBlock(xx, yy, zz, blockMaterial);
                    if (yy < finalHeight - 1) {
                        chunkData.setBlock(xx, yy, zz, Material.STONE);
                    }
                }
                if (blockMaterial == Material.GRASS_BLOCK && finalHeight > MIN_HEIGHT + 1 && random.nextInt(80) == 0) {
                    generateConeTree(chunkData, xx, finalHeight, zz, random);
                }
            }
        }
        return chunkData;
    }

    private void generateConeTree(ChunkData chunkData, int x, int y, int z, Random random) {
        int treeHeight = random.nextInt(6) + 8; // Height between 8 and 13
        Material trunkMaterial = Material.SPRUCE_LOG;
        Material leafMaterial = Material.SPRUCE_LEAVES;
        Material branchLogMaterial = Material.SPRUCE_LOG;
        Material branchFenceMaterial = Material.SPRUCE_FENCE;

        // Generate trunk with fence transitions
        for (int i = 0; i < treeHeight - 2; i++) {
            if (y + i < MAX_HEIGHT - 2) {
                chunkData.setBlock(x, y + i, z, trunkMaterial);
            }
        }
        // Transition to fences
        if (y + treeHeight - 2 < MAX_HEIGHT - 2) chunkData.setBlock(x, y + treeHeight - 2, z, Material.SPRUCE_FENCE);
        if (y + treeHeight - 1 < MAX_HEIGHT - 2) chunkData.setBlock(x, y + treeHeight - 1, z, Material.SPRUCE_FENCE);

        // Generate cone-shaped leaves and branches
        for (int yy = y + 2; yy < y + treeHeight + 2; yy++) { // Increased leaf height
            int radius = Math.max(0, (int) Math.round((treeHeight + 2 - yy + y) / 1.8) - 1);
            float normalizedHeight = (float) (yy - y - 2) / (treeHeight - 3); // 0 at bottom leaves, 1 at top

            for (int xx = x - radius; xx <= x + radius; xx++) {
                for (int zz = z - radius; zz <= z + radius; zz++) {
                    double distanceSq = Math.pow(xx - x, 2) + Math.pow(zz - z, 2);
                    if (distanceSq <= radius * radius + 0.5) {
                        if (chunkData.getType(xx, yy, zz).isAir()) {
                            chunkData.setBlock(xx, yy, zz, leafMaterial);
                        }
                        // Generate branches (sideways logs) sparsely, more at the bottom
                        float branchChanceFactor = 1.0f - normalizedHeight * 0.7f; // Higher chance at lower levels
                        if (random.nextFloat() < 0.05f * branchChanceFactor) { // Reduced base branch chance
                            int offsetX = xx - x;
                            int offsetZ = zz - z;

                            if (Math.abs(offsetX) > 0) {
                                int branchX = x + (int) Math.signum(offsetX);
                                if (chunkData.getType(branchX, yy, z).isAir() && Math.abs(branchX - x) <= radius + 2) {
                                    chunkData.setBlock(branchX, yy, z, branchLogMaterial);
                                    Orientable logData = (Orientable) branchLogMaterial.createBlockData();
                                    logData.setAxis(org.bukkit.Axis.X);
                                    chunkData.setBlock(branchX, yy, z, logData);
                                    // Add fence extension, shorter at higher levels
                                    float fenceChance = 0.6f * (1.0f - normalizedHeight * 0.8f);
                                    int fenceLength = (int) (3 * (1.0f - normalizedHeight * 0.7f)); // Longer at bottom
                                    for (int i = 1; i <= fenceLength; i++) {
                                        int fenceX = branchX + i * (int) Math.signum(offsetX);
                                        if (random.nextFloat() < fenceChance && chunkData.getType(fenceX, yy, z).isAir() && Math.abs(fenceX - x) <= radius + 3) {
                                            chunkData.setBlock(fenceX, yy, z, branchFenceMaterial);
                                        } else {
                                            break; // Stop extending if chance fails or hits a block
                                        }
                                    }
                                }
                            } else if (Math.abs(offsetZ) > 0) {
                                int branchZ = z + (int) Math.signum(offsetZ);
                                if (chunkData.getType(x, yy, branchZ).isAir() && Math.abs(branchZ - z) <= radius + 2) {
                                    chunkData.setBlock(x, yy, branchZ, branchLogMaterial);
                                    Orientable logData = (Orientable) branchLogMaterial.createBlockData();
                                    logData.setAxis(org.bukkit.Axis.Z);
                                    chunkData.setBlock(x, yy, branchZ, logData);
                                    // Add fence extension, shorter at higher levels
                                    float fenceChance = 0.6f * (1.0f - normalizedHeight * 0.8f);
                                    int fenceLength = (int) (3 * (1.0f - normalizedHeight * 0.7f)); // Longer at bottom
                                    for (int i = 1; i <= fenceLength; i++) {
                                        int fenceZ = branchZ + i * (int) Math.signum(offsetZ);
                                        if (random.nextFloat() < fenceChance && chunkData.getType(x, yy, fenceZ).isAir() && Math.abs(fenceZ - z) <= radius + 3) {
                                            chunkData.setBlock(x, yy, fenceZ, branchFenceMaterial);
                                        } else {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Generate a denser top
        for (int xx = x - 1; xx <= x + 1; xx++) {
            for (int zz = z - 1; zz <= z + 1; zz++) {
                if (chunkData.getType(xx, y + treeHeight + 1, zz).isAir()) {
                    chunkData.setBlock(xx, y + treeHeight + 1, zz, leafMaterial);
                }
            }
        }
        if (chunkData.getType(x, y + treeHeight + 2, z).isAir()) {
            chunkData.setBlock(x, y + treeHeight + 2, z, leafMaterial);
        }
    }

    private Material colorToMinecraftMaterial(Color color) {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();

        if (red > 200 && green > 200 && blue > 200) {
            return Material.SNOW_BLOCK;
        } else if (red > 150 && green > 150) {
            return Material.OAK_LOG;
        } else if (green > 150) {
            return Material.GRASS_BLOCK;
        } else if (red < 100 && green < 100 && blue < 100) {
            return Material.STONE;
        } else if (blue > 150) {
            return Material.WATER; // Example for water
        } else {
            return Material.GRASS_BLOCK;
        }
    }
}