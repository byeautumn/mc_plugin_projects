package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

public interface TerrainGenerator extends ChunkGenerationStage{
    // This method is the internal primary entry point for this pipeline stage,
    // responsible for calculating the heightmap and populating the context.
}