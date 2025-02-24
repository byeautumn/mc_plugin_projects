package org.byeautumn.chuachua.generate.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.byeautumn.chuachua.generate.Generable;
import org.byeautumn.chuachua.noise.Perlin;
import org.byeautumn.chuachua.noise.Perlin2;
import org.byeautumn.chuachua.noise.PerlinDS;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class MountainWorldGenerator extends ChunkGenerator {
    private final long seed;
    private final static int SNOW_LINE_ELEVATION = 170;
    private final static int MIN_HEIGHT = 40;
    private final static int MAX_HEIGHT = 200;

    private Perlin perlin;
    private final Perlin2 perlin2;
    private final PerlinDS perlinDS;

    public MountainWorldGenerator(long seed) {
        this.seed = seed;
        this.perlin = new Perlin(seed);
        this.perlin2 = new Perlin2(seed);
        this.perlinDS = new PerlinDS(seed);
    }

    @Override
    public ChunkData generateChunkData(@NonNull World world, @NonNull Random random, int x, int z, ChunkGenerator.BiomeGrid biome) {
        ChunkData chunkData = createChunkData(world);

        for (int xx = 0; xx < 16; xx++) {
            for (int zz = 0; zz < 16; zz++) {
                int worldX = x * 16 + xx;
                int worldZ = z * 16 + zz;

                float scale = 0.001f;
//                double heightValue = perlin.getHeight((float)xx * scale, (float)zz * scale, MIN_HEIGHT, MAX_HEIGHT);  // Adjust scale (0.02)
//                double heightValue = this.perlin2.noise(worldX * scale, worldZ * scale, 0.0);
                double heightValue = this.perlinDS.noise(worldX * scale, worldZ * scale);

//                if (heightValue > 1.0 || heightValue < -1.0) {
//                    System.err.println("HeightValue is out of range of [-1, 1]: " + heightValue);
//                }
                int height = (int) (((heightValue + 1.5) / 3.0) * (MAX_HEIGHT - MIN_HEIGHT) + MIN_HEIGHT);

                // Set blocks based on height
                for (int yy = -64; yy < height; yy++) {
                    if (height >= SNOW_LINE_ELEVATION) {
                        chunkData.setBlock(xx, yy, zz, Material.SNOW_BLOCK); // Surface
                    } else {
                        chunkData.setBlock(xx, yy, zz, Material.GRASS_BLOCK); // Surface
                    }

                    if (yy < height - 4) {
                        chunkData.setBlock(xx, yy, zz, Material.DIRT); // Below surface
                    }
                    if (yy < height - 4) {
                        chunkData.setBlock(xx, yy, zz, Material.STONE); // Below surface
                    }
                }

                // Set biome (important for other world features)
                if (height >= SNOW_LINE_ELEVATION) {
                    biome.setBiome(xx, zz, Biome.SNOWY_PLAINS);
                } else {
                    biome.setBiome(xx, zz, Biome.PLAINS);
                }

            }
        }
        return chunkData;
    }

}
