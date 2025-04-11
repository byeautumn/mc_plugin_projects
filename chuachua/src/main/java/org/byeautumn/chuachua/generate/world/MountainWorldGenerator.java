package org.byeautumn.chuachua.generate.world;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.LimitedRegion;
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
    private final float mountainIntensity = 0.3f;
    private final float mountainThreshold = 0.6f;
    private final float maskScale = 0.004f;
    private final int maskOctaves = 6;
    private final float maskPersistence = 0.5f;
    private final float baseScale = 0.002f;
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
    private final BiomeGenerator biomeGenerator;
    private final Logger logger;
    private final TreePopulator treePopulator;

    List<BlockPopulator> populators = new ArrayList<BlockPopulator>();

    public MountainWorldGenerator(long seed) {
        this.seed = seed;
        this.perlin = new Perlin(this.seed);
        this.mountainMaskNoise = new Perlin(this.seed + 1);
        this.highFrequencyNoise = new Perlin(this.seed + 2);
        this.topDetailNoise = new Perlin(this.seed + 5);
        this.biomeGenerator = new BiomeGenerator(this.seed);
        this.logger = Logger.getLogger("MountainWorldGenerator");
//        populators.add(new TreePopulator(seed));
        populators.add(new TreeGenerator());
//        this.treePopulator = new TreePopulator(this.seed + 3); // Different seed for tree placement

    }

//    @Override
//    public @NonNull List<BlockPopulator> getDefaultPopulators(@NonNull World world) {
//        List<BlockPopulator> populators = new ArrayList<>();
//        populators.add(treePopulator);
//        return populators;
//    }

    @Override
    public ChunkData generateChunkData(@NonNull World world, @NonNull Random random, int x, int z, @NonNull BiomeGrid biome) {
        ChunkData chunkData = createChunkData(world);

        try {
//            logger.info("[DEBUG] generateChunkData called for chunk: x=" + x + ", z=" + z + ", world=" + world.getName());

            for (int xx = 0; xx < 16; xx++) {
                for (int zz = 0; zz < 16; zz++) {
                    int worldX = x * 16 + xx;
                    int worldZ = z * 16 + zz;

//                    logger.info("[DEBUG] Processing local: xx=" + xx + ", zz=" + zz + " (world: x=" + worldX + ", z=" + worldZ + ")");

                    float baseHeightValue = (
                            perlin.getHeight((worldX + epsilon) * baseScale, worldZ * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f) * 1.25f +
                                    perlin.getHeight(worldX * baseScale, (worldZ + epsilon) * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f) +
                                    perlin.getHeight((worldX - epsilon) * baseScale, worldZ * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f) +
                                    perlin.getHeight(worldX * baseScale, (worldZ - epsilon) * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f) +
                                    perlin.getHeight(worldX * baseScale, worldZ * baseScale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f)
                    ) / 5.25f;
                    int height = (int) baseHeightValue;
//                    logger.info("[DEBUG] baseHeightValue=" + baseHeightValue + ", height=" + height);

                    float rawMaskValue = mountainMaskNoise.layeredPerlin((float) worldX * maskScale, (float) worldZ * maskScale, maskOctaves, maskPersistence);
                    float maskValue = (rawMaskValue + 1.0f) / 2.0f;
//                    logger.info("[DEBUG] rawMaskValue=" + rawMaskValue + ", maskValue=" + maskValue);

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
//                    logger.info("[DEBUG] amplification=" + amplification);

                    float finalAmplification = amplification;

                    if (height > blendStart) {
                        float blendFactor = (float) (height - blendStart) / (MAX_HEIGHT - blendStart);
                        blendFactor = Math.min(1.0f, Math.max(0.0f, blendFactor));
                        float smoothBlend = blendFactor * blendFactor * (3 - 2 * blendFactor);
                        finalAmplification = amplification * (1.0f - smoothBlend * blendFactorStrength);
                    }
//                    logger.info("[DEBUG] finalAmplification=" + finalAmplification);

                    int finalHeight = (int) (height * finalAmplification);
//                    logger.info("[DEBUG] finalHeight (before detail)=" + finalHeight);

                    if (finalHeight >= MAX_HEIGHT - 10) {
                        float detailValue = topDetailNoise.layeredPerlin(worldX * topDetailScale, worldZ * topDetailScale, 3, 0.7f);
                        float detailInfluence = detailValue * 8.0f;
                        finalHeight = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, (int) (MAX_HEIGHT + detailInfluence)));
//                        logger.info("[DEBUG] finalHeight (after detail)=" + finalHeight + ", detailValue=" + detailValue + ", detailInfluence=" + detailInfluence);
                    } else {
                        finalHeight = Math.max(MIN_HEIGHT, finalHeight);
                    }
//                    logger.info("[DEBUG] finalHeight (clamped)=" + finalHeight);

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
//                    logger.info("[DEBUG] Biome at (" + xx + ", " + zz + "): " + currentBiome + ", blockMaterial: " + blockMaterial + ", biomeColor: " + biomeColor);

                    for (int yy = world.getMinHeight(); yy < finalHeight; yy++) {
                        chunkData.setBlock(xx, yy, zz, blockMaterial);
                        if (yy < finalHeight - 1) {
                            chunkData.setBlock(xx, yy, zz, Material.STONE);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("[ERROR] Exception in generateChunkData for chunk x=" + x + ", z=" + z + ": " + e.getMessage());
            e.printStackTrace(); // Print the full stack trace to the server console
        }

        return chunkData;
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