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
    public ChunkData generateChunkData(@NonNull World world, @NonNull Random random, int x, int z, BiomeGrid biome) {
        ChunkData chunkData = createChunkData(world);
        float scale = 0.001f;

        for (int xx = 0; xx < 16; xx++) { // Use wx and wz for world-related loop indices
            for (int zz = 0; zz < 16; zz++) {
                int worldX = x * 16 + xx;
                int worldZ = z * 16 + zz;
                int height = perlin.getHeight(worldX * scale, worldZ * scale, MIN_HEIGHT, MAX_HEIGHT, 1, 0.0f);
                for (int yy = -64; yy < height; yy++) {

                    Material blockMaterial;


                    if (height >= SNOW_LINE_ELEVATION) {
                        blockMaterial = Material.SNOW_BLOCK;
                    } else {
                        blockMaterial = Material.GRASS_BLOCK;
                    }
                    chunkData.setBlock(xx, yy, zz, blockMaterial);

                    if (yy < height - 1) { // Changed to -1 for a smoother transition
                        blockMaterial = Material.DIRT;
                        chunkData.setBlock(xx, yy, zz, blockMaterial);
                    }
                    if (yy < height - 4) { // Deeper layers
                        blockMaterial = Material.STONE;
                        chunkData.setBlock(xx, yy, zz, blockMaterial);
                    }
                }
                Biome currentBiome = (height >= SNOW_LINE_ELEVATION) ? Biome.SNOWY_PLAINS : Biome.PLAINS;
                biome.setBiome(xx, zz, currentBiome);

            }

        }

        return chunkData;
    }


//                double heightValue = perlin.getHeight((float)xx * scale, (float)zz * scale, MIN_HEIGHT, MAX_HEIGHT);  // Adjust scale (0.02)
//                double heightValue = this.perlin2.noise(worldX * scale, worldZ * scale, 0.0);
//                double heightValue = this.perlinDS.noise(worldX * scale, worldZ * scale);

//                if (heightValue > 1.0 || heightValue < -1.0) {
//                    System.err.println("HeightValue is out of range of [-1, 1]: " + heightValue);
//                }
//                int height = (int) (((heightValue + 1.5) / 3.0) * (MAX_HEIGHT - MIN_HEIGHT) + MIN_HEIGHT);

                // Set blocks based on height

}




