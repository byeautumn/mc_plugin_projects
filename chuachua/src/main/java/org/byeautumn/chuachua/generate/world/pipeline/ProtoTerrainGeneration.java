package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import org.byeautumn.chuachua.noise.SimplexUsageOctaves;

import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ProtoTerrainGeneration implements TerrainGenerator {

    // --- Biome Definition (unchanged from previous fix) ---
    public static class BiomeParameters {
        public final int minHeight;
        public final int maxHeight;
        public final double baseNoiseScale;
        public final float warpIntensity;
        public final double heightPowerExponent;
        public final float baseNoisePersistence;
        public final float baseNoiseInitialAmp;
        public final float baseNoiseLacunarity;

        public BiomeParameters(int minHeight, int maxHeight, double baseNoiseScale, float warpIntensity, double heightPowerExponent, float baseNoisePersistence, float baseNoiseInitialAmp, float baseNoiseLacunarity) {
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.baseNoiseScale = baseNoiseScale;
            this.warpIntensity = warpIntensity;
            this.heightPowerExponent = heightPowerExponent;
            this.baseNoisePersistence = baseNoisePersistence;
            this.baseNoiseInitialAmp = baseNoiseInitialAmp;
            this.baseNoiseLacunarity = baseNoiseLacunarity;
        }
    }
    private static final BiomeParameters MOUNTAIN_BIOME = new BiomeParameters(
            60, 200,
            0.0008,
            80.0f,
            1.0, // Keep at 1.0 for linear base noise
            0.6f, 4.0f, 2.0f
    );
    private static final BiomeParameters PLAINS_BIOME = new BiomeParameters(
            40, 90,
            0.0035,
            20.0f,
            1.0, // Keep at 1.0 for linear base noise
            0.6f, 1.5f, 2.0f
    );
    // --- End Biome Definition ---

    // --- Noise templates ---
    private final SimplexUsageOctaves warpNoiseTemplateX;
    private final SimplexUsageOctaves warpNoiseTemplateZ;
    private final SimplexUsageOctaves baseHeightNoiseTemplate;

    // --- Global World Parameters ---
    private final long seed;
    private final double baseWorldScale = 1.0 / 2048.0; // Keep this smaller scale for now

    // --- Constants for chunk dimensions and map array size ---
    public static final int CHUNK_SIZE = 16;
    public static final int MAP_ARRAY_BORDER = 1;
    public static final int MAP_ARRAY_DIM = CHUNK_SIZE + (2 * MAP_ARRAY_BORDER);

    // --- Region Map Parameters ---
    private static final double REGION_THRESHOLD = 0.5;
    private static final double REGION_BLEND_ZONE_WIDTH = 0.2;

    // --- Water Level ---
    private int waterLevel = 63;

    // --- Shared Context ---
    private GenerationContext context;

    // --- Logger for this class ---
    private final Logger stageLogger;


    public ProtoTerrainGeneration(long seed) {
        this.seed = seed;
        this.stageLogger = Logger.getLogger(ProtoTerrainGeneration.class.getName());
        this.stageLogger.setLevel(Level.INFO);

        // Constructor calls for SimplexUsageOctaves, now passing long seed:
        // SimplexUsageOctaves(int octaves, float persistence, double lacunarity, double scale, long seed, float low, float high, float initialAmp, float powerExponent)
        // For warp noise, it's okay to use powerExponent > 1.0 if you want sharper warps.
        this.warpNoiseTemplateX = new SimplexUsageOctaves(
                4, 0.6f, 2.0, baseWorldScale * 10.0, this.seed + 100L, -1.0f, 1.0f, 1.0f, 2.0f // Power exponent for warp can be higher
        );
        this.warpNoiseTemplateZ = new SimplexUsageOctaves(
                4, 0.6f, 2.0, baseWorldScale * 10.0, this.seed + 200L, -1.0f, 1.0f, 1.0f, 2.0f // Power exponent for warp can be higher
        );
        // For base height noise, powerExponent is 1.0 to get a full, linear range from noise()
        this.baseHeightNoiseTemplate = new SimplexUsageOctaves(
                8, 0.5f, 2.0, baseWorldScale, this.seed, -1f, 1.0f, 1.0f, 1.0f // Set powerExponent to 1.0 here
        );
        stageLogger.info("ProtoTerrainGeneration: Initialized.");
    }

    @Override
    public void generate(World world, Random random, int chunkX, int chunkZ, ChunkData chunkData, BiomeGrid biomeGrid){
        if (context == null || context.regionBlendMap == null || context.heightmap == null) {
            stageLogger.severe("ProtoTerrainGeneration: GenerationContext or maps not initialized! Context: " + (context == null ? "null" : "not null") + ", regionBlendMap: " + (context != null && context.regionBlendMap == null ? "null" : "not null") + ", heightmap: " + (context != null && context.heightmap == null ? "null" : "not null"));
            throw new IllegalStateException("GenerationContext must be set and maps initialized before generateChunkData is called.");
        }

        stageLogger.info("ProtoTerrainGeneration: Starting heightmap calculation for chunk (" + chunkX + ", " + chunkZ + ").");
        stageLogger.info("ProtoTerrainGeneration: Accessing regionBlendMap[0][0] = " + context.regionBlendMap[0][0]);

        try {
            double minCalculatedHeight = Double.MAX_VALUE;
            double maxCalculatedHeight = Double.MIN_VALUE;
            double minNormalizedNoise = Double.MAX_VALUE;
            double maxNormalizedNoise = Double.MIN_VALUE;


            for (int localX = 0; localX < MAP_ARRAY_DIM; localX++) {
                for (int localZ = 0; localZ < MAP_ARRAY_DIM; localZ++) {
                    double worldX = chunkX * CHUNK_SIZE + (localX - MAP_ARRAY_BORDER);
                    double worldZ = chunkZ * CHUNK_SIZE + (localZ - MAP_ARRAY_BORDER);

                    double regionMapValue = context.regionBlendMap[localX][localZ];

                    BiomeParameters biomeParamsA = PLAINS_BIOME;
                    BiomeParameters biomeParamsB = MOUNTAIN_BIOME;

                    double blendFactor = 0.0;
                    double lowerBound = REGION_THRESHOLD - (REGION_BLEND_ZONE_WIDTH / 2.0);
                    double upperBound = REGION_THRESHOLD + (REGION_BLEND_ZONE_WIDTH / 2.0);

                    if (regionMapValue < lowerBound) {
                        blendFactor = 0.0;
                    } else if (regionMapValue > upperBound) {
                        blendFactor = 1.0;
                    } else {
                        double blendProgress = (regionMapValue - lowerBound) / REGION_BLEND_ZONE_WIDTH;
                        blendFactor = blendProgress * blendProgress * (3 - 2 * blendProgress);
                    }

                    int activeMinHeight = (int) lerp(biomeParamsA.minHeight, biomeParamsB.minHeight, blendFactor);
                    int activeMaxHeight = (int) lerp(biomeParamsA.maxHeight, biomeParamsB.maxHeight, blendFactor);
                    double activeBaseNoiseScale = lerp(biomeParamsA.baseNoiseScale, biomeParamsB.baseNoiseScale, blendFactor);
                    float activeWarpIntensity = (float) lerp(biomeParamsA.warpIntensity, biomeParamsB.warpIntensity, blendFactor);
                    double activeHeightPowerExponent = lerp(biomeParamsA.heightPowerExponent, biomeParamsB.heightPowerExponent, blendFactor);
                    float activeBaseNoisePersistence = (float) lerp(biomeParamsA.baseNoisePersistence, biomeParamsB.baseNoisePersistence, blendFactor);
                    float activeBaseNoiseInitialAmp = (float) lerp(biomeParamsA.baseNoiseInitialAmp, biomeParamsB.baseNoiseInitialAmp, blendFactor);
                    float activeBaseNoiseLacunarity = (float) lerp(biomeParamsA.baseNoiseLacunarity, biomeParamsB.baseNoiseLacunarity, blendFactor);


                    SimplexUsageOctaves currentWarpNoiseX = new SimplexUsageOctaves(
                            warpNoiseTemplateX.getOctaves(),
                            warpNoiseTemplateX.getPersistence(), warpNoiseTemplateX.getLacunarity(),
                            warpNoiseTemplateX.getScale(), warpNoiseTemplateX.getSeed(),
                            warpNoiseTemplateX.getLow(), warpNoiseTemplateX.getHigh(),
                            warpNoiseTemplateX.getInitialAmp(), warpNoiseTemplateX.getPowerExponent()
                    );
                    SimplexUsageOctaves currentWarpNoiseZ = new SimplexUsageOctaves(
                            warpNoiseTemplateZ.getOctaves(),
                            warpNoiseTemplateZ.getPersistence(), warpNoiseTemplateZ.getLacunarity(),
                            warpNoiseTemplateZ.getScale(), warpNoiseTemplateZ.getSeed(),
                            warpNoiseTemplateZ.getLow(), warpNoiseTemplateZ.getHigh(),
                            warpNoiseTemplateZ.getInitialAmp(), warpNoiseTemplateZ.getPowerExponent()
                    );

                    // Use octaveSimplex for warp, as it's a specific shaping
                    float offsetX = (float) (currentWarpNoiseX.octaveSimplex(worldX, 0, worldZ) * activeWarpIntensity);
                    float offsetZ = (float) (currentWarpNoiseZ.octaveSimplex(worldX, 0, worldZ) * activeWarpIntensity);

                    double warpedWorldX = worldX + offsetX;
                    double warpedWorldZ = worldZ + offsetZ;

                    SimplexUsageOctaves currentBaseHeightNoise = new SimplexUsageOctaves(
                            baseHeightNoiseTemplate.getOctaves(),
                            activeBaseNoisePersistence, activeBaseNoiseLacunarity,
                            activeBaseNoiseScale,
                            baseHeightNoiseTemplate.getSeed(),
                            baseHeightNoiseTemplate.getLow(), baseHeightNoiseTemplate.getHigh(),
                            activeBaseNoiseInitialAmp,
                            (float) activeHeightPowerExponent // This will be 1.0 as per BiomeParameters
                    );

                    // --- CRITICAL CHANGE: Use the new 'noise()' method for base terrain ---
                    double normalizedNoise = currentBaseHeightNoise.noise(warpedWorldX, waterLevel, warpedWorldZ);
                    if (normalizedNoise < minNormalizedNoise) minNormalizedNoise = normalizedNoise;
                    if (normalizedNoise > maxNormalizedNoise) maxNormalizedNoise = normalizedNoise;

                    double finalHeight = normalizedNoise * (activeMaxHeight - activeMinHeight) + activeMinHeight;

                    finalHeight = Math.max(world.getMinHeight() + 1, finalHeight);
                    finalHeight = Math.min(world.getMaxHeight() - 1, finalHeight);

                    context.heightmap[localX][localZ] = finalHeight;

                    if (finalHeight < minCalculatedHeight) minCalculatedHeight = finalHeight;
                    if (finalHeight > maxCalculatedHeight) maxCalculatedHeight = finalHeight;

                    if ((localX == 0 && localZ == 0) || (localX == MAP_ARRAY_DIM - 1 && localZ == MAP_ARRAY_DIM - 1)) {
                        stageLogger.fine("ProtoTerrainGeneration: Sample heightmap[" + localX + "][" + localZ + "] = " + finalHeight + " (Normalized Noise: " + normalizedNoise + ")");
                    }
                }
            }
            stageLogger.info("ProtoTerrainGeneration: Heightmap generated for chunk (" + chunkX + ", " + chunkZ + "). Min Height: " + minCalculatedHeight + ", Max Height: " + maxCalculatedHeight + ". Normalized Noise Range: [" + minNormalizedNoise + ", " + maxNormalizedNoise + "]");

        } catch (Exception e) {
            stageLogger.log(Level.SEVERE,
                    "[ERROR] ProtoTerrainGeneration: Exception during height calculation for chunk (" + chunkX + ", " + chunkZ + ")", e);
            throw e;
        }

        stageLogger.info("ProtoTerrainGeneration: Starting block placement for chunk (" + chunkX + ", " + chunkZ + ").");
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int surfaceHeight = (int) Math.round(context.heightmap[x + MAP_ARRAY_BORDER][z + MAP_ARRAY_BORDER]);
                if ((x == 0 && z == 0) || (x == CHUNK_SIZE -1 && z == CHUNK_SIZE -1)) {
                    stageLogger.fine("ProtoTerrainGeneration: Chunk (" + chunkX + "," + chunkZ + ") Local (" + x + "," + z + ") Surface Height: " + surfaceHeight);
                }

                for (int y = world.getMinHeight(); y <= surfaceHeight; y++) {
                    if (y == world.getMinHeight()) {
                        chunkData.setBlock(x, y, z, Material.BEDROCK);
                    } else if (y < surfaceHeight - 3) {
                        chunkData.setBlock(x, y, z, Material.STONE);
                    } else if (y < surfaceHeight) {
                        chunkData.setBlock(x, y, z, Material.DIRT);
                    } else if (surfaceHeight < waterLevel && y == surfaceHeight) {
                        chunkData.setBlock(x, y, z, Material.GRAVEL);
                    } else {
                        chunkData.setBlock(x, y, z, Material.GRASS_BLOCK);
                    }
                }
            }
        }

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                for (int y = world.getMinHeight(); y < waterLevel; y++) {
                    if (chunkData.getType(x, y, z) == Material.AIR) {
                        chunkData.setBlock(x, y, z, Material.WATER);
                    }
                    if (y < waterLevel && chunkData.getType(x, y, z) == Material.DIRT) {
                        chunkData.setBlock(x, y, z, Material.STONE);
                    }
                }
            }
        }
        stageLogger.info("ProtoTerrainGeneration: Finished block placement for chunk (" + chunkX + ", " + chunkZ + ").");
    }

    @Override
    public void setContext(GenerationContext context) {
        this.context = context;
        stageLogger.info("ProtoTerrainGeneration: Context set.");
    }

    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }
}