package org.byeautumn.chuachua.generate.world.pipeline;

import org.byeautumn.chuachua.noise.SimplexUsageOctaves;

/**
 * Handles the generation of continentalness values for the world generation pipeline.
 * Continentalness typically defines large-scale landmasses and oceans, and influences
 * altitude and overall biome distribution.
 */
public class ContinentalGenerationHandler {

    // --- Continental Noise Parameters ---
    // These constants are crucial and must be consistent with how you use this
    // in other parts of your pipeline (e.g., in NoiseVisualizer and ProtoBiomeAssignment).

    // Base Continental Noise (large scale)
    public static final int BASE_OCTAVES = 6;
    public static final float BASE_PERSISTENCE = 0.55f;
    public static final double BASE_LACUNARITY = 2.0;
    public static final double BASE_SCALE = 1.0 / 4096.0; // Very large scale

    // Detail Noise 1 (medium scale)
    public static final int DETAIL1_OCTAVES = 5;
    public static final float DETAIL1_PERSISTENCE = 0.5f;
    public static final double DETAIL1_LACUNARITY = 2.0;
    public static final double DETAIL1_SCALE = 1.0 / 2048.0; // Medium scale
    public static final long DETAIL1_SEED_OFFSET = 123L; // Offset from base world seed

    // Detail Noise 2 (smaller scale)
    public static final int DETAIL2_OCTAVES = 4;
    public static final float DETAIL2_PERSISTENCE = 0.45f;
    public static final double DETAIL2_LACUNARITY = 2.0;
    public static final double DETAIL2_SCALE = 1.0 / 1024.0; // Smaller scale
    public static final long DETAIL2_SEED_OFFSET = 456L; // Offset from base world seed

    // Global Bias Noise (very large, slow-changing)
    // Often used to tilt or bias the entire continental pattern
    public static final int GLOBAL_BIAS_OCTAVES = 3;
    public static final float GLOBAL_BIAS_PERSISTENCE = 0.6f;
    public static final double GLOBAL_BIAS_LACUNARITY = 2.0;
    public static final double GLOBAL_BIAS_SCALE = 1.0 / 8192.0; // Extremely large scale
    public static final long GLOBAL_BIAS_SEED_OFFSET = 789L; // Offset from base world seed
    public static final float GLOBAL_BIAS_POWER_EXPONENT = 2.0f; // Can use a power to shape bias

    // Weights for combining noise layers
    public static final double BASE_WEIGHT = 0.5;
    public static final double DETAIL1_WEIGHT = 0.3;
    public static final double DETAIL2_WEIGHT = 0.15;
    public static final double GLOBAL_BIAS_WEIGHT = 0.05;

    // Expected min/max output of the combined continentalness value
    public static final float CONTINENTALNESS_RAW_OUTPUT_MIN = -1.0f;
    public static final float CONTINENTALNESS_RAW_OUTPUT_MAX = 1.0f;

    private ContinentalGenerationHandler() {
        // Private constructor to prevent instantiation, as it's a utility class
    }

    /**
     * Generates a raw continentalness value for a given world coordinate by combining multiple
     * Simplex noise layers. The output is typically in the range of [-1, 1].
     *
     * @param worldSeed The base seed of the world.
     * @param x The world X coordinate.
     * @param z The world Z coordinate.
     * @return The raw combined continentalness noise value.
     */
    public static double generateContinentalness(long worldSeed, int x, int z) {
        // Instantiate and evaluate each noise layer
        SimplexUsageOctaves continentalBaseNoise = new SimplexUsageOctaves(
                BASE_OCTAVES, BASE_PERSISTENCE, BASE_LACUNARITY, BASE_SCALE, worldSeed,
                CONTINENTALNESS_RAW_OUTPUT_MIN, CONTINENTALNESS_RAW_OUTPUT_MAX, 1.0f, 1.0f
        );
        double base = continentalBaseNoise.noise(x, 0, z);

        SimplexUsageOctaves continentalDetailNoise1 = new SimplexUsageOctaves(
                DETAIL1_OCTAVES, DETAIL1_PERSISTENCE, DETAIL1_LACUNARITY, DETAIL1_SCALE, worldSeed + DETAIL1_SEED_OFFSET,
                CONTINENTALNESS_RAW_OUTPUT_MIN, CONTINENTALNESS_RAW_OUTPUT_MAX, 1.0f, 1.0f
        );
        double detail1 = continentalDetailNoise1.noise(x, 0, z);

        SimplexUsageOctaves continentalDetailNoise2 = new SimplexUsageOctaves(
                DETAIL2_OCTAVES, DETAIL2_PERSISTENCE, DETAIL2_LACUNARITY, DETAIL2_SCALE, worldSeed + DETAIL2_SEED_OFFSET,
                CONTINENTALNESS_RAW_OUTPUT_MIN, CONTINENTALNESS_RAW_OUTPUT_MAX, 1.0f, 1.0f
        );
        double detail2 = continentalDetailNoise2.noise(x, 0, z);

        SimplexUsageOctaves continentalGlobalBias = new SimplexUsageOctaves(
                GLOBAL_BIAS_OCTAVES, GLOBAL_BIAS_PERSISTENCE, GLOBAL_BIAS_LACUNARITY, GLOBAL_BIAS_SCALE, worldSeed + GLOBAL_BIAS_SEED_OFFSET,
                CONTINENTALNESS_RAW_OUTPUT_MIN, CONTINENTALNESS_RAW_OUTPUT_MAX, 1.0f, GLOBAL_BIAS_POWER_EXPONENT
        );
        double globalBias = continentalGlobalBias.noise(x, 0, z);

        // Combine the noise layers with their respective weights
        double combinedContinentalNoise =
                (base * BASE_WEIGHT) +
                        (detail1 * DETAIL1_WEIGHT) +
                        (detail2 * DETAIL2_WEIGHT) +
                        (globalBias * GLOBAL_BIAS_WEIGHT);

        // Ensure the value is clamped to the expected range of the combined noise
        // This is important because summing weighted values can sometimes push it slightly outside [-1, 1]
        return Math.max(CONTINENTALNESS_RAW_OUTPUT_MIN, Math.min(CONTINENTALNESS_RAW_OUTPUT_MAX, combinedContinentalNoise));
    }

    /**
     * Generates a normalized continentalness value (typically [0, 1]) for a given world coordinate.
     * This is useful if subsequent stages (like biome assignment) expect normalized inputs.
     *
     * @param worldSeed The base seed of the world.
     * @param x The world X coordinate.
     * @param z The world Z coordinate.
     * @return The normalized continentalness value (0.0 to 1.0).
     */
    public static double generateNormalizedContinentalness(long worldSeed, int x, int z) {
        double rawContinentalness = generateContinentalness(worldSeed, x, z);
        // Normalize from [CONTINENTALNESS_RAW_OUTPUT_MIN, CONTINENTALNESS_RAW_OUTPUT_MAX] to [0, 1]
        return (rawContinentalness - CONTINENTALNESS_RAW_OUTPUT_MIN) / (CONTINENTALNESS_RAW_OUTPUT_MAX - CONTINENTALNESS_RAW_OUTPUT_MIN);
    }
}