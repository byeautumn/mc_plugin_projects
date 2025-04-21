package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.byeautumn.chuachua.noise.Perlin;

import java.awt.*;
import java.util.Random;
import java.util.logging.Logger;

public class ProtoTerrainGeneration implements TerrainGenerator {


    private final float mountainIntensity = 0.25f; // Adjusted
    private final float mountainThreshold = 0.6f;
    private final float maskScale = 0.004f;
    private final int maskOctaves = 6;
    private final float maskPersistence = 0.5f;
    private final float baseScale = 0.0015f;
    private final static int MIN_HEIGHT = 40;
    private final static int MAX_HEIGHT = 200;
    private final float epsilon = 0.1f;
    private final long seed;
    private final Perlin perlin;
    private final Perlin mountainMaskNoise;
    private final Perlin topDetailNoise;
    private final float topDetailScale = 0.01f;
    private final int blendStart = MAX_HEIGHT - 50;
    private final float blendFactorStrength = 0.8f;
    private final float highFrequencyScale = 0.015f;
    private final float highFrequencyIntensity = 8.0f;
    private final Logger logger;
    private final BiomeGeneratorOriginal biomeGenerator;
    private final static int SNOW_LINE_ELEVATION = 170;



    public ProtoTerrainGeneration(long seed) {
        this.seed = seed;
        this.topDetailNoise = new Perlin(this.seed + 2);
        this.perlin = new Perlin(this.seed);
        this.mountainMaskNoise = new Perlin(this.seed + 1);
        this.logger = Logger.getLogger("MountainWorldGenerator");
        this.biomeGenerator = new BiomeGeneratorOriginal(this.seed);
    }

    @Override
    public void generate(World world, Random random, int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData, ChunkGenerator.BiomeGrid biomeGrid) {

        float[][] heightmap = new float[18][18];

        try {
            for (int xx = -1; xx < 17; xx++) {
                for (int zz = -1; zz < 17; zz++) {
                    int worldX = chunkX * 16 + xx;
                    int worldZ = chunkZ * 16 + zz;

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
        } catch (Exception e) {
            logger.severe("[ERROR] Exception in generateChunkData for chunk x=" + chunkX + ", z=" + chunkZ + ": " + e.getMessage());
            e.printStackTrace();
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
                        (int) (biomeGenerator.getTemperature(chunkX * 16 + xx, finalHeight) * 255),
                        (int) (biomeGenerator.getAltitude(chunkX * 16 + xx, chunkZ * 16 + zz) * 255),
                        (int) (biomeGenerator.getHydration(chunkX * 16 + xx, chunkX * 16 + zz) * 255),
                        chunkX * 16 + xx,
                        chunkZ * 16 + zz
                );
                Material blockMaterial = colorToMinecraftMaterial(biomeColor);
                Biome currentBiome = (finalHeight >= SNOW_LINE_ELEVATION) ? Biome.SNOWY_PLAINS : Biome.PLAINS;
                biomeGrid.setBiome(xx, zz, currentBiome);

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



