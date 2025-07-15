package org.byeautumn.chuachua.noise;

import java.util.logging.Logger;
import java.util.logging.Level;

public class SimplexUsageOctaves {
    private int octaves;
    private float persistence;
    private double lacunarity;
    private double scale;
    private long seed;
    private float low;
    private float high;

    private float initialAmp;
    private float powerExponent;

    private final Logger noiseLogger;

    public SimplexUsageOctaves(int octaves, float persistence, double lacunarity, double scale, long seed, float low, float high, float initialAmp, float powerExponent) {
        this.octaves = octaves;
        this.persistence = persistence;
        this.lacunarity = lacunarity;
        this.scale = scale;
        this.seed = seed;
        this.low = low;
        this.high = high;
        this.initialAmp = initialAmp;
        this.powerExponent = powerExponent;
        this.noiseLogger = Logger.getLogger(SimplexUsageOctaves.class.getName());
        this.noiseLogger.setLevel(Level.FINE);
    }

    /**
     * Generates standard Fractal Brownian Motion (fBm) noise.
     * Output range is typically [-1.0f, 1.0f] before scaling to [low, high].
     * This is suitable for general terrain.
     */
    public float noise(double xCoord, double yCoord, double zCoord) {
        float totalNoise = 0;
        float totalAmplitude = 0;

        float currentAmp = this.initialAmp;
        double currentFreq = this.scale;

        for (int i = 0; i < octaves; ++i) {
            float noiseValue = OpenSimplex2S.noise3_ImproveXY(seed, (float)(xCoord * currentFreq), (float)(yCoord * currentFreq), (float)(zCoord * currentFreq));
            totalNoise += noiseValue * currentAmp;
            totalAmplitude += currentAmp;
            currentAmp *= persistence;
            currentFreq *= lacunarity;
        }

        float rawNormalizedNoise = totalNoise / totalAmplitude;

        // Scale the raw noise to the desired [low, high] range.
        // This assumes rawNormalizedNoise is [-1.0, 1.0] and maps it to [0.0, 1.0]
        float finalScaledNoise = ((rawNormalizedNoise + 1.0f) / 2.0f) * (high - low) + low;
        return finalScaledNoise;
    }


    /**
     * Generates ridged, powered, and inverted noise.
     * Output range is typically [0.0f, 1.0f] before scaling to [low, high].
     * This is suitable for specific features like canyons or sharp peaks.
     */
    public float octaveSimplex(double xCoord, double yCoord, double zCoord){
        float totalNoise = 0;
        float totalAmplitude = 0;

        float currentAmp = this.initialAmp;
        double currentFreq = this.scale;

        for (int i = 0; i < octaves; ++i) {
            float noiseValue = OpenSimplex2S.noise3_ImproveXY(seed, (float)(xCoord * currentFreq), (float)(yCoord * currentFreq), (float)(zCoord * currentFreq));
            totalNoise += noiseValue * currentAmp;
            totalAmplitude += currentAmp;
            currentAmp *= persistence;
            currentFreq *= lacunarity;
        }

        float rawNormalizedNoise = totalNoise / totalAmplitude;

        // --- Terrain shaping logic (ridged, powered, inverted) ---
        float ridgedNoise = Math.abs(rawNormalizedNoise) * -1.0f;
        float mappedForPower = (ridgedNoise + 1.0f);

        // Apply power transformation.
        // Use powerExponent from constructor.
        float poweredValue = (float) Math.pow(mappedForPower, powerExponent);

        // Invert the powered value
        float invertedPoweredValue = 1.0f - poweredValue;

        // Scale the final noise value to the desired 'low' and 'high' range.
        float finalScaledNoise = invertedPoweredValue * (high - low) + low;
        return finalScaledNoise;
    }

    // --- Getter Methods ---
    public int getOctaves() { return octaves; }
    public float getPersistence() { return persistence; }
    public double getLacunarity() { return lacunarity; }
    public double getScale() { return scale; }
    public long getSeed() { return seed; }
    public float getLow() { return low; }
    public float getHigh() { return high; }
    public float getInitialAmp() { return initialAmp; }
    public float getPowerExponent() { return powerExponent; }
}
