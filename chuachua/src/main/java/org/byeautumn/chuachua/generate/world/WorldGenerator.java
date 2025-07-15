package org.byeautumn.chuachua.generate.world;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.byeautumn.chuachua.generate.world.pipeline.ChunkGenerationStage;
import org.byeautumn.chuachua.generate.world.pipeline.GenerationContext;

import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level; // <-- Ensure this import is present

public class WorldGenerator extends ChunkGenerator{
    Map<Integer, ChunkGenerationStage> ChunkGenerationStages;
    private final Logger worldGenLogger;

    public WorldGenerator(Map<Integer, ChunkGenerationStage> chunkGenerationStages, Logger worldGenLogger) {
        this.ChunkGenerationStages = chunkGenerationStages;
        this.worldGenLogger = worldGenLogger;
        this.worldGenLogger.info("WorldGenerator: Initialized with " + chunkGenerationStages.size() + " generation stages.");
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        worldGenLogger.info("WorldGenerator: Generating chunk data for chunk (" + x + ", " + z + ") in world '" + world.getName() + "'");
        ChunkData chunkData = createChunkData(world); // This creates an empty chunk

        GenerationContext context = new GenerationContext(this.worldGenLogger);
        worldGenLogger.info("WorldGenerator: GenerationContext created for chunk (" + x + ", " + z + ").");

        for(Map.Entry<Integer, ChunkGenerationStage> entry : this.ChunkGenerationStages.entrySet()){
            int stageOrder = entry.getKey();
            ChunkGenerationStage stage = entry.getValue();

            worldGenLogger.info("WorldGenerator: Executing stage " + stageOrder + ": " + stage.getClass().getSimpleName() + " for chunk (" + x + ", " + z + ").");
            try {
                stage.setContext(context);
                stage.generate(world, random, x, z, chunkData, biome);
                worldGenLogger.info("WorldGenerator: Stage " + stage.getClass().getSimpleName() + " completed successfully for chunk (" + x + ", " + z + ").");
            } catch (Exception e) {
                // Corrected line: Use java.util.logging.Level.SEVERE
                worldGenLogger.log(java.util.logging.Level.SEVERE, "WorldGenerator: Stage " + stage.getClass().getSimpleName() + " failed for chunk (" + x + ", " + z + ")!", e);
                return createChunkData(world); // Return empty chunk on error
            }
        }
        worldGenLogger.info("WorldGenerator: All stages completed for chunk (" + x + ", " + z + "). Returning ChunkData.");
        return chunkData;
    }
}