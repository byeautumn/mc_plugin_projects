package org.byeautumn.chuachua.generate.world.pipeline;

import java.awt.Color;
import java.util.Random;

public class BiomeGeneratorOriginal {

    public enum BiomeType {
        SNOWY_MOUNTAINS,
        DESERT,
        FOREST,
        PLAINS
    }

    private final long seed;
    private final Random random;

    public BiomeGeneratorOriginal(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    public float getTemperature(int x, int y) {
        return (float) Math.sin(x * 0.01 + y * 0.01);
    }

    public float getAltitude(int x, int y) {
        return (float) Math.cos(x * 0.01 - y * 0.01) * 20 + 50;
    }

    public float getHydration(int x, int y) {
        return (float) Math.sin(x * 0.01 + y * 0.01) * 0.5f;
    }

    public Color getBiomeFromFactors(float temperature, float altitude, float hydration, int x, int y) {
        if (altitude > 60.0f) {
            return Color.RED; // Snowy Mountains
        } else if (hydration < -0.1f) {
            return Color.YELLOW; // Desert
        } else if (temperature > 0.3f) {
            return Color.GREEN; // Forest
        } else {
            return Color.BLACK; // Plains
        }
    }

    public Color getBiomeColor(int x, int y) {
        return getBiomeFromFactors(getTemperature(x, y), getAltitude(x, y), getHydration(x, y), x, y);
    }

    protected float calculateBlendValue(float value, float lowerBound, float upperBound) {
        return (value - lowerBound) / (upperBound - lowerBound);
    }
}