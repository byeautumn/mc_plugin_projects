package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;
import org.byeautumn.chuachua.noise.Perlin;

import java.util.Random;

public class ProtoBiomeGeneration implements BiomeGenerator {

    private final Perlin tempNoise;
    private final Perlin hydrNoise;
    private final float noiseScale = 0.0549873201f;

    public ProtoBiomeGeneration(long tempSeed, long hydrSeed) {
        this.tempNoise = new Perlin(tempSeed);
        this.hydrNoise = new Perlin(hydrSeed);

    }

    @Override
    public void generate(World world, Random random, int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData, ChunkGenerator.BiomeGrid biomeGrid) {
        float x = chunkX + noiseScale;
        float z = chunkZ + noiseScale;
        float temp = getTemp(x, z);
        float hydr = getHydr(x, z);
        Biome biome = getBiome(temp, hydr);
        biomeGrid.setBiome(chunkX, chunkZ, biome);
    }

    private Biome getBiome(float temp, float hydr){

        if (temp < -0.5f) { // Very Cold
            if (hydr < 0.0f) {
                return Biome.SNOWY_PLAINS; // Snowy Tundra
            } else {
                return Biome.SNOWY_TAIGA; // Snowy Taiga
            }
        } else if (temp < 0.0f) { // Cold
            if (hydr < -0.5f) {
                return Biome.DESERT;  //desert
            } else if (hydr < 0.0f) {
                return Biome.PLAINS; // Plains
            }
            else{
                return Biome.TAIGA; // Taiga
            }
        } else if (temp < 0.5f) { // Temperate
            if (hydr < -0.5f) {
                return Biome.DESERT; // Desert
            } else if (hydr < 0.0f) {
                return Biome.PLAINS; // Plains
            } else if (hydr < 0.5f) {
                return Biome.FOREST; // Forest
            } else {
                return Biome.SWAMP; // Swamp
            }
        } else { // Hot
            if (hydr < -0.5f) {
                return Biome.DESERT; // Desert
            } else if (hydr < 0.0f) {
                return Biome.SAVANNA; // Savanna
            } else {
                return Biome.JUNGLE; // Jungle
            }
        }
    }

    private float getTemp(float x, float z){
        return tempNoise.layeredPerlin(x, z, 8, 0.5f);
    }
    private float getHydr (float x, float z){
        return hydrNoise.layeredPerlin(x, z, 8, 0.5f);
    }

}
