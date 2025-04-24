package org.byeautumn.chuachua.generate.world.pipeline;

import org.byeautumn.chuachua.noise.Perlin;

import java.util.logging.Logger;

public class LocationBiomeValues {

    private final Perlin tempNoise;
    private final Perlin hydrNoise;
    private final Perlin altitudeNoise; // Added altitude noise
    private final Perlin continentalNoise; // Added continental noise
    private final Perlin regionalNoise;    // Added regional noise
    private final Perlin erosionNoise;    // Added regional noise
    private final int octaves = 4;
    private final float persistence = 0.5f;
    private final Logger logger;
    private BiomeConstants biomeConstants;


    public LocationBiomeValues(long tempSeed, long hydrSeed, long altitudeSeed, long continentalSeed, long regionalSeed, long erosionSeed) {
        this.tempNoise = new Perlin(tempSeed);
        this.hydrNoise = new Perlin(hydrSeed);
        this.altitudeNoise = new Perlin(altitudeSeed);
        this.continentalNoise = new Perlin(continentalSeed); //initialize the noise
        this.regionalNoise = new Perlin(regionalSeed);
        this.erosionNoise = new Perlin(erosionSeed);
        this.logger = Logger.getLogger("LocationBiomeValues");
        this.biomeConstants = new BiomeConstants();

    }

    public float getTemp(float x, float z) {
        float temp = tempNoise.layeredPerlin(x * biomeConstants.getTempScale(), z * biomeConstants.getTempScale(), octaves, persistence);
//        logger.info("getTemp: temp: " + temp + ".");
        return temp;
    }

    public float getHydr(float x, float z) {
        float hydr = hydrNoise.layeredPerlin(x * biomeConstants.getHydrScale(), z * biomeConstants.getHydrScale(), octaves, persistence);
//        logger.info("getHydr: hydr: " + hydr + ".");
        return hydr;
    }

    public float getAltitude(float x, float z) {
        float altitude = altitudeNoise.layeredPerlin(x * biomeConstants.getAltitudeScale(), z * biomeConstants.getAltitudeScale(), octaves, persistence);
//        logger.info("getAltitude: altitude: " + altitude + ".");
        return altitude;
    }

    public float getContinental(float x, float z) {
        float continental = continentalNoise.layeredPerlin(x * biomeConstants.getContinentalScale(), z * biomeConstants.getContinentalScale(), octaves, persistence);
//        logger.info("getContinental: continental: " + continental + ".");
        return continental;
    }

    public float getRegional(float x, float z) {
        float regional = regionalNoise.layeredPerlin(x * biomeConstants.getRegionalScale(), z * biomeConstants.getRegionalScale(), octaves, persistence);
//        logger.info("getRegional: regional: " + regional + ".");
        return regional;
    }
    public float getErosion(float x, float z) {
        float regional = erosionNoise.layeredPerlin(x * biomeConstants.getErosionScale(), z * biomeConstants.getErosionScale(), octaves, persistence);
//        logger.info("getRegional: regional: " + regional + ".");
        return regional;
    }

}
