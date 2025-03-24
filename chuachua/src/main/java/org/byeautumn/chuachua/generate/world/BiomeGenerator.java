package org.byeautumn.chuachua.generate.world;

import org.bukkit.Material;
import org.byeautumn.chuachua.noise.Perlin;

public class BiomeGenerator {

    private final Perlin perlin;

    public BiomeGenerator(long seed) {
        this.perlin = new Perlin(seed);
    }

    public float getTemperature(int x, int y) {
        float scale = 0.0005f; // Reduced scale for larger temperature biomes
        return perlin.perlin(x * scale, y * scale);
    }

    public float getAltitude(int x, int y) {
        int minHeight = 0;
        int maxHeight = 100;
        int octaves = 6;
        float persistence = 0.5f;

        float scale = 0.005f; // Reduced scale for larger altitude biomes

        return perlin.getHeight(x * scale, y * scale, minHeight, maxHeight, octaves, persistence);
    }

    public float getHydration(int x, int y) {
        float scale = 0.002f;
        return perlin.perlin(x * scale, y * scale);
    }

    public BiomeType getBiomeFromFactors(float temperature, float altitude, float hydration, int x, int y) {
        BiomeType biome = BiomeType.PLAINS;

        float biomeNoise = perlin.perlin(x * 0.03f, y * 0.03f);
        float biomeNoiseThreshold = -0.2f;

        float edgeNoise = perlin.perlin(x * 0.1f, y * 0.1f);
        float edgeNoiseThreshold = -0.4f;

        if (altitude > 50.0f) {
            biome = BiomeType.SNOWY_MOUNTAINS;
        } else if (hydration < -0.085f) {
            biome = BiomeType.DESERT;
        } else if (hydration > -0.065f && temperature > -0.035f) {
            biome = BiomeType.TROPICAL_RAINFOREST;
        } else if (temperature > -0.055f && hydration > -0.075f) {
            biome = BiomeType.FOREST;
        } else if (temperature < -0.057f && altitude > 27.5f) {
            biome = BiomeType.TUNDRA;
        } if (temperature < -0.057f && altitude > 27.5f && hydration < -0.081f) {
            biome = BiomeType.BARREN_WASTELAND;
        }

        float blendFactor = 0.6f;

        if (temperature > -0.065f && temperature < -0.055f && hydration > -0.080f) {
            float blendedTemp = temperature * blendFactor + (-0.055f) * (1 - blendFactor);
            float blendedHyd = hydration * blendFactor + (-0.075f) * (1 - blendFactor);
            if (blendedTemp > -0.062f && blendedHyd > -0.077f && edgeNoise > edgeNoiseThreshold) {
                biome = BiomeType.WOODLAND;
            }
        }

        if (hydration < -0.080f && hydration > -0.085f && altitude < 30.0f) {
            float blendedHyd = hydration * blendFactor + (-0.085f) * (1 - blendFactor);
            if (blendedHyd < -0.083f && edgeNoise > edgeNoiseThreshold) {
                biome = BiomeType.SAVANNA;
            }
        }

        if (hydration < -0.082f && temperature < -0.058f && altitude > 28.0f && edgeNoise > -0.2f) {
            biome = BiomeType.BARREN_WASTELAND;
        }

        if (temperature < -0.055f && temperature > -0.06f && altitude > 29.0f && altitude < 31.0f) {
            float blendedTemp = temperature * blendFactor + (-0.06f) * (1 - blendFactor);
            float blendedAlt = altitude * blendFactor + (30.0f) * (1 - blendFactor);
            if (blendedTemp < -0.059f && blendedAlt > 29.7f && edgeNoise > edgeNoiseThreshold) {
                biome = BiomeType.TAIGA;
            }
        }

        if (temperature < -0.056f && altitude > 27.0f) {
            float tundraBlend = (temperature - (-0.06f)) / (-0.056f - (-0.06f)) * (altitude - 30.0f) / (27.0f - 30.0f);
            tundraBlend = Math.max(0, Math.min(1, tundraBlend));

            if (biomeNoise > -0.1f && edgeNoise > -0.2f) {
                biome = BiomeType.TUNDRA;
            }
        }

        return biome;
    }

    public Material getBiomeColor(BiomeType biome) {
        switch (biome) {
            case SNOWY_MOUNTAINS:
                return Material.LIGHT_BLUE_CONCRETE;
            case TUNDRA:
                return Material.GRAY_CONCRETE;
            case DESERT:
                return Material.YELLOW_CONCRETE;
            case TROPICAL_RAINFOREST:
                return Material.GREEN_CONCRETE;
            case FOREST:
                return Material.GREEN_CONCRETE;
            case PLAINS:
                return Material.LIME_CONCRETE;
            case WOODLAND:
                return Material.BROWN_CONCRETE;
            case SAVANNA:
                return Material.ORANGE_CONCRETE;
            case BARREN_WASTELAND:
                return Material.RED_CONCRETE;
            case TAIGA:
                return Material.LIME_CONCRETE;
            default:
                return Material.WHITE_CONCRETE;
        }
    }

    public Material getTundraColor() {
        return Material.GRAY_CONCRETE;
    }

    public enum BiomeType {
        SNOWY_MOUNTAINS, TUNDRA, DESERT, TROPICAL_RAINFOREST, FOREST, PLAINS, WOODLAND, SAVANNA, BARREN_WASTELAND, TAIGA
    }
}
