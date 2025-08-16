package org.byeautumn.chuachua.generate.world.pipeline;

import org.byeautumn.chuachua.noise.SimplexUsageOctaves;

/**
 * Handles the generation of climate values for the world generation pipeline.
 * Climate values typically influence temperature and humidity, which in turn
 * determine biome distribution.
 */
public class ClimateGenerationHandler {

    // --- Climate Noise Parameters ---
    // These constants are crucial and should be consistent with how you use this
    // in other parts of your pipeline (e.g., in NoiseVisualizer).
    public static final int CLIMATE_OCTAVES = 5;
    public static final float CLIMATE_PERSISTENCE = 0.5f;
    public static final double CLIMATE_LACUNARITY = 2.0;
    public static final double CLIMATE_SCALE = 1.0 / 1024.0; // Adjust this for desired climate patch size
    public static final long CLIMATE_SEED_OFFSET = 999L; // Offset from the main world seed

    // Expected min/max output of the Simplex noise before normalization if any
    public static final float CLIMATE_RAW_OUTPUT_MIN = -1.0f;
    public static final float CLIMATE_RAW_OUTPUT_MAX = 1.0f;

    private ClimateGenerationHandler() {
        // Private constructor to prevent instantiation, as it's a utility class
    }

    /**
     * Generates a raw climate noise value for a given world coordinate.
     * The output is typically in the range of [-1, 1] for Simplex noise.
     *
     * @param worldSeed The base seed of the world.
     * @param x The world X coordinate.
     * @param z The world Z coordinate.
     * @return The raw climate noise value.
     */
    public static double generateClimate(long worldSeed, int x, int z) {
        // Use an offset seed to make climate distinct from continentalness
        long climateNoiseSeed = worldSeed + CLIMATE_SEED_OFFSET;

        // Instantiate SimplexUsageOctaves with defined parameters
        SimplexUsageOctaves climateNoise = new SimplexUsageOctaves(
                CLIMATE_OCTAVES,
                CLIMATE_PERSISTENCE,
                CLIMATE_LACUNARITY,
                CLIMATE_SCALE,
                climateNoiseSeed,
                CLIMATE_RAW_OUTPUT_MIN,
                CLIMATE_RAW_OUTPUT_MAX,
                1.0f, // Initial amplitude (typically 1.0 for base noise)
                1.0f  // Power exponent (typically 1.0 for linear combination)
        );

        // Generate 2D noise (using 0 for the Y-coordinate as it's a 2D map)
        double climateValue = climateNoise.noise(x, 0, z);

        // Ensure the value is clamped to the expected range, though Simplex should generally stay within [-1, 1]
        return Math.max(CLIMATE_RAW_OUTPUT_MIN, Math.min(CLIMATE_RAW_OUTPUT_MAX, climateValue));
    }

    /**
     * Generates a normalized climate noise value (typically [0, 1]) for a given world coordinate.
     * This is useful if subsequent stages (like biome assignment) expect normalized inputs.
     *
     * @param worldSeed The base seed of the world.
     * @param x The world X coordinate.
     * @param z The world Z coordinate.
     * @return The normalized climate noise value (0.0 to 1.0).
     */
    public static double generateNormalizedClimate(long worldSeed, int x, int z) {
        double rawClimate = generateClimate(worldSeed, x, z);
        // Normalize from [CLIMATE_RAW_OUTPUT_MIN, CLIMATE_RAW_OUTPUT_MAX] to [0, 1]
        return (rawClimate - CLIMATE_RAW_OUTPUT_MIN) / (CLIMATE_RAW_OUTPUT_MAX - CLIMATE_RAW_OUTPUT_MIN);
    }
}