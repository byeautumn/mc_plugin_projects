package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;

public interface BiomeGenerator extends ChunkGenerationStage {
    void generateBiomesForChunk(World world, int chunkX, int chunkZ, BiomeGrid biomeGrid);
}