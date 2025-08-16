package org.byeautumn.chuachua.noise;

import org.bukkit.block.Biome;
import org.byeautumn.chuachua.Chuachua;
import org.byeautumn.chuachua.generate.world.pipeline.ClimateGenerationHandler;
import org.byeautumn.chuachua.generate.world.pipeline.ContinentalGenerationHandler;
import org.byeautumn.chuachua.generate.world.pipeline.ProtoBiomeAssignment;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.Color;

public class NoiseVisualizer {

    private static final Logger LOGGER = Logger.getLogger(NoiseVisualizer.class.getName());

    // --- General Biome Thresholds (if needed for visualization within this class) ---
    // This value represents a threshold for continentalness, e.g., below this is ocean, above is land.
    // Make sure this value is consistent with ProtoBiomeAssignment if it uses such a threshold.
    public static final double CONTINENTAL_THRESHOLD = 0.0; // Example: assuming 0.0 separates ocean from land

    // --- Biome Color Mapping (Define colors for your biomes) ---
    private static Color getBiomeColor(Biome biome) {
        switch (biome) {
            case PLAINS: return new Color(144, 195, 96); // Light Green
            case FOREST: return new Color(64, 128, 64); // Darker Green
            case DESERT: return new Color(255, 220, 128); // Sandy Yellow
            case STONY_PEAKS: return new Color(128, 128, 128); // Gray
            case OCEAN: return new Color(32, 32, 128); // Dark Blue
            case WARM_OCEAN: return new Color(64, 64, 192); // Lighter Blue
            case COLD_OCEAN: return new Color(64, 64, 128); // Muted Blue
            case DEEP_OCEAN: return new Color(16, 16, 80); // Very Dark Blue
            case BEACH: return new Color(255, 255, 160); // Light Yellow
            case SNOWY_PLAINS: return new Color(200, 220, 255); // Light Cyan/Snowy
            case SNOWY_SLOPES: return new Color(160, 160, 160); // Snowy Gray
            case JUNGLE: return new Color(0, 100, 0); // Deep Green
            case BADLANDS: return new Color(192, 96, 0); // Orange-brown
            case FROZEN_OCEAN: return new Color(100, 100, 160); // Icy Blue
            default: return Color.MAGENTA; // Fallback for unhandled biomes (easy to spot)
        }
    }

    /**
     * Generates and saves a grayscale image of Simplex noise.
     *
     * @param fileName The name of the output PNG file (e.g., "continental_map.png").
     * @param outputPath The directory path where the image should be saved.
     * @param seed The seed for the noise generator.
     * @param width The width of the output image in pixels.
     * @param height The height of the output image in pixels.
     * @param scale The frequency/scale for the noise.
     * @param octaves The number of octaves for the Simplex noise.
     * @param persistence The persistence for the Simplex noise.
     * @param lacunarity The lacunarity for the Simplex noise.
     * @param initialAmp The initial amplitude for the Simplex noise.
     * @param powerExponent The power exponent for the Simplex noise.
     * @param outputMin The expected minimum output value of the noise (for normalization to 0-255).
     * @param outputMax The expected maximum output value of the noise (for normalization to 0-255).
     */
    public static void visualizeSimplexNoise(String fileName, String outputPath, long seed, int width, int height,
                                             double scale, int octaves, float persistence, double lacunarity,
                                             float initialAmp, float powerExponent,
                                             double outputMin, double outputMax) {
        LOGGER.info("Generating Simplex noise visualization: " + fileName);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        SimplexUsageOctaves simplexNoise = new SimplexUsageOctaves(
                octaves, persistence, lacunarity, scale, seed,
                (float) outputMin, (float) outputMax, initialAmp, powerExponent
        );

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double value = simplexNoise.noise(x, 0, y);
                double normalizedValue = (value - outputMin) / (outputMax - outputMin);
                normalizedValue = Math.max(0.0, Math.min(1.0, normalizedValue));
                int rgbValue = (int) (normalizedValue * 255.0);
                int rgb = (rgbValue << 16) | (rgbValue << 8) | rgbValue;
                image.setRGB(x, y, rgb);
            }
        }

        saveImage(image, fileName, outputPath);
    }

    /**
     * Generates and saves a grayscale image of Worley noise.
     *
     * @param fileName The name of the output PNG file (e.g., "biome_regions.png").
     * @param outputPath The directory path where the image should be saved.
     * @param seed The seed for the noise generator.
     * @param width The width of the output image in pixels.
     * @param height The height of the output image in pixels.
     * @param scale The frequency/scale for the noise.
     * @param featureType The WorleyNoise.FeatureType to visualize (e.g., F2_MINUS_F1).
     * @param outputMin The expected minimum output value of the noise (for normalization to 0-255).
     * @param outputMax The expected maximum output value of the noise (for normalization to 0-255).
     */
    public static void visualizeWorleyNoise(String fileName, String outputPath, long seed, int width, int height,
                                            double scale, WorleyNoise.FeatureType featureType,
                                            double outputMin, double outputMax) {
        LOGGER.info("Generating Worley noise visualization: " + fileName);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        WorleyNoise worleyNoise = new WorleyNoise(seed, 1);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double value = worleyNoise.noise2D(x * scale, y * scale, featureType);
                double normalizedValue = (value - outputMin) / (outputMax - outputMin);
                normalizedValue = Math.max(0.0, Math.min(1.0, normalizedValue));
                int rgbValue = (int) (normalizedValue * 255.0);
                int rgb = (rgbValue << 16) | (rgbValue << 8) | rgbValue;
                image.setRGB(x, y, rgb);
            }
        }

        saveImage(image, fileName, outputPath);
    }

    /**
     * Generates and saves a colored image representing the final biome map.
     * This method now uses the ContinentalGenerationHandler and ClimateGenerationHandler classes
     * to get the intermediate noise values, simulating the full pipeline.
     *
     * @param fileName The name of the output PNG file (e.g., "final_biome_map.png").
     * @param outputPath The directory path where the image should be saved.
     * @param seed The world seed to use for all noise generators.
     * @param width The width of the output image in pixels.
     * @param height The height of the output image in pixels.
     */
    public static void visualizeFullBiomeMap(String fileName, String outputPath, long seed, int width, int height) {
        LOGGER.info("Generating full biome map visualization: " + fileName);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        WorleyNoise worleyNoise = new WorleyNoise(seed, 1);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double continentalnessValue = ContinentalGenerationHandler.generateContinentalness(seed, x, y);
                double climateValue = ClimateGenerationHandler.generateClimate(seed, x, y);

                Biome assignedBiome = ProtoBiomeAssignment.getBiomeAt(seed, x, y, continentalnessValue, climateValue);

                Color biomeColor = getBiomeColor(assignedBiome);
                image.setRGB(x, y, biomeColor.getRGB());
            }
        }

        saveImage(image, fileName, outputPath);
    }


    /**
     * Helper method to save the BufferedImage to a file.
     *
     * @param image The BufferedImage to save.
     * @param fileName The name of the file (e.g., "noise_output.png").
     * @param customOutputPath The directory path where the image should be saved. If null or empty,
     * it defaults to the plugin's data folder or current working directory.
     */
    private static void saveImage(BufferedImage image, String fileName, String customOutputPath) {
        File dataFolder = null;

        if (customOutputPath != null && !customOutputPath.trim().isEmpty()) {
            dataFolder = new File(customOutputPath);
        } else {
            try {
                dataFolder = Chuachua.getInstance.getDataFolder();
            } catch (NoClassDefFoundError | Exception e) {
                LOGGER.log(Level.WARNING, "Could not access Chuachua plugin instance. Saving noise image to current directory.", e);
                dataFolder = new File("/Users/alexgao/dev/minecraft/minecraft_spigot_server_1.21.4/io");
            }
        }

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File outputFile = new File(dataFolder, fileName);
        try {
            ImageIO.write(image, "PNG", outputFile);
            LOGGER.info("Noise image saved to: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save noise image to: " + outputFile.getAbsolutePath(), e);
        }
    }

    // --- Main Method for Independent Testing ---
    public static void main(String[] args) {
        long testSeed = 12345L;
        int imageSize = 1024;

        String myCustomOutputPath = "/Users/alexgao/dev/minecraft/minecraft_spigot_server_1.21.4/io";

        // Example 1: Visualize Continental Base Noise (using parameters directly from ContinentalGenerationHandler)
        visualizeSimplexNoise(
                "continental_base_noise_from_proto.png",
                myCustomOutputPath,
                testSeed, imageSize, imageSize,
                ContinentalGenerationHandler.BASE_SCALE,
                ContinentalGenerationHandler.BASE_OCTAVES,
                ContinentalGenerationHandler.BASE_PERSISTENCE,
                ContinentalGenerationHandler.BASE_LACUNARITY,
                1.0f, 1.0f,
                ContinentalGenerationHandler.CONTINENTALNESS_RAW_OUTPUT_MIN,
                ContinentalGenerationHandler.CONTINENTALNESS_RAW_OUTPUT_MAX
        );

        // Example 2: Visualize Climate Noise (using parameters directly from ClimateGenerationHandler)
        visualizeSimplexNoise(
                "climate_noise_from_proto.png",
                myCustomOutputPath,
                testSeed + ClimateGenerationHandler.CLIMATE_SEED_OFFSET,
                imageSize, imageSize,
                ClimateGenerationHandler.CLIMATE_SCALE,
                ClimateGenerationHandler.CLIMATE_OCTAVES,
                ClimateGenerationHandler.CLIMATE_PERSISTENCE,
                ClimateGenerationHandler.CLIMATE_LACUNARITY,
                1.0f, 1.0f,
                ClimateGenerationHandler.CLIMATE_RAW_OUTPUT_MIN,
                ClimateGenerationHandler.CLIMATE_RAW_OUTPUT_MAX
        );

        // Example 3: Visualize Worley Noise (F2-F1)
        visualizeWorleyNoise(
                "biome_worley_f2_minus_f1_visualizer.png",
                myCustomOutputPath,
                testSeed, imageSize, imageSize,
                ProtoBiomeAssignment.REGION_WORLEY_SCALE,
                WorleyNoise.FeatureType.F2_MINUS_F1,
                -1.0, 1.0
        );

        // --- Main Event: Visualize the FULL Biome Map (now using Proto classes) ---
        visualizeFullBiomeMap(
                "final_biome_map_visualizer.png",
                myCustomOutputPath,
                testSeed, imageSize, imageSize
        );

        // --- NEW: Example for visualizing the Continentalness threshold ---
        // This generates a simple black/white map based on the CONTINENTAL_THRESHOLD
        visualizeContinentalThresholdMap(
                "continental_threshold_map.png",
                myCustomOutputPath,
                testSeed, imageSize, imageSize,
                CONTINENTAL_THRESHOLD // Uses the newly defined constant
        );

        LOGGER.info("All noise visualizations completed.");
    }

    /**
     * Generates and saves a binary image representing land (white) vs. ocean (black)
     * based on the continentalness noise and a specific threshold.
     *
     * @param fileName The name of the output PNG file.
     * @param outputPath The directory path where the image should be saved.
     * @param seed The world seed.
     * @param width The width of the image.
     * @param height The height of the image.
     * @param threshold The continentalness value above which is considered land.
     */
    public static void visualizeContinentalThresholdMap(String fileName, String outputPath, long seed, int width, int height, double threshold) {
        LOGGER.info("Generating continental threshold map visualization: " + fileName);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double continentalnessValue = ContinentalGenerationHandler.generateContinentalness(seed, x, y);
                int color = (continentalnessValue >= threshold) ? Color.WHITE.getRGB() : Color.BLACK.getRGB();
                image.setRGB(x, y, color);
            }
        }
        saveImage(image, fileName, outputPath);
    }
}