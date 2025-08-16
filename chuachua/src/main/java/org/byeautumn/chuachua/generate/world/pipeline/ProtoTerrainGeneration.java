package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator; // Still needed for ChunkData and BiomeGrid types in generate method signature
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.block.Biome; // Import Bukkit's Biome enum

import org.byeautumn.chuachua.noise.SimplexUsageOctaves;

import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This file contains both ProtoTerrainGeneration and ProtoBiomeGeneration classes.
 * ProtoTerrainGeneration is the public class, and ProtoBiomeGeneration is a non-public class
 * within the same file, as per user request.
 *
 * It's generally recommended in Java to have one public class per .java file for better
 * organization and maintainability.
 */

// ProtoTerrainGeneration is responsible for calculating the heightmap and placing blocks.
// It relies on the biomeMap being populated by ProtoBiomeGeneration.
// It now correctly implements TerrainGenerator.
public class ProtoTerrainGeneration implements TerrainGenerator { // Implements TerrainGenerator

    private GenerationContext context;
    private final Logger stageLogger;

    // --- Noise templates ---
    private final SimplexUsageOctaves warpNoiseTemplateX;
    private final SimplexUsageOctaves warpNoiseTemplateZ;
    private final SimplexUsageOctaves baseHeightNoiseTemplate;

    // --- Global World Parameters ---
    private final long seed;
    private final double baseWorldScale = 1.0 / 512.0;

    // --- Constants for chunk dimensions and map array size ---
    public static final int CHUNK_SIZE = 16;
    public static final int MAP_ARRAY_BORDER = 1;
    public static final int MAP_ARRAY_DIM = CHUNK_SIZE + (2 * MAP_ARRAY_BORDER);

    // --- Water Level (Remains here as it's used in block placement) ---
    private int waterLevel = 62;

    public ProtoTerrainGeneration(long seed) {
        this.seed = seed;
        this.stageLogger = Logger.getLogger(ProtoTerrainGeneration.class.getName());
        this.stageLogger.setLevel(Level.INFO);

        // Initialize warp noise templates with a fixed output range for offsets (-1.0 to 1.0)
        // The 'power' parameter (1.2f) here is a general characteristic of the warp noise itself,
        // and its intensity will be further controlled by activeBiomeParams.warpIntensity
        // when currentWarpNoiseX/Z are created.
        this.warpNoiseTemplateX = new SimplexUsageOctaves(
                4, 0.6f, 2.0, baseWorldScale * 10.0, this.seed + 100L, -1.0f, 1.0f, 1.0f, 1.2f
        );
        this.warpNoiseTemplateZ = new SimplexUsageOctaves(
                4, 0.6f, 2.0, baseWorldScale * 10.0, this.seed + 200L, -1.0f, 1.0f, 1.0f, 1.2f
        );
        this.baseHeightNoiseTemplate = new SimplexUsageOctaves(
                6,
                0.4f,
                1.8,
                baseWorldScale, this.seed, -1f, 1.0f, 1.0f, 1.0f
        );
        stageLogger.info("ProtoTerrainGeneration: Initialized.");
    }

    @Override
    public void generate(World world, Random random, int chunkX, int chunkZ, ChunkData chunkData, BiomeGrid biomeGrid){
        if (this.context == null || context.heightmap == null || context.biomeMap == null) {
            stageLogger.severe("ProtoTerrainGeneration: GenerationContext or maps not initialized! Context: " + (context == null ? "null" : "not null") + ", heightmap: " + (context.heightmap == null ? "null" : "not null") + ", biomeMap: " + (context.biomeMap == null ? "null" : "not null"));
            throw new IllegalStateException("GenerationContext must be set and heightmap/biomeMap initialized by prior stages.");
        }

        stageLogger.info("ProtoTerrainGeneration: Starting heightmap calculation and block placement for chunk (" + chunkX + ", " + chunkZ + ").");

        // --- Heightmap Calculation based on assigned Biomes ---
        for (int localX = 0; localX < MAP_ARRAY_DIM; localX++) {
            for (int localZ = 0; localZ < MAP_ARRAY_DIM; localZ++) {
                double worldX = chunkX * CHUNK_SIZE + (localX - MAP_ARRAY_BORDER);
                double worldZ = chunkZ * CHUNK_SIZE + (localZ - MAP_ARRAY_BORDER);

                Biome biomeType = context.biomeMap[localX][localZ]; // Get biome from context.biomeMap
                // Retrieve BiomeParameters from ProtoBiomeGeneration
                ProtoBiomeGeneration.BiomeParameters activeBiomeParams = ProtoBiomeGeneration.getBiomeParameters(biomeType);

                // Corrected: Use fixed low/high for warp noise, and pass biome-specific warpIntensity as the power.
                // The output of octaveSimplex will then already have the warp intensity applied.
                SimplexUsageOctaves currentWarpNoiseX = new SimplexUsageOctaves(
                        warpNoiseTemplateX.getOctaves(),
                        activeBiomeParams.baseNoisePersistence,
                        activeBiomeParams.baseNoiseLacunarity,
                        activeBiomeParams.baseNoiseScale * 10.0,
                        warpNoiseTemplateX.getSeed(),
                        -1.0f, 1.0f, // FIXED: Warp noise should output a normalized range for offsets
                        activeBiomeParams.baseNoiseInitialAmp,
                        activeBiomeParams.warpIntensity // Use biome-specific warp intensity as the power
                );
                SimplexUsageOctaves currentWarpNoiseZ = new SimplexUsageOctaves(
                        warpNoiseTemplateZ.getOctaves(),
                        activeBiomeParams.baseNoisePersistence,
                        activeBiomeParams.baseNoiseLacunarity,
                        activeBiomeParams.baseNoiseScale * 10.0,
                        warpNoiseTemplateZ.getSeed(),
                        -1.0f, 1.0f, // FIXED: Warp noise should output a normalized range for offsets
                        activeBiomeParams.baseNoiseInitialAmp,
                        activeBiomeParams.warpIntensity // Use biome-specific warp intensity as the power
                );

                // Removed redundant multiplication by activeBiomeParams.warpIntensity here,
                // as it's already applied by the SimplexUsageOctaves's 'power' parameter.
                float offsetX = (float) (currentWarpNoiseX.octaveSimplex(worldX, 0, worldZ));
                float offsetZ = (float) (currentWarpNoiseZ.octaveSimplex(worldX, 0, worldZ));

                double warpedWorldX = worldX + offsetX;
                double warpedWorldZ = worldZ + offsetZ;

                SimplexUsageOctaves currentBaseHeightNoise = new SimplexUsageOctaves(
                        baseHeightNoiseTemplate.getOctaves(),
                        activeBiomeParams.baseNoisePersistence, activeBiomeParams.baseNoiseLacunarity,
                        activeBiomeParams.baseNoiseScale, // Use active biome's noise scale
                        baseHeightNoiseTemplate.getSeed(),
                        baseHeightNoiseTemplate.getLow(), baseHeightNoiseTemplate.getHigh(),
                        activeBiomeParams.baseNoiseInitialAmp, // Use active biome's initial amplitude
                        (float) activeBiomeParams.heightPowerExponent
                );

                double normalizedNoise = currentBaseHeightNoise.noise(warpedWorldX, waterLevel, warpedWorldZ);

                // SAFEGURAD: Explicitly clamp normalizedNoise to the expected [-1, 1] range
                // This prevents out-of-bounds values from SimplexUsageOctaves (if any)
                // from causing extreme height calculations.
                normalizedNoise = Math.max(-1.0, Math.min(1.0, normalizedNoise));

                double finalHeight = normalizedNoise * (activeBiomeParams.maxHeight - activeBiomeParams.minHeight) + activeBiomeParams.minHeight;

                // Ensure final height is within Minecraft's world height limits
                finalHeight = Math.max(world.getMinHeight() + 1, finalHeight);
                finalHeight = Math.min(world.getMaxHeight() - 1, finalHeight);

                context.heightmap[localX][localZ] = finalHeight;

                if ((localX == 0 && localZ == 0) || (localX == MAP_ARRAY_DIM - 1 && localZ == MAP_ARRAY_DIM - 1)) {
                    stageLogger.fine("ProtoTerrainGeneration: Sample heightmap[" + localX + "][" + localZ + "] = " + finalHeight + " (Normalized Noise: " + normalizedNoise + ")");
                }
            }
        }
        stageLogger.info("ProtoTerrainGeneration: Heightmap generated for chunk (" + chunkX + ", " + chunkZ + ").");


        // --- Block Placement ---
        stageLogger.info("ProtoTerrainGeneration: Starting block placement for chunk (" + chunkX + ", " + chunkZ + ").");
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int surfaceHeight = (int) Math.round(context.heightmap[x + MAP_ARRAY_BORDER][z + MAP_ARRAY_BORDER]);
                // Get biome from context.biomeMap for consistency with height calculation
                Biome biomeType = context.biomeMap[x + MAP_ARRAY_BORDER][z + MAP_ARRAY_BORDER];

                for (int y = world.getMinHeight(); y <= surfaceHeight; y++) {
                    if (y == world.getMinHeight()) {
                        chunkData.setBlock(x, y, z, Material.BEDROCK);
                    } else if (y < surfaceHeight - 3) {
                        chunkData.setBlock(x, y, z, Material.STONE);
                    } else if (y < surfaceHeight) {
                        // Determine top layer material based on biome
                        if (biomeType == Biome.BEACH) {
                            chunkData.setBlock(x, y, z, Material.SAND);
                        } else if (biomeType == Biome.WARM_OCEAN || biomeType == Biome.LUKEWARM_OCEAN || biomeType == Biome.COLD_OCEAN || biomeType == Biome.DEEP_OCEAN) {
                            chunkData.setBlock(x, y, z, Material.SAND); // Ocean floor top layer
                        }
                        else {
                            chunkData.setBlock(x, y, z, Material.DIRT);
                        }
                    } else if (surfaceHeight < waterLevel && y == surfaceHeight) {
                        // For underwater terrain surface
                        if (biomeType == Biome.DEEP_OCEAN) {
                            chunkData.setBlock(x, y, z, Material.GRAVEL);
                        } else { // Shallow ocean or other underwater
                            chunkData.setBlock(x, y, z, Material.SAND);
                        }
                    } else { // Topmost block above water
                        if (biomeType == Biome.BEACH) {
                            chunkData.setBlock(x, y, z, Material.SAND);
                        } else {
                            chunkData.setBlock(x, y, z, Material.GRASS_BLOCK);
                        }
                    }
                }
            }
        }

        // Fill water in air pockets below waterLevel
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                for (int y = world.getMinHeight(); y < waterLevel; y++) {
                    if (chunkData.getType(x, y, z) == Material.AIR) {
                        chunkData.setBlock(x, y, z, Material.WATER);
                    }
                    // Ensure underwater dirt/stone are converted to sand/gravel
                    if (y < waterLevel && (chunkData.getType(x, y, z) == Material.DIRT || chunkData.getType(x, y, z) == Material.STONE)) {
                        Biome biomeType = context.biomeMap[x + MAP_ARRAY_BORDER][z + MAP_ARRAY_BORDER]; // Use context.biomeMap
                        if (biomeType == Biome.DEEP_OCEAN) {
                            chunkData.setBlock(x, y, z, Material.GRAVEL);
                        } else if (biomeType == Biome.COLD_OCEAN || biomeType == Biome.WARM_OCEAN || biomeType == Biome.LUKEWARM_OCEAN || biomeType == Biome.BEACH) {
                            chunkData.setBlock(x, y, z, Material.SAND);
                        }
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