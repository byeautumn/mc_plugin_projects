package org.byeautumn.chuachua.generate.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.byeautumn.chuachua.generate.world.pipeline.BiomeGeneratorOriginal;
import org.byeautumn.chuachua.generate.world.pipeline.tree.TreePopulator;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.ArrayList;

import org.byeautumn.chuachua.noise.Perlin;

public class MountainWorldGenerator extends ChunkGenerator {

    private final long seed;
    private final static int SNOW_LINE_ELEVATION = 170;
    private final static int MIN_HEIGHT = 40;
    private final static int MAX_HEIGHT = 200;
    private final float mountainIntensity = 0.25f; // Adjusted
    private final float mountainThreshold = 0.6f;
    private final float maskScale = 0.004f;
    private final int maskOctaves = 6;
    private final float maskPersistence = 0.5f;
    private final float baseScale = 0.0015f; // Adjusted
    private final float epsilon = 0.1f;
    private final int blendStart = MAX_HEIGHT - 50;
    private final float blendFactorStrength = 0.8f;
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
    private final BiomeGeneratorOriginal biomeGenerator;
    private final Logger logger;
    private final TreePopulator treePopulator;

    public MountainWorldGenerator(long seed) {
        this.seed = seed;
        this.perlin = new Perlin(this.seed);
        this.mountainMaskNoise = new Perlin(this.seed + 1);
        this.highFrequencyNoise = new Perlin(this.seed + 2);
        this.topDetailNoise = new Perlin(this.seed + 5);
        this.biomeGenerator = new BiomeGeneratorOriginal(this.seed);
        this.logger = Logger.getLogger("MountainWorldGenerator");
        this.treePopulator = new TreePopulator(this.seed + 3);
    }

    @Override
    public @NonNull List<BlockPopulator> getDefaultPopulators(@NonNull World world) {
        List<BlockPopulator> populators = new ArrayList<>();
        populators.add(treePopulator);
        return populators;
    }

    @Override
    public ChunkData generateChunkData(@NonNull World world, @NonNull Random random, int x, int z, @NonNull BiomeGrid biome) {
        ChunkData chunkData = createChunkData(world);

        try {
            // Extended Heightmap (18x18 for 1-block overlap)
            float[][] heightmap = new float[18][18];

            // Generate Extended Heightmap
            for (int xx = -1; xx < 17; xx++) {
                for (int zz = -1; zz < 17; zz++) {
                    int worldX = x * 16 + xx;
                    int worldZ = z * 16 + zz;

                    float baseHeightValue = (
                            perlin.getHeight((worldX + epsilon) * baseScale, worldZ * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f) * 1.25f +
                                    perlin.getHeight(worldX * baseScale, (worldZ + epsilon) * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f) +
                                    perlin.getHeight((worldX - epsilon) * baseScale, worldZ * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f) +
                                    perlin.getHeight(worldX * baseScale, (worldZ - epsilon) * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f) +
                                    perlin.getHeight(worldX * baseScale, worldZ * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f)
                    ) / 5.25f;
                    int height = (int) baseHeightValue;

                    float rawMaskValue = mountainMaskNoise.layeredPerlin((float) worldX * maskScale, (float) worldZ * maskScale, maskOctaves, maskPersistence);
                    float maskValue = (rawMaskValue + 1.0f) / 2.0f;

                    float amplification = 1.0f;
                    float falloffRange = 0.2f;
                    if (maskValue > mountainThreshold) {
                        float normalizedMountainValue = (float) ((maskValue - mountainThreshold) / (1.0f - mountainThreshold));
                        amplification = 1.0f + normalizedMountainValue * mountainIntensity;
                    } else if (maskValue > mountainThreshold - falloffRange) {
                        float blend = (maskValue - (mountainThreshold - falloffRange)) / falloffRange;
                        float intensityAtThreshold = ((mountainThreshold > 0 ? (0.0f) / (1.0f - mountainThreshold) : 0)) * mountainIntensity;
                        amplification = 1.0f + blend * intensityAtThreshold;
                    }

                    float finalAmplification = amplification;

                    if (height > blendStart) {
                        float blendFactor = (float) (height - blendStart) / (MAX_HEIGHT - blendStart);
                        blendFactor = Math.min(1.0f, Math.max(0.0f, blendFactor));
                        float smoothBlend = blendFactor * blendFactor * (3 - 2 * blendFactor);
                        finalAmplification = amplification * (1.0f - smoothBlend * blendFactorStrength);
                    }

                    int finalHeight = (int) (height * finalAmplification);

                    if (finalHeight > MAX_HEIGHT) {
                        float blend = (float) (finalHeight - MAX_HEIGHT) / (finalHeight - height);
                        finalHeight = (int) (MAX_HEIGHT + (finalHeight - MAX_HEIGHT) * (1 - blend * blend));
                    }

                    float elevationFactor = (float) finalHeight / MAX_HEIGHT;
                    float detailScaleAdjusted = highFrequencyScale * (0.6f + 0.4f * elevationFactor);
                    float detailIntensityAdjusted = highFrequencyIntensity * elevationFactor * 0.6f;

                    if (finalHeight >= MAX_HEIGHT - 10) {
                        float detailValue = topDetailNoise.layeredPerlin(worldX * topDetailScale, worldZ * topDetailScale, 3, 0.7f);
                        float detailInfluence = detailValue * detailIntensityAdjusted;
                        finalHeight = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, (int) (finalHeight + detailInfluence)));
                    } else {
                        finalHeight = Math.max(MIN_HEIGHT, finalHeight);
                    }

                    heightmap[xx + 1][zz + 1] = finalHeight; // Store heightmap
                }
            }

            // Border Averaging
            for (int xx = 0; xx < 16; xx++) {
                for (int zz = 0; zz < 16; zz++) {
                    heightmap[xx + 1][zz + 1] = (heightmap[xx + 1][zz + 1] + heightmap[xx][zz + 1] + heightmap[xx + 2][zz + 1] + heightmap[xx + 1][zz] + heightmap[xx + 1][zz + 2]) / 5.0f;
                }
            }

            // Apply Gaussian Blur (Smoothing)
            applyGaussianBlur(heightmap, 1);

            // Set Chunk Data
            for (int xx = 0; xx < 16; xx++) {
                for (int zz = 0; zz < 16; zz++) {
                    int finalHeight = (int) heightmap[xx + 1][zz + 1];

                    Color biomeColor = biomeGenerator.getBiomeFromFactors(
                            (int) (biomeGenerator.getTemperature(x * 16 + xx, finalHeight) * 255),
                            (int) (biomeGenerator.getAltitude(x * 16 + xx, z * 16 + zz) * 255),
                            (int) (biomeGenerator.getHydration(x * 16 + xx, z * 16 + zz) * 255),
                            x * 16 + xx,
                            z * 16 + zz
                    );
                    Material blockMaterial = colorToMinecraftMaterial(biomeColor);
                    Biome currentBiome = (finalHeight >= SNOW_LINE_ELEVATION) ? Biome.SNOWY_PLAINS : Biome.PLAINS;
                    biome.setBiome(xx, zz, currentBiome);

                    for (int yy = world.getMinHeight(); yy < finalHeight; yy++) {
                        if (yy < MIN_HEIGHT + 5) {
                            chunkData.setBlock(xx, yy, zz, Material.STONE);
                        } else {
                            chunkData.setBlock(xx, yy, zz, blockMaterial);
                        }
                        if (yy < finalHeight - 1) {
                            chunkData.setBlock(xx, yy, zz, Material.STONE);
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.severe("[ERROR] Exception in generateChunkData for chunk x=" + x + ", z=" + z + ": " + e.getMessage());
            e.printStackTrace();
        }

        return chunkData;
    }

    private void applyGaussianBlur(float[][] heightmap, int radius) {
        float[][] temp = new float[18][18];
        for (int xx = 0; xx < 18; xx++) {
            for (int zz = 0; zz < 18; zz++) {
                float sum = 0;
                float weightSum = 0;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        int nx = xx + dx;
                        int nz = zz + dz;
                        if (nx >= 0 && nx < 18 && nz >= 0 && nz < 18) {
                            float weight = (float) Math.exp(-(dx * dx + dz * dz) / 2.0);
                            sum += heightmap[nx][nz] * weight;
                            weightSum += weight;
                        }
                    }
                }
                temp[xx][zz] = sum / weightSum;
            }
        }
        for (int xx = 0; xx < 18; xx++) {
            for (int zz = 0; zz < 18; zz++) {
                heightmap[xx][zz] = temp[xx][zz];
            }
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
            return Material.WATER;
        } else {
            return Material.GRASS_BLOCK;
        }
    }
}