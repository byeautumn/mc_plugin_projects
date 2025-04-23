package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;
import java.util.logging.Logger;

public class ProtoBiomeGeneration implements BiomeGenerator {

    
    private final Logger logger;
    private final LocationBiomeValues locationBiomeValues;
    private final BiomeConstants biomeConstants;


    public ProtoBiomeGeneration(Long seed) { //added seeds
        this.logger = Logger.getLogger("ProtoBiomeGeneration");
        this.locationBiomeValues = new LocationBiomeValues(seed + 2, seed + 1, seed, seed + 3, seed+ 4);
        this.biomeConstants= new BiomeConstants();
    }

    @Override
    public void generate(World world, Random random, int chunkX, int chunkZ, ChunkGenerator.ChunkData chunkData, ChunkGenerator.BiomeGrid biomeGrid) {
        float x = chunkX + biomeConstants.getNoiseScale();
        float z = chunkZ + biomeConstants.getNoiseScale();
        float temp = locationBiomeValues.getTemp(x, z);
        float hydr = locationBiomeValues.getHydr(x, z);
        float altitude = locationBiomeValues.getAltitude(x, z);
        float continental = locationBiomeValues.getContinental(x, z); //get values.
        float regional = locationBiomeValues.getRegional(x, z);       //get values

        // Biome blending
        for (int bx = 0; bx < 16; bx++) {
            for (int bz = 0; bz < 16; bz++) {
                float normalizedXInChunk = bx / 15.0f;
                float normalizedZInChunk = bz / 15.0f;

                // 4 corner values for temp, hydr, and altitude
                float tempNW = locationBiomeValues.getTemp(x + normalizedXInChunk - biomeConstants.getNoiseScale(), z + normalizedZInChunk - biomeConstants.getNoiseScale());
                float tempNE = locationBiomeValues.getTemp(x + normalizedXInChunk + biomeConstants.getNoiseScale(), z + normalizedZInChunk - biomeConstants.getNoiseScale());
                float tempSW = locationBiomeValues.getTemp(x + normalizedXInChunk - biomeConstants.getNoiseScale(), z + normalizedZInChunk + biomeConstants.getNoiseScale());
                float tempSE = locationBiomeValues.getTemp(x + normalizedXInChunk + biomeConstants.getNoiseScale(), z + normalizedZInChunk + biomeConstants.getNoiseScale());

                float hydrNW = locationBiomeValues.getHydr(x + normalizedXInChunk - biomeConstants.getNoiseScale(), z + normalizedZInChunk - biomeConstants.getNoiseScale());
                float hydrNE = locationBiomeValues.getHydr(x + normalizedXInChunk + biomeConstants.getNoiseScale(), z + normalizedZInChunk - biomeConstants.getNoiseScale());
                float hydrSW = locationBiomeValues.getHydr(x + normalizedXInChunk - biomeConstants.getNoiseScale(), z + normalizedZInChunk + biomeConstants.getNoiseScale());
                float hydrSE = locationBiomeValues.getHydr(x + normalizedXInChunk + biomeConstants.getNoiseScale(), z + normalizedZInChunk + biomeConstants.getNoiseScale());

                float altitudeNW = locationBiomeValues.getAltitude(x + normalizedXInChunk - biomeConstants.getNoiseScale(), z + normalizedZInChunk - biomeConstants.getNoiseScale());
                float altitudeNE = locationBiomeValues.getAltitude(x + normalizedXInChunk + biomeConstants.getNoiseScale(), z + normalizedZInChunk - biomeConstants.getNoiseScale());
                float altitudeSW = locationBiomeValues.getAltitude(x + normalizedXInChunk - biomeConstants.getNoiseScale(), z + normalizedZInChunk + biomeConstants.getNoiseScale());
                float altitudeSE = locationBiomeValues.getAltitude(x + normalizedXInChunk + biomeConstants.getNoiseScale(), z + normalizedZInChunk + biomeConstants.getNoiseScale());

                float continentalNW = locationBiomeValues.getContinental(x + normalizedXInChunk - biomeConstants.getNoiseScale(), z + normalizedZInChunk - biomeConstants.getNoiseScale());
                float continentalNE = locationBiomeValues.getContinental(x + normalizedXInChunk + biomeConstants.getNoiseScale(), z + normalizedZInChunk - biomeConstants.getNoiseScale());
                float continentalSW = locationBiomeValues.getContinental(x + normalizedXInChunk - biomeConstants.getNoiseScale(), z + normalizedZInChunk + biomeConstants.getNoiseScale());
                float continentalSE = locationBiomeValues.getContinental(x + normalizedXInChunk + biomeConstants.getNoiseScale(), z + normalizedZInChunk + biomeConstants.getNoiseScale());

                float regionalNW = locationBiomeValues.getRegional(x + normalizedXInChunk - biomeConstants.getNoiseScale(), z + normalizedZInChunk - biomeConstants.getNoiseScale());
                float regionalNE = locationBiomeValues.getRegional(x + normalizedXInChunk + biomeConstants.getNoiseScale(), z + normalizedZInChunk - biomeConstants.getNoiseScale());
                float regionalSW = locationBiomeValues.getRegional(x + normalizedXInChunk - biomeConstants.getNoiseScale(), z + normalizedZInChunk + biomeConstants.getNoiseScale());
                float regionalSE = locationBiomeValues.getRegional(x + normalizedXInChunk + biomeConstants.getNoiseScale(), z + normalizedZInChunk + biomeConstants.getNoiseScale());

                // Bilinear interpolation
                float factorX = bx / 15.0f;
                float factorZ = bz / 15.0f;

                float interpolatedTempN = (tempNW * (1 - factorX) + tempNE * factorX);
                float interpolatedTempS = (tempSW * (1 - factorX) + tempSE * factorX);
                float interpolatedTemp = (interpolatedTempN * (1 - factorZ) + interpolatedTempS * factorZ);

                float interpolatedHydrN = (hydrNW * (1 - factorX) + hydrNE * factorX);
                float interpolatedHydrS = (hydrSW * (1 - factorX) + hydrSE * factorX);
                float interpolatedHydr = (interpolatedHydrN * (1 - factorZ) + interpolatedHydrS * factorZ);

                float interpolatedAltitudeN = (altitudeNW * (1 - factorX) + altitudeNE * factorX);
                float interpolatedAltitudeS = (altitudeSW * (1 - factorX) + altitudeSE * factorX);
                float interpolatedAltitude = (interpolatedAltitudeN * (1 - factorZ) + interpolatedAltitudeS * factorZ);

                float interpolatedContinentalN = (continentalNW * (1 - factorX) + continentalNE * factorX);
                float interpolatedContinentalS = (continentalSW * (1 - factorX) + continentalSE * factorX);
                float interpolatedContinental = (interpolatedContinentalN * (1 - factorZ) + interpolatedContinentalS * factorZ);

                float interpolatedRegionalN = (regionalNW * (1 - factorX) + regionalNE * factorX);
                float interpolatedRegionalS = (regionalSW * (1 - factorX) + regionalSE * factorX);
                float interpolatedRegional = (interpolatedRegionalN * (1 - factorZ) + interpolatedRegionalS * factorZ);

                biomeGrid.setBiome(chunkX + bx, chunkZ + bz, getBiome(interpolatedTemp, interpolatedHydr, interpolatedAltitude, interpolatedContinental, interpolatedRegional)); // Pass new parameters
            }
        }
    }

    private Biome getBiome(float temp, float hydr, float altitude, float continental, float regional) { //added continental and regional
        if (altitude < -0.2f) {
            return Biome.DEEP_OCEAN;
        } else if (altitude < 0.0f) {
            return Biome.OCEAN;
        }



        if (continental < -0.8f) { // Example continental influence
            return Biome.DESERT;
        } else if (continental > 0.8f) {
            return Biome.TAIGA;
        }

        if (regional > 0.5f)
        {
            return Biome.JUNGLE;
        }

        if (temp < -0.15f) {
            if (hydr < -0.15f) {
                return Biome.SNOWY_PLAINS;
            } else {
                return Biome.SNOWY_TAIGA;
            }
        } else if (temp < -0.05f) {
            if (hydr < -0.1f) {
                return Biome.DESERT;
            } else if (hydr < 0.0f) {
                return Biome.PLAINS;
            } else {
                return Biome.TAIGA;
            }
        } else if (temp < 0.05f) {
            if (hydr < -0.05f) {
                return Biome.DESERT;
            } else if (hydr < 0.05f) {
                return Biome.PLAINS;
            } else if (hydr < 0.1f) {
                return Biome.FOREST;
            } else {
                return Biome.SWAMP;
            }
        } else {
            if (hydr < -0.05f) {
                return Biome.DESERT;
            } else if (hydr < 0.05f) {
                return Biome.SAVANNA;
            } else {
                return Biome.JUNGLE;
            }
        }
    }
}
