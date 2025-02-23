package org.byeautumn.chuachua.generate.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.byeautumn.chuachua.generate.Generable;
import org.byeautumn.chuachua.noise.Perlin;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Random;

public class MountainWorldGenerator extends ChunkGenerator {
    private double scale = 0.02d; // Adjust for terrain scale
    private double heightScale = 20.0d; // Adjust for terrain height
    private Random random;
    private final long seed = 12261977L;
    private final Perlin perlin = new Perlin(seed);

    @Override
    public ChunkGenerator.ChunkData generateChunkData(@NonNull World world, @NonNull Random random, int x, int z, ChunkGenerator.BiomeGrid biome) {
        ChunkData chunkData = createChunkData(world);

        for (int xx = 0; xx < 16; xx++) {
            for (int zz = 0; zz < 16; zz++) {
                int worldX = x * 16 + xx;
                int worldZ = z * 16 + zz;

                // Use Perlin noise to calculate height
                double heightValue = this.perlin.getHeight(worldX * scale, worldZ * scale);
                int height = (int) (heightValue * heightScale) + 64; // Adjust baseline height (64)

                // Set blocks based on height
                for (int yy = 0; yy < height; yy++) {
                    chunkData.setBlock(xx, yy, zz, Material.GRASS_BLOCK); // Surface
                    if (yy < height - 4) {
                        chunkData.setBlock(xx, yy, zz, Material.DIRT); // Below surface
                    }
                    if (yy < height - 4) {
                        chunkData.setBlock(xx, yy, zz, Material.STONE); // Below surface
                    }
                }

                // Set biome (important for other world features)
                biome.setBiome(xx, zz, Biome.PLAINS); // Example biome - Customize as needed
            }
        }
        return chunkData;
    }
}
