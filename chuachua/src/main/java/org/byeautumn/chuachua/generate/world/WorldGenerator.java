package org.byeautumn.chuachua.generate.world;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.byeautumn.chuachua.generate.world.pipeline.ChunkGenerationStage;
import org.byeautumn.chuachua.generate.world.pipeline.GenerationContext; // Ensure this import is present

import java.util.List; // Needed for getDefaultPopulators if uncommented
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;

public class WorldGenerator extends ChunkGenerator {

    private final Map<Integer, ChunkGenerationStage> chunkGenerationStages;
    private final Logger logger;

    // You need a single GenerationContext instance per chunk generation request
    // to pass state between the pipeline stages.
    // It's created ONCE per call to generateChunkData.
    private GenerationContext currentGenerationContext;

    public WorldGenerator(Map<Integer, ChunkGenerationStage> chunkGenerationStages, Logger logger) {
        this.chunkGenerationStages = chunkGenerationStages;
        this.logger = logger;
        logger.info("WorldGenerator: Initialized with " + chunkGenerationStages.size() + " pipeline stages.");
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
        logger.info(String.format("WorldGenerator: Generating chunk data for chunk (%d, %d).", chunkX, chunkZ));

        // 1. Create a fresh context for this chunk
        //    FIX: Updated constructor call to match the new GenerationContext constructor
        currentGenerationContext = new GenerationContext(chunkX, chunkZ);

        // 2. Create the ChunkData object (from Bukkit) that will be filled
        ChunkData chunkData = createChunkData(world); // Changed 'create =' to 'createChunkData' for proper method call

        // 3. Iterate through the pipeline stages in order
        for (Map.Entry<Integer, ChunkGenerationStage> entry : chunkGenerationStages.entrySet()) {
            ChunkGenerationStage stage = entry.getValue();
            logger.fine(String.format("WorldGenerator: Executing stage %d: %s for chunk (%d, %d).",
                    entry.getKey(), stage.getClass().getSimpleName(), chunkX, chunkZ));

            // Set the context for the current stage
            stage.setContext(currentGenerationContext);

            // Execute the stage's generation logic
            stage.generate(world, random, chunkX, chunkZ, chunkData, biomeGrid);
        }

        logger.info(String.format("WorldGenerator: Finished generating chunk data for chunk (%d, %d).", chunkX, chunkZ));
        return chunkData;
    }

    // You might also need to override getDefaultPopulators if you have structures/features
    // @Override
    // public List<BlockPopulator> getDefaultPopulators(World world) {
    //     // ... return your populators ...
    //     return super.getDefaultPopulators(world); // Or return an empty list if no populators
    // }

    // Override getDefaultBiome if you want a fallback biome, though your pipeline handles this.
    // @Override
    // public Biome getDefaultBiome(World world, int x, int z) {
    //     // You could potentially use ProtoBiomeAssignment.getBiomeAt here for predictive lookups
    //     // if you also store/pass the continental and climate noise instances (or re-calculate).
    //     // For now, if your pipeline is robust, this might not be strictly necessary for generation.
    //     return Biome.PLAINS; // Default example, replace with a suitable default
    // }

    // Override getFixedSpawnLocation if you want to control the world's spawn point
    // @Override
    // public Location getFixedSpawnLocation(World world, Random random) {
    //     // You might want to generate a safe spawn location here based on your generated maps.
    //     // For example, find a land biome at a reasonable height.
    //     return new Location(world, 0, 64, 0); // Default example
    // }
}