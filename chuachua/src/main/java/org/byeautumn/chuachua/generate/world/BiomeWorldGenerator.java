package org.byeautumn.chuachua.generate.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.byeautumn.chuachua.generate.world.BiomeGenerator;

import java.util.Random;

public class BiomeWorldGenerator extends ChunkGenerator {

    private final BiomeGenerator biomeGenerator;
    private final Random random;

    public BiomeWorldGenerator(long seed) {
        this.random = new Random(seed);
        this.biomeGenerator = new BiomeGenerator(seed);
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
        ChunkData chunkData = createChunkData(world);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                float temperature = biomeGenerator.getTemperature(worldX, worldZ);
                float altitude = biomeGenerator.getAltitude(worldX, worldZ);
                float hydration = biomeGenerator.getHydration(worldX, worldZ);

                BiomeGenerator.BiomeType biome = biomeGenerator.getBiomeFromFactors(temperature, altitude, hydration, worldX, worldZ);
                Material concreteColor = biomeGenerator.getBiomeColor(biome);

                for (int y = 0; y < 64; y++) {
                    chunkData.setBlock(x, y, z, concreteColor);
                }
            }
        }
        return chunkData;
    }
}