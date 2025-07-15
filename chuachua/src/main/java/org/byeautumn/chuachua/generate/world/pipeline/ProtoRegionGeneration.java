package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import org.byeautumn.chuachua.noise.WorleyNoise;
import org.byeautumn.chuachua.noise.WorleyNoise.FeatureType;

import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;

import static org.byeautumn.chuachua.generate.world.pipeline.GenerationContext.MAP_ARRAY_DIM;

public class ProtoRegionGeneration implements RegionGenerator {

    private final WorleyNoise worleyNoise; // Declared as final
    private final long seed;
    private GenerationContext context;
    private Logger stageLogger;

    private static final double REGION_WORLEY_SCALE = 0.00005;

    public ProtoRegionGeneration(long seed) {
        this.seed = seed;
        this.worleyNoise = new WorleyNoise(seed, 1); // <-- CRITICAL FIX: Initialize worleyNoise here!
        // Logger initialized in setContext, as context provides the parent logger
    }

    @Override
    public void setContext(GenerationContext context) {
        this.context = context;
        if (context != null) {
            this.stageLogger = context.getLogger();
            stageLogger.info("ProtoRegionGeneration: Context set.");
        } else {
            this.stageLogger = Logger.getLogger(ProtoRegionGeneration.class.getName());
            this.stageLogger.setLevel(Level.SEVERE);
            stageLogger.severe("ProtoRegionGeneration: setContext called with null context!");
        }
    }

    @Override
    public void generateRegionMap(int chunkX, int chunkZ, int mapWidth, int mapHeight) {
        if (context == null || context.regionBlendMap == null) {
            stageLogger.severe("ProtoRegionGeneration: GenerationContext or regionBlendMap not initialized! Context: " + (context == null ? "null" : "not null") + ", regionBlendMap: " + (context != null && context.regionBlendMap == null ? "null" : "not null"));
            throw new IllegalStateException("GenerationContext not set or regionBlendMap not initialized.");
        }
        stageLogger.info("ProtoRegionGeneration: Starting generateRegionMap for chunk (" + chunkX + ", " + chunkZ + "). Map dimensions: " + mapWidth + "x" + mapHeight);

        int CHUNK_SIZE = 16;
        int MAP_ARRAY_BORDER = 1;

        try {
            for (int localX = 0; localX < mapWidth; localX++) {
                for (int localZ = 0; localZ < mapHeight; localZ++) {
                    double worldX = chunkX * CHUNK_SIZE + (localX - MAP_ARRAY_BORDER);
                    double worldZ = chunkZ * CHUNK_SIZE + (localZ - MAP_ARRAY_BORDER);

                    // This call will now work because worleyNoise is initialized
                    double rawRegionValue = worleyNoise.noise2D(
                            worldX * REGION_WORLEY_SCALE,
                            worldZ * REGION_WORLEY_SCALE,
                            FeatureType.F2_MINUS_F1
                    );

                    double normalizedRegionValue = (rawRegionValue + 1.0) / 2.0;

                    context.regionBlendMap[localX][localZ] = normalizedRegionValue;
                    if (localX == 0 && localZ == 0) {
                        stageLogger.fine("ProtoRegionGeneration: Sample regionBlendMap[0][0] = " + normalizedRegionValue);
                    }
                }
            }
            stageLogger.info("ProtoRegionGeneration: Region map populated for chunk (" + chunkX + ", " + chunkZ + ").");
        } catch (Exception e) {
            stageLogger.log(Level.SEVERE, "ProtoRegionGeneration: Exception during region map generation for chunk (" + chunkX + ", " + chunkZ + ")", e);
            throw e;
        }
    }

    @Override
    public void generate(World world, Random random, int chunkX, int chunkZ, ChunkData chunkData, BiomeGrid biomeGrid) {
        if (stageLogger == null && context != null) {
            this.stageLogger = context.getLogger();
        } else if (stageLogger == null) {
            this.stageLogger = Logger.getLogger(ProtoRegionGeneration.class.getName());
            this.stageLogger.setLevel(Level.SEVERE);
            this.stageLogger.severe("ProtoRegionGeneration: generate() called without context or logger initialized!");
        }

        stageLogger.info("ProtoRegionGeneration: Generic generate method called for chunk (" + chunkX + ", " + chunkZ + "). Delegating to generateRegionMap.");

        generateRegionMap(chunkX, chunkZ, MAP_ARRAY_DIM, MAP_ARRAY_DIM);
    }
}