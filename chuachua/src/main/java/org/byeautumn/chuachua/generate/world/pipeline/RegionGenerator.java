package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;

import java.util.Random;

public interface RegionGenerator extends ChunkGenerationStage {
    void generateRegionMap(int chunkX, int chunkZ, int mapWidth, int mapHeight);
}