package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.World; // Needed for generate method
import org.bukkit.generator.ChunkGenerator.BiomeGrid; // Needed for generate method
import org.bukkit.generator.ChunkGenerator.ChunkData; // Needed for generate method
import org.bukkit.block.Biome; // Using Bukkit's Biome enum
import org.byeautumn.chuachua.noise.WorleyNoise; // Import WorleyNoise
// Import the NoiseLogger class

import java.io.File; // Needed for NoiseLogger setup
import java.io.IOException; // Needed for NoiseLogger setup
import java.util.Random; // Needed for generate method
import java.util.logging.Level;
import java.util.logging.Logger;

// Import static constants for map dimensions
import static org.byeautumn.chuachua.generate.world.pipeline.ProtoTerrainGeneration.CHUNK_SIZE;
import static org.byeautumn.chuachua.generate.world.pipeline.ProtoTerrainGeneration.MAP_ARRAY_BORDER;
import static org.byeautumn.chuachua.generate.world.pipeline.ProtoTerrainGeneration.MAP_ARRAY_DIM;

/**
 * Assigns a specific Minecraft biome based on world coordinates, continentalness,
 * and climate values. This class uses a combination of noise and thresholds
 * to determine the final biome. It functions as a pipeline stage, populating
 * the GenerationContext's biomeMap and Bukkit's BiomeGrid.
 */
public class ProtoBiomeAssignment implements BiomeGenerator { // <-- Now correctly implements BiomeGenerator

    private static final Logger LOGGER = Logger.getLogger(ProtoBiomeAssignment.class.getName());

    // --- Worley Noise for Biome Regions ---
    // This scale determines the size of the large "biome regions" that Worley noise creates.
    // A smaller value means larger regions.
    public static final double REGION_WORLEY_SCALE = 1.0 / 256.0; // Example: 1.0 / 256 blocks per Worley cell

    // --- Continentalness Thresholds (Values typically range from -1.0 to 1.0) ---
    // These values define the boundaries for different continental features.
    // Adjust these based on the output range and distribution of your ContinentalGenerationHandler.
    public static final double DEEP_OCEAN_THRESHOLD = -0.7; // Values below this are deep ocean
    public static final double OCEAN_THRESHOLD = -0.4;    // Values between DEEP_OCEAN_THRESHOLD and this are ocean
    public static final double COAST_THRESHOLD = -0.2;    // Values between OCEAN_THRESHOLD and this are coast
    public static final double LAND_THRESHOLD = 0.2;      // Values between COAST_THRESHOLD and this are plains/forest
    public static final double MOUNTAIN_THRESHOLD = 0.6;  // Values above this are mountains

    // --- Climate Thresholds (Values typically range from -1.0 to 1.0, or 0.0 to 1.0 if normalized) ---
    // Assuming climate is raw [-1, 1] from ClimateGenerationHandler.
    // Adjust these based on the output range and distribution of your ClimateGenerationHandler.
    public static final double COLD_CLIMATE_THRESHOLD = -0.3; // Values below this are cold (snowy, frozen)
    public static final double TEMPERATE_CLIMATE_THRESHOLD = 0.3; // Values between cold and this are temperate
    // Values above this are hot (desert, jungle)

    // --- Instance fields for pipeline stage ---
    private GenerationContext context; // The shared context for this pipeline stage

    // Constructor for the pipeline stage (no longer private)
    public ProtoBiomeAssignment() {
//        LOGGER.info("ProtoBiomeAssignment: Initialized as a pipeline stage.");
    }

    /**
     * Determines the biome at a given world coordinate based on continentalness,
     * climate, and a Worley noise region value.
     *
     * @param worldSeed The base seed of the world.
     * @param x The world X coordinate.
     * @param z The world Z coordinate.
     * @param continentalnessValue The raw continentalness value (e.g., from ContinentalGenerationHandler).
     * @param climateValue The raw climate value (e.g., from ClimateGenerationHandler).
     * @return The assigned Bukkit Biome enum.
     */
    public static Biome getBiomeAt(long worldSeed, int x, int z, double continentalnessValue, double climateValue) {

        // Use a Worley noise to create distinct "regions" for macro biome assignment.
        // F2_MINUS_F1 often creates cellular patterns that are good for this.
        // We add a seed offset to make the biome region pattern unique from other noise types.
        WorleyNoise biomeRegionNoise = new WorleyNoise(worldSeed + 1000L, 1); // Use a unique seed offset for biome regions
        double regionValue = biomeRegionNoise.noise2D(x * REGION_WORLEY_SCALE, z * REGION_WORLEY_SCALE, WorleyNoise.FeatureType.F2_MINUS_F1);

        // --- Assign Biome Based on Continentalness and Climate ---
        // Prioritize conditions: Deepest first, then common, then extremes.

        if (continentalnessValue < DEEP_OCEAN_THRESHOLD) {
            return Biome.DEEP_OCEAN;
        } else if (continentalnessValue < OCEAN_THRESHOLD) {
            return Biome.OCEAN;
        } else if (continentalnessValue < COAST_THRESHOLD) {
            return Biome.BEACH; // Coastal regions
        }
        // --- Land Biomes ---
        else {
            // Apply climate thresholds for land biomes
            if (climateValue < COLD_CLIMATE_THRESHOLD) {
                // Cold regions
                if (continentalnessValue > MOUNTAIN_THRESHOLD) {
                    return Biome.SNOWY_SLOPES; // High, cold mountains
                } else if (regionValue > 0.0) { // Example using regionValue for sub-variation
                    return Biome.ICE_SPIKES; // Another cold biome variation
                } else {
                    return Biome.SNOWY_PLAINS; // Flat, cold land
                }
            } else if (climateValue < TEMPERATE_CLIMATE_THRESHOLD) {
                // Temperate regions
                if (continentalnessValue > MOUNTAIN_THRESHOLD) {
                    return Biome.STONY_PEAKS; // Temperate mountains (or regular mountains)
                } else if (regionValue > 0.0) { // Example: use region for forest vs plains
                    return Biome.FOREST;
                } else {
                    return Biome.PLAINS;
                }
            } else {
                // Hot regions
                if (continentalnessValue > MOUNTAIN_THRESHOLD) {
                    return Biome.BADLANDS; // Hot, arid mountains/plateaus
                } else if (regionValue > 0.0) { // Example: use region for jungle vs desert
                    return Biome.JUNGLE;
                } else {
                    return Biome.DESERT;
                }
            }
        }
    }

    // --- Pipeline Stage Methods ---

    @Override
    public void setContext(GenerationContext context) {
        this.context = context;
//        LOGGER.fine("ProtoBiomeAssignment: Context set for chunk (" + context.getChunkX() + ", " + context.getChunkZ() + ").");
    }

    @Override
    public void generate(World world, Random random, int chunkX, int chunkZ, ChunkData chunkData, BiomeGrid biomeGrid) {
        if (context == null || context.continentalnessMap == null || context.regionBlendMap == null || context.biomeMap == null) {
            LOGGER.severe("ProtoBiomeAssignment: GenerationContext or required maps (continentalnessMap, regionBlendMap, biomeMap) not initialized!");
            throw new IllegalStateException("GenerationContext must be set and maps populated by prior stages before generate is called.");
        }

//        LOGGER.info("ProtoBiomeAssignment: Assigning biomes for chunk (" + chunkX + ", " + chunkZ + ").");

        for (int localX = 0; localX < MAP_ARRAY_DIM; localX++) {
            for (int localZ = 0; localZ < MAP_ARRAY_DIM; localZ++) {
                int worldX = chunkX * CHUNK_SIZE + (localX - MAP_ARRAY_BORDER);
                int worldZ = chunkZ * CHUNK_SIZE + (localZ - MAP_ARRAY_BORDER);

                // Retrieve values from the GenerationContext, populated by previous stages
                double continentalnessValue = context.continentalnessMap[localX][localZ];
                double climateValue = context.regionBlendMap[localX][localZ]; // Assuming regionBlendMap holds climate data

                // Determine the biome using the static helper method
                Biome assignedBiome = getBiomeAt(world.getSeed(), worldX, worldZ, continentalnessValue, climateValue);

                // Set the biome in Bukkit's BiomeGrid for all Y levels in the column
                if (biomeGrid != null) {
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                        biomeGrid.setBiome(localX - MAP_ARRAY_BORDER, y, localZ - MAP_ARRAY_BORDER, assignedBiome);
                    }
                } else {
//                    LOGGER.fine("ProtoBiomeAssignment: biomeGrid is null. Skipping Bukkit BiomeGrid update for chunk (" + chunkX + ", " + chunkZ + ").");
                }

                // Store the assigned biome in the GenerationContext for subsequent stages (e.g., terrain generation)
                context.biomeMap[localX][localZ] = assignedBiome;

                // Optional: Log a sample point for debugging
                if (LOGGER.isLoggable(Level.FINE)) {
                    if ((localX == 0 && localZ == 0) || (localX == MAP_ARRAY_DIM - 1 && localZ == MAP_ARRAY_DIM - 1)) {
//                        LOGGER.fine(String.format("ProtoBiomeAssignment: Sample at (%d, %d) world (%d, %d): Biome=%s, Cont=%.4f, Climate=%.4f",
//                                localX, localZ, worldX, worldZ, assignedBiome.name(), continentalnessValue, climateValue));
                    }
                }
            }
        }
//        LOGGER.info("ProtoBiomeAssignment: Biomes assigned for chunk (" + chunkX + ", " + chunkZ + ").");
    }

    // --- Noise Logging Integration (Static methods for command usage) ---
    private static boolean loggingActive = false;
    private static NoiseLogger currentLogger = null;

    /**
     * Starts logging noise data to a CSV file for a specified area around a center point.
     * If logging is already active, it will log a warning and return null.
     *
     * @param seed The world seed for which noise data will be generated.
     * @param centerX The X coordinate (block) around which to center the logging.
     * @param centerZ The Z coordinate (block) around which to center the logging.
     * @return The absolute path to the log file, or null if logging could not be started.
     */
    public static String startNoiseLogging(long seed, int centerX, int centerZ) {
        if (loggingActive) {
            LOGGER.warning("Noise logging is already active.");
            return null;
        }

        try {
            String logPath = "plugins/Chuachua/noise_output.csv"; // Define your standard log file path here
            File logFile = new File(logPath);
            File parentDir = logFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs(); // Ensure parent directories exist
            }

            currentLogger = new NoiseLogger(seed, centerX, centerZ, logPath);
            currentLogger.startLogging(); // Starts the logging in a new thread
            loggingActive = true;
//            LOGGER.info("Started noise logging to: " + currentLogger.getLogFilePath());
            return currentLogger.getLogFilePath();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start noise logging due to file error.", e);
            return null;
        } catch (Exception e) { // Catch any other unexpected exceptions during logger setup
            LOGGER.log(Level.SEVERE, "An unexpected error occurred while starting noise logging.", e);
            return null;
        }
    }

    /**
     * Stops the currently active noise logging process.
     * If no logging is active, this method does nothing.
     */
    public static void stopNoiseLogging() {
        if (currentLogger != null) {
            currentLogger.stopLogging();
            currentLogger = null;
        }
        loggingActive = false;
        LOGGER.info("Stopped noise logging.");
    }
}