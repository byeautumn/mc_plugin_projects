package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;
import java.util.logging.Logger; // <-- Import Logger
import java.util.logging.Level;  // <-- Import Level

// --- NEW IMPORT ---
import static org.byeautumn.chuachua.generate.world.pipeline.GenerationContext.MAP_ARRAY_DIM; // <-- ADD THIS LINE

public class ProtoBiomeGeneration implements BiomeGenerator {

    private final long seed;
    private GenerationContext context;
    private Logger stageLogger; // <-- New field for logger

    private static final double REGION_THRESHOLD = 0.5;
    private static final double REGION_BLEND_ZONE_WIDTH = 0.2;

    public ProtoBiomeGeneration(long seed) {
        this.seed = seed;
        // Logger initialized in setContext, as context provides the parent logger
    }

    @Override
    public void setContext(GenerationContext context) {
        this.context = context;
        if (context != null) {
            this.stageLogger = context.getLogger(); // <-- Get logger from context
            stageLogger.info("ProtoBiomeGeneration: Context set.");
        } else {
            // Fallback logger if context is null (shouldn't happen if WorldGenerator is correct)
            this.stageLogger = Logger.getLogger(ProtoBiomeGeneration.class.getName());
            this.stageLogger.setLevel(Level.SEVERE); // Default to SEVERE if no context
            stageLogger.severe("ProtoBiomeGeneration: setContext called with null context!");
        }
    }

    @Override
    public void generateBiomesForChunk(World world, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
        if (context == null || context.regionBlendMap == null || context.heightmap == null) {
            stageLogger.severe("ProtoBiomeGeneration: GenerationContext, regionBlendMap, or heightmap not initialized! Context: " + (context == null ? "null" : "not null") + ", regionBlendMap: " + (context != null && context.regionBlendMap == null ? "null" : "not null") + ", heightmap: " + (context != null && context.heightmap == null ? "null" : "not null"));
            throw new IllegalStateException("GenerationContext not set or maps not initialized.");
        }
        stageLogger.info("ProtoBiomeGeneration: Starting biome assignment for chunk (" + chunkX + ", " + chunkZ + ").");

        int CHUNK_SIZE = 16;
        int MAP_ARRAY_BORDER = 1;

        try {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    double regionMapValue = context.regionBlendMap[x + MAP_ARRAY_BORDER][z + MAP_ARRAY_BORDER];
                    int surfaceHeight = (int) Math.round(context.heightmap[x + MAP_ARRAY_BORDER][z + MAP_ARRAY_BORDER]);

                    Biome assignedBiome;

                    double lowerBound = REGION_THRESHOLD - (REGION_BLEND_ZONE_WIDTH / 2.0);
                    double upperBound = REGION_THRESHOLD + (REGION_BLEND_ZONE_WIDTH / 2.0);

                    if (regionMapValue < lowerBound) {
                        assignedBiome = Biome.PLAINS;
                    } else if (regionMapValue > upperBound) {
                        assignedBiome = Biome.STONY_PEAKS; // Using STONY_PEAKS as an example for mountains
                    } else {
                        // In the blend zone, you could choose a transition biome,
                        // or just pick one based on a slight bias.
                        if (regionMapValue < REGION_THRESHOLD) {
                            assignedBiome = Biome.FOREST;
                        } else {
                            assignedBiome = Biome.TAIGA;
                        }
                    }

                    // Optional: Further refine biome based on height or other factors
                    if (assignedBiome == Biome.STONY_PEAKS && surfaceHeight > world.getMaxHeight() * 0.8) {
                        assignedBiome = Biome.SNOWY_SLOPES;
                    } else if (assignedBiome == Biome.PLAINS && surfaceHeight < world.getSeaLevel() - 5) {
                        assignedBiome = Biome.DEEP_OCEAN;
                    } else if (surfaceHeight < world.getSeaLevel() && assignedBiome != Biome.DEEP_OCEAN) {
                        assignedBiome = Biome.OCEAN;
                    }

                    biomeGrid.setBiome(x, z, assignedBiome);
                    // Log a sample biome assignment
                    if (x == 0 && z == 0) {
                        stageLogger.fine("ProtoBiomeGeneration: Sample biome[0][0] = " + assignedBiome.name() + " (regionValue: " + regionMapValue + ", height: " + surfaceHeight + ")");
                    }
                }
            }
            stageLogger.info("ProtoBiomeGeneration: Biomes assigned for chunk (" + chunkX + ", " + chunkZ + ").");
        } catch (Exception e) {
            stageLogger.log(Level.SEVERE, "ProtoBiomeGeneration: Exception during biome assignment for chunk (" + chunkX + ", " + chunkZ + ")", e);
            throw e; // Re-throw to ensure WorldGenerator catches it
        }
    }

    @Override
    public void generate(World world, Random random, int chunkX, int chunkZ, ChunkData chunkData, BiomeGrid biomeGrid) {
        // Ensure logger is available even if setContext was called with null (unlikely but safe)
        if (stageLogger == null && context != null) {
            this.stageLogger = context.getLogger();
        } else if (stageLogger == null) {
            this.stageLogger = Logger.getLogger(ProtoBiomeGeneration.class.getName());
            this.stageLogger.setLevel(Level.SEVERE);
            this.stageLogger.severe("ProtoBiomeGeneration: generate() called without context or logger initialized!");
        }

        stageLogger.info("ProtoBiomeGeneration: Generic generate method called for chunk (" + chunkX + ", " + chunkZ + "). Delegating to generateBiomesForChunk.");
        generateBiomesForChunk(world, chunkX, chunkZ, biomeGrid);
    }
}