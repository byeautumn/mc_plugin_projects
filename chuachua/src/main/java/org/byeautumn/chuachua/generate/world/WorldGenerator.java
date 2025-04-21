package org.byeautumn.chuachua.generate.world;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.byeautumn.chuachua.generate.world.pipeline.ChunkGenerationStage;

import java.util.Map;
import java.util.Random;

public class WorldGenerator extends ChunkGenerator{
    Map<Integer, ChunkGenerationStage> ChunkGenerationStages;

    public WorldGenerator(Map<Integer, ChunkGenerationStage> chunkGenerationStages) {
        ChunkGenerationStages = chunkGenerationStages;

    }


    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
        ChunkData chunkData = createChunkData(world);
        for(ChunkGenerationStage stage : this.ChunkGenerationStages.values()){
            stage.generate(world, random, x, z, chunkData, biome);
        }
        return chunkData;
    }
}
