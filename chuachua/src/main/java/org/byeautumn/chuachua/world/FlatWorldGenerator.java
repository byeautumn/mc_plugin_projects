package org.byeautumn.chuachua.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.byeautumn.chuachua.noise.Perlin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Random;

public class FlatWorldGenerator extends ChunkGenerator {

    @Override
    public ChunkData generateChunkData(@NonNull World world, @NonNull Random random, int x, int z, BiomeGrid biome) {
        ChunkData chunk = createChunkData(world);

        // Set the biome for the entire chunk (important for flat worlds)
        for (int bx = 0; bx < 16; bx++) {
            for (int bz = 0; bz < 16; bz++) {
                biome.setBiome(bx, bz, Biome.PLAINS); // Or any other biome you want
            }
        }

        // Create the flat layers
        for (int bx = 0; bx < 16; bx++) {
            for (int bz = 0; bz < 16; bz++) {
                chunk.setBlock(bx, 0, bz, Material.BEDROCK); // Bedrock layer
                chunk.setBlock(bx, 1, bz, Material.DIRT);   // Dirt layer
                chunk.setBlock(bx, 2, bz, Material.SAND); // Sand layer

            }
        }

        return chunk;
    }



//    @Override
//    public boolean isNaturalSpawner(World world, int x, int z) {
//        return false; // Prevent natural mob spawning on the flat world
//    }
        }
