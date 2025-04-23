package org.byeautumn.chuachua.generate.world.pipeline;

import org.byeautumn.chuachua.noise.Perlin;

import java.util.logging.Logger;

public class LocationBiomeValues {

    private final Perlin tempNoise;
    private final Perlin hydrNoise;
    private final Perlin altitudeNoise; // Added altitude noise
    private final Perlin continentalNoise; // Added continental noise
    private final Perlin regionalNoise;    // Added regional noise
    private final int octaves = 4;
    private final float persistence = 0.5f;
    private final float tempScale = 0.007f;
    private final float hydrScale = 0.007f;
    private final float altitudeScale = 0.02f;
    private final float continentalScale = 0.01f; //scaling for continental
    private final float regionalScale = 0.05f;    //scaling for regional
    private final Logger logger;


    public LocationBiomeValues(long tempSeed, long hydrSeed, long altitudeSeed, long continentalSeed, long regionalSeed) {
        this.tempNoise = new Perlin(tempSeed);
        this.hydrNoise = new Perlin(hydrSeed);
        this.altitudeNoise = new Perlin(altitudeSeed);
        this.continentalNoise = new Perlin(continentalSeed); //initialize the noise
        this.regionalNoise = new Perlin(regionalSeed);
        this.logger = Logger.getLogger("LocationBiomeValues");

    }

    public float getTemp(float x, float z) {
        float temp = tempNoise.layeredPerlin(x * tempScale, z * tempScale, octaves, persistence);
//        logger.info("getTemp: temp: " + temp + ".");
        return temp;
    }

    public float getHydr(float x, float z) {
        float hydr = hydrNoise.layeredPerlin(x * hydrScale, z * hydrScale, octaves, persistence);
//        logger.info("getHydr: hydr: " + hydr + ".");
        return hydr;
    }

    public float getAltitude(float x, float z) {
        float altitude = altitudeNoise.layeredPerlin(x * altitudeScale, z * altitudeScale, octaves, persistence);
//        logger.info("getAltitude: altitude: " + altitude + ".");
        return altitude;
    }

    public float getContinental(float x, float z) {
        float continental = continentalNoise.layeredPerlin(x * continentalScale, z * continentalScale, octaves, persistence);
//        logger.info("getContinental: continental: " + continental + ".");
        return continental;
    }

    public float getRegional(float x, float z) {
        float regional = regionalNoise.layeredPerlin(x * regionalScale, z * regionalScale, octaves, persistence);
//        logger.info("getRegional: regional: " + regional + ".");
        return regional;
    }
}
