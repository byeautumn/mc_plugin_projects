package org.byeautumn.chuachua.generate.world;

import org.byeautumn.chuachua.noise.Perlin;
import java.awt.Color;

public class BiomeGenerator {

    public final Perlin perlin;

    public BiomeGenerator(long seed) {
        this.perlin = new Perlin(seed);
    }

    public float getTemperature(int x, int y) {
        int octaves = 7;
        float persistence = 0.6f;

        float scale = 0.0005f;
        return perlin.layeredPerlin(x * scale, y * scale, octaves, persistence);
    }

    public float getAltitude(int x, int y) {
        int minHeight = 0;
        int maxHeight = 100;
        int octaves = 6;
        float persistence = 0.5f;

        float scale = 0.05f;
        return perlin.getHeight(x * scale, y * scale, minHeight, maxHeight, octaves, persistence);
    }

    public float getHydration(int x, int y) {
        int octaves = 8;
        float persistence = 0.4f;

        float scale = 0.005f;
        return perlin.layeredPerlin(x * scale, y * scale, octaves, persistence);
    }

    public Color getBiomeFromFactors(float temperature, float altitude, float hydration, int x, int y) {
        if (altitude > 50.0f) {
            return getBiomeBaseColor(BiomeType.SNOWY_MOUNTAINS);
        } else if (hydration < -0.04f) {
            return getBiomeBaseColor(BiomeType.DESERT);
        } else if (temperature > 0.5f) {
            return getBiomeBaseColor(BiomeType.FOREST);
        } else {
            return getBiomeBaseColor(BiomeType.PLAINS); // Default
        }
    }
    public Color getBiomeColor(int x, int y) {
        return getBiomeFromFactors(getTemperature(x, y), getAltitude(x, y), getHydration(x, y), x, y);
    }

    public Color getBiomeBaseColor(BiomeType biome) {
        switch (biome) {
            case SNOWY_MOUNTAINS: return Color.RED;
            case DESERT: return Color.YELLOW;
            case FOREST: return Color.GREEN;
            case PLAINS: return Color.CYAN;
            default: return Color.CYAN; // Default to PLAINS color
        }
    }

    public Color blendBiomeColors(BiomeType biome1, BiomeType biome2, float blend) {
        Color color1 = getBiomeBaseColor(biome1);
        Color color2 = getBiomeBaseColor(biome2);

        int red = (int) (color1.getRed() * (1 - blend) + color2.getRed() * blend);
        int green = (int) (color1.getGreen() * (1 - blend) + color2.getGreen() * blend);
        int blue = (int) (color1.getBlue() * (1 - blend) + color2.getBlue() * blend);

        // Clamp the color values to the valid range (0-255)
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        return new Color(red, green, blue);
    }

    public enum BiomeType {
        SNOWY_MOUNTAINS, DESERT, FOREST, PLAINS
    }
}