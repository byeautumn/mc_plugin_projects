package org.byeautumn.chuachua.generate.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Random;

import org.byeautumn.chuachua.noise.Perlin;

public class MountainWorldGenerator extends ChunkGenerator {

    private final long seed;
    private final static int SNOW_LINE_ELEVATION = 170;
    private final static int MIN_HEIGHT = 40;
    private final static int MAX_HEIGHT = 200;

    private Perlin perlin;

    public MountainWorldGenerator(long seed) {
        this.seed = seed;
        this.perlin = new Perlin(seed);
    }

    @Override
    public ChunkData generateChunkData(@NonNull World world, @NonNull Random random, int x, int z, BiomeGrid biome) {
        ChunkData chunkData = createChunkData(world);
        float scale = 0.001f;
        float epsilon = 0.1f;

        for (int xx = 0; xx < 16; xx++) {
            for (int zz = 0; zz < 16; zz++) {
                int worldX = x * 16 + xx;
                int worldZ = z * 16 + zz;

                float height1 = perlin.getHeight((worldX + epsilon) * scale, worldZ * scale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f);
                float height2 = perlin.getHeight(worldX * scale, (worldZ + epsilon) * scale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f);
                float height3 = perlin.getHeight((worldX - epsilon) * scale, worldZ * scale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f);
                float height4 = perlin.getHeight(worldX * scale, (worldZ - epsilon) * scale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f);
                float centerHeight = perlin.getHeight(worldX * scale, worldZ * scale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f);

                int height = (int) ((centerHeight * 1.25f + height1 + height2 + height3 + height4) / 5.25f);

                // Interpolate at chunk boundaries (relative to current chunk)
                if (xx == 0 && x > 0) { // Left boundary
                    float prevHeight = perlin.getHeight((worldX - 1) * scale, worldZ * scale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f);
                    height = (int) ((height + prevHeight) / 2.0f);
                }
                if (xx == 15) { // Right boundary (no need to check world bounds here)
                    float nextHeight = perlin.getHeight((worldX + 1) * scale, worldZ * scale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f);
                    height = (int) ((height + nextHeight) / 2.0f);
                }
                if (zz == 0 && z > 0) { // Front boundary
                    float prevHeight = perlin.getHeight(worldX * scale, (worldZ - 1) * scale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f);
                    height = (int) ((height + prevHeight) / 2.0f);
                }
                if (zz == 15) { // Back boundary (no need to check world bounds here)
                    float nextHeight = perlin.getHeight(worldX * scale, (worldZ + 1) * scale, MIN_HEIGHT, MAX_HEIGHT, 8, 0.5f);
                    height = (int) ((height + nextHeight) / 2.0f);
                }

                for (int yy = -64; yy < height; yy++) {
                    Material blockMaterial;
                    if (height >= SNOW_LINE_ELEVATION) {
                        blockMaterial = Material.SNOW_BLOCK;
                    } else {
                        blockMaterial = Material.GRASS_BLOCK;
                    }
                    chunkData.setBlock(xx, yy, zz, blockMaterial);

                    if (yy < height - 1) {
                        blockMaterial = Material.DIRT;
                        chunkData.setBlock(xx, yy, zz, blockMaterial);
                    }
                    if (yy < height - 4) {
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
}