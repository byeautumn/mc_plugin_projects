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
                    generateRealisticTree(chunkData, xx, finalHeight, zz, random);
                }
            }
        }
        return chunkData;
    }

    private void generateRealisticTree(ChunkData chunkData, int x, int y, int z, Random random) {
        // Determine tree type (for material variation - can be expanded)
        Material logMaterial;
        Material leafMaterial;
        if (random.nextBoolean()) {
            logMaterial = Material.OAK_LOG;
            leafMaterial = Material.OAK_LEAVES;
        } else {
            logMaterial = Material.SPRUCE_LOG;
            leafMaterial = Material.SPRUCE_LEAVES;
        }

        int trunkHeight = random.nextInt(8) + 8; // Height variation
        double baseThickness = 1.5 + random.nextDouble(); // Trunk thickness at base
        double taperRate = 0.6 + random.nextDouble() * 0.3; // How much the trunk tapers

        // Generate Trunk with slight variation
        for (int i = 0; i < trunkHeight; i++) {
            double currentThickness = baseThickness * Math.pow(taperRate, (double) i / trunkHeight);
            int radius = (int) Math.round(currentThickness / 2.0);
            for (int ox = -radius; ox <= radius; ox++) {
                for (int oz = -radius; oz <= radius; oz++) {
                    if (ox * ox + oz * oz <= radius * radius + 0.2) {
                        int blockY = y + i;
                        if (blockY < MAX_HEIGHT - 1) {
                            chunkData.setBlock(x + ox, blockY, z + oz, logMaterial);
                        }
                    }
                }
            }
            // Slight random trunk bends
            if (random.nextInt(5) == 0 && i > 2) {
                x += random.nextInt(3) - 1;
                z += random.nextInt(3) - 1;
            }
        }

        // Generate Branches (more at the bottom, shorter and thinner at the top)
        int numBranches = random.nextInt(8) + 6;
        for (int i = 0; i < numBranches; i++) {
            double branchStartHeightRatio = 0.2 + (double) i / numBranches * 0.6; // Branches mostly in lower/mid section
            int branchY = y + (int) (trunkHeight * branchStartHeightRatio) + random.nextInt(3) - 1;
            if (branchY < y + 2) continue; // Don't place branches too low

            double branchLength = (1.0 - branchStartHeightRatio) * 6 + random.nextDouble() * 3;
            double branchThickness = 0.8 - branchStartHeightRatio * 0.6 + random.nextDouble() * 0.2;
            double angleXZ = random.nextDouble() * 2 * Math.PI;
            double angleY = Math.PI / 2 + (random.nextDouble() * 0.8 - 0.4); // Tend to grow outwards/slightly upwards

            int branchStartX = x + (int) (Math.cos(angleXZ) * (baseThickness / 2));
            int branchStartZ = z + (int) (Math.sin(angleXZ) * (baseThickness / 2));

            int lastBX = branchStartX;
            int lastBY = branchY;
            int lastBZ = branchStartZ;

            for (double j = 0; j < branchLength; j += 0.8) {
                int bx = branchStartX + (int) (Math.cos(angleXZ) * Math.cos(angleY) * j);
                int bz = branchStartZ + (int) (Math.sin(angleXZ) * Math.cos(angleY) * j);
                int by = branchY + (int) (Math.sin(angleY) * j);

                // Generate branch segments
                drawLine(chunkData, lastBX, lastBY, lastBZ, bx, by, bz, logMaterial);
                lastBX = bx;
                lastBY = by;
                lastBZ = bz;

                if (by < MAX_HEIGHT - 1) {
                    double currentBranchThickness = branchThickness * (1.0 - j / branchLength);
                    if (currentBranchThickness < 0.5 && random.nextInt(3) == 0) {
                        chunkData.setBlock(bx, by, bz, Material.SPRUCE_FENCE); // Simulate thinner twigs
                    }
                }
            }

            // Add leaves at the end of branches
            int leafDensity = 3 + random.nextInt(4);
            for (int ly = lastBY - leafDensity / 2; ly < lastBY + leafDensity / 2; ly++) {
                for (int lx = lastBX - leafDensity / 2; lx < lastBX + leafDensity / 2; lx++) {
                    for (int lz = lastBZ - leafDensity / 2; lz < lastBZ + leafDensity / 2; lz++) {
                        if (Math.pow(lx - lastBX, 2) + Math.pow(ly - lastBY, 2) + Math.pow(lz - lastBZ, 2) < Math.pow(leafDensity / 2.0, 2) + 1 && chunkData.getType(lx, ly, lz).isAir() && ly < MAX_HEIGHT) {
                            chunkData.setBlock(lx, ly, lz, leafMaterial);
                        }
                    }
                }
            }
        }

        // Add leaves at the top of the trunk (more defined shape)
        int topLeafRadius = 2 + random.nextInt(2);
        int topY = y + trunkHeight - 1;
        for (int ly = topY - topLeafRadius; ly <= topY + topLeafRadius; ly++) {
            for (int lx = x - topLeafRadius; lx <= x + topLeafRadius; lx++) {
                for (int lz = z - topLeafRadius; lz <= z + topLeafRadius; lz++) {
                    if (Math.pow(lx - x, 2) + Math.pow(ly - topY, 2) + Math.pow(lz - z, 2) <= topLeafRadius * topLeafRadius + 1 && chunkData.getType(lx, ly, lz).isAir() && ly < MAX_HEIGHT) {
                        chunkData.setBlock(lx, ly, lz, leafMaterial);
                    }
                }
            }
        }
    }

    private void drawLine(ChunkData chunkData, int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int dz = Math.abs(z2 - z1);
        int sx = (x1 < x2) ? 1 : -1;
        int sy = (y1 < y2) ? 1 : -1;
        int sz = (z1 < z2) ? 1 : -1;
        int err = dx - dy - dz;

        while (true) {
            if (y1 < MAX_HEIGHT - 1) {
                chunkData.setBlock(x1, y1, z1, material);
            }
            if (x1 == x2 && y1 == y2 && z1 == z2) break;
            int e2 = 2 * err;
            if (e2 > -dy - dz) { err -= dy + dz; x1 += sx; }
            else if (e2 < dx - dz) { err += dx - dz; y1 += sy; }
            else /* if (e2 < dx - dy) */ { err += dx - dy; z1 += sz; }
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