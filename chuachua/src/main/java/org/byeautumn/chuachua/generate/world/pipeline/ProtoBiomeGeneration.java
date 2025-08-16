package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.generator.ChunkGenerator.ChunkData;
import org.bukkit.block.Biome; // Import Bukkit's Biome enum
import org.byeautumn.chuachua.noise.SimplexUsageOctaves;
import org.byeautumn.chuachua.noise.WorleyNoise; // Import WorleyNoise
import org.byeautumn.chuachua.Chuachua; // Import your main plugin class

import java.io.File; // Import File
import java.io.FileWriter; // Import FileWriter
import java.io.IOException; // Import IOException
import java.text.SimpleDateFormat; // Import SimpleDateFormat for timestamp
import java.util.Date; // Import Date
import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;

// Import static constants from ProtoTerrainGeneration
import static org.byeautumn.chuachua.generate.world.pipeline.ProtoTerrainGeneration.CHUNK_SIZE;
import static org.byeautumn.chuachua.generate.world.pipeline.ProtoTerrainGeneration.MAP_ARRAY_BORDER;
import static org.byeautumn.chuachua.generate.world.pipeline.ProtoTerrainGeneration.MAP_ARRAY_DIM;


public class ProtoBiomeGeneration implements BiomeGenerator {

    private GenerationContext context;
    private final Logger stageLogger;

    // --- Biome Definition ---
    public static class BiomeParameters {
        public final int minHeight;
        public final int maxHeight;
        public final double baseNoiseScale;
        public final float warpIntensity;
        public final double heightPowerExponent;
        public final float baseNoisePersistence;
        public final float baseNoiseInitialAmp;
        public final float baseNoiseLacunarity;

        public BiomeParameters(int minHeight, int maxHeight, double baseNoiseScale, float warpIntensity, double heightPowerExponent, float baseNoisePersistence, float baseNoiseInitialAmp, float baseNoiseLacunarity) {
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.baseNoiseScale = baseNoiseScale;
            this.warpIntensity = warpIntensity;
            this.heightPowerExponent = heightPowerExponent;
            this.baseNoisePersistence = baseNoisePersistence;
            this.baseNoiseInitialAmp = baseNoiseInitialAmp;
            this.baseNoiseLacunarity = baseNoiseLacunarity;
        }
    }
    private static final BiomeParameters MOUNTAIN_BIOME = new BiomeParameters(
            140, // Increased minHeight
            300, // Increased maxHeight
            0.0007,
            25.0f,
            1.8, // Sharper peaks
            0.55f, 4.0f, 2.0f
    );
    private static final BiomeParameters PLAINS_BIOME = new BiomeParameters(
            80, // Increased minHeight significantly
            130, // Increased maxHeight
            0.003,
            8.0f,
            1.0,
            0.5f, 1.5f, 2.0f
    );
    private static final BiomeParameters COLD_OCEAN_BIOME = new BiomeParameters(
            30, // Adjusted minHeight to be deeper
            40, // Adjusted maxHeight to be deeper
            0.004,
            2.0f,
            1.0,
            0.55f, 1.2f, 2.0f
    );
    private static final BiomeParameters DEEP_OCEAN_BIOME = new BiomeParameters(
            -60, // Adjusted minHeight to be even deeper
            -20, // Adjusted maxHeight to be even deeper
            0.005,
            1.0f,
            1.0,
            0.6f, 1.0f, 2.0f
    );
    private static final BiomeParameters WARM_OCEAN_BIOME = new BiomeParameters(
            40, // Adjusted minHeight to be deeper
            50, // Adjusted maxHeight to be deeper
            0.0038,
            1.0f,
            1.0,
            0.5f, 1.1f, 2.0f
    );
    private static final BiomeParameters BEACH_BIOME = new BiomeParameters(
            60, // Min height to ensure it's consistently above water
            75, // Max height
            0.0025, // Smoother noise for flatter beaches
            0.5f, // Very little warp
            1.0,
            0.45f, 1.0f, 2.0f
    );
    // --- End Biome Definition ---

    // --- Region Map Parameters (for biome distribution) ---
    // Adjusted thresholds to map normalizedRegionValue from [-3.0, 3.0]
    // Values are scaled by 3.0 from their previous [0.0, 1.0] range.
    private static final double PLAINS_END = 0.12 * 3.0;       // Values < 0.36 are Plains
    private static final double COLD_OCEAN_END = 0.18 * 3.0;   // Values >= 0.36 and < 0.54 are Cold Ocean
    private static final double WARM_OCEAN_END = 0.25 * 3.0;   // Values >= 0.54 and < 0.75 are Warm Ocean
    private static final double BEACH_END = 0.35 * 3.0;        // Values >= 0.75 and < 1.05 are Beach

    private static final double DEEP_OCEAN_START = 0.60 * 3.0; // Values >= 1.80 and < MOUNTAINS_START are Deep Ocean
    private static final double MOUNTAINS_START = 0.85 * 3.0;  // Values >= 2.55 are Mountains
    // Any unclassified values between BEACH_END (1.05) and DEEP_OCEAN_START (1.80) will be a fallback.

    // This noise template is for the region blend map. Its range needs to be wide.
    private final SimplexUsageOctaves regionNoiseTemplate;

    // --- Static WorleyNoise instance for direct biome lookup ---
    private static WorleyNoise staticWorleyNoise;
    private static long currentStaticWorleySeed = 0; // To track if seed changes

    // Define REGION_WORLEY_SCALE here as the single source of truth
    public static final double REGION_WORLEY_SCALE = 0.005; // This value is crucial for biome size

    // --- Static fields for continuous noise logging to file ---
    private volatile static boolean isNoiseLoggingActive = false;
    private static Thread noiseLoggingThread = null;
    private static FileWriter noiseLogWriter = null;
    private static final String NOISE_LOG_FILENAME = "noise_output.csv";


    public ProtoBiomeGeneration() {
        this.stageLogger = Logger.getLogger(ProtoBiomeGeneration.class.getName());
        this.stageLogger.setLevel(Level.INFO);
        stageLogger.info("ProtoBiomeGeneration: Initialized.");

        this.regionNoiseTemplate = new SimplexUsageOctaves(
                6, 0.5f, 2.0, 1.0 / 1024.0, new Random().nextLong(), -3.0f, 3.0f, 1.0f, 1.0f
        );
    }

    // NEW: Public static method to get BiomeParameters for a given Biome
    public static BiomeParameters getBiomeParameters(Biome biomeType) {
        if (biomeType == Biome.DEEP_OCEAN) {
            return DEEP_OCEAN_BIOME;
        } else if (biomeType == Biome.COLD_OCEAN) {
            return COLD_OCEAN_BIOME;
        } else if (biomeType == Biome.WARM_OCEAN) {
            return WARM_OCEAN_BIOME;
        } else if (biomeType == Biome.BEACH) {
            return BEACH_BIOME;
        } else if (biomeType == Biome.PLAINS) {
            return PLAINS_BIOME;
        } else if (biomeType == Biome.STONY_PEAKS) {
            return MOUNTAIN_BIOME;
        } else {
            // Fallback for unhandled biomes
            return PLAINS_BIOME; // Default to plains parameters
        }
    }

    /**
     * Determines the biome at specific world coordinates (x, z) using the world seed.
     * This method directly calculates the biome without needing a full GenerationContext
     * or loaded chunk data, making it suitable for predictive lookups.
     *
     * @param seed The world seed.
     * @param worldX The world X coordinate.
     * @param worldZ The world Z coordinate.
     * @return The Biome at the given coordinates according to the generation rules.
     */
    public static Biome getBiomeAt(long seed, int worldX, int worldZ) {
        // Use the main plugin logger for these critical debug messages
        Logger mainPluginLogger = Chuachua.getInstance.getLogger();

        // TEMPORARY DEBUG LOG: Confirm entry into this method
        mainPluginLogger.log(Level.FINE,
                String.format("Entering getBiomeAt for (%d, %d)", worldX, worldZ)
        );

        // Initialize or re-initialize static WorleyNoise if seed changes
        if (staticWorleyNoise == null || currentStaticWorleySeed != seed) {
            staticWorleyNoise = new WorleyNoise(seed, 1);
            currentStaticWorleySeed = seed;
        }

        // Calculate the raw region value using the Worley noise
        double rawRegionValue = staticWorleyNoise.noise2D(
                worldX * REGION_WORLEY_SCALE,
                worldZ * REGION_WORLEY_SCALE,
                WorleyNoise.FeatureType.F2_MINUS_F1
        );

        // Normalize the region value to a -3.0 to 3.0 range
        // Original Worley noise is typically -1.0 to 1.0.
        // To map [-1, 1] to [-3, 3], multiply by 3.0.
        double normalizedRegionValue = rawRegionValue * 3.0;

        // Log for debugging purposes
        mainPluginLogger.log(Level.FINE,
                String.format("getBiomeAt(%d, %d): rawRegionValue=%.4f, normalizedRegionValue=%.4f",
                        worldX, worldZ, rawRegionValue, normalizedRegionValue)
        );

        // Determine biome based on normalizedRegionValue and thresholds
        if (normalizedRegionValue < PLAINS_END) {
            return Biome.PLAINS;
        } else if (normalizedRegionValue < COLD_OCEAN_END) {
            return Biome.COLD_OCEAN;
        } else if (normalizedRegionValue < WARM_OCEAN_END) {
            return Biome.WARM_OCEAN;
        } else if (normalizedRegionValue < BEACH_END) {
            return Biome.BEACH;
        } else if (normalizedRegionValue >= MOUNTAINS_START) {
            return Biome.STONY_PEAKS;
        } else if (normalizedRegionValue >= DEEP_OCEAN_START) {
            return Biome.DEEP_OCEAN;
        } else {
            // Fallback for the remaining gap
            return Biome.FOREST; // A more general fallback
        }
    }

    /**
     * Starts a continuous logging thread to print noise output for a small area to a file.
     *
     * @param seed The world seed.
     * @param centerX The central X coordinate for logging.
     * @param centerZ The central Z coordinate for logging.
     * @return The absolute path to the log file, or null if an error occurred.
     */
    public static String startNoiseLogging(long seed, int centerX, int centerZ) {
        if (isNoiseLoggingActive) {
            Chuachua.getInstance.getLogger().info("Noise logging is already active.");
            return null;
        }

        File dataFolder = Chuachua.getInstance.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs(); // Create plugin data folder if it doesn't exist
        }
        File logFile = new File(dataFolder, NOISE_LOG_FILENAME);

        try {
            // Append to file if it exists, otherwise create new.
            noiseLogWriter = new FileWriter(logFile, true);
            // Write header only if the file is new or empty
            if (logFile.length() == 0) {
                noiseLogWriter.write("Timestamp,X,Z,RawNoise,NormalizedNoise,PredictedBiome\n");
            }
        } catch (IOException e) {
            Chuachua.getInstance.getLogger().log(Level.SEVERE, "Failed to open noise log file: " + logFile.getAbsolutePath(), e);
            return null;
        }

        isNoiseLoggingActive = true;
        Chuachua.getInstance.getLogger().info("Starting continuous noise logging to " + logFile.getAbsolutePath() + " around (" + centerX + ", " + centerZ + ").");

        noiseLoggingThread = new Thread(() -> {
            int radius = 1; // Log a 3x3 grid around the center
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            while (isNoiseLoggingActive) {
                try {
                    String timestamp = sdf.format(new Date());
                    for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                        for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                            int currentX = centerX + xOffset;
                            int currentZ = centerZ + zOffset;

                            // Initialize or re-initialize static WorleyNoise if seed changes
                            if (staticWorleyNoise == null || currentStaticWorleySeed != seed) {
                                staticWorleyNoise = new WorleyNoise(seed, 1);
                                currentStaticWorleySeed = seed;
                            }

                            // Calculate the raw region value using the Worley noise
                            double rawRegionValue = staticWorleyNoise.noise2D(
                                    currentX * REGION_WORLEY_SCALE,
                                    currentZ * REGION_WORLEY_SCALE,
                                    WorleyNoise.FeatureType.F2_MINUS_F1
                            );

                            // Normalize the region value to a -3.0 to 3.0 range
                            double normalizedRegionValue = rawRegionValue * 3.0;

                            Biome predictedBiome = getBiomeAt(seed, currentX, currentZ); // This will also log to console (FINE level)

                            // Write to file
                            if (noiseLogWriter != null) {
                                noiseLogWriter.write(String.format("%s,%d,%d,%.4f,%.4f,%s\n",
                                        timestamp, currentX, currentZ, rawRegionValue, normalizedRegionValue, predictedBiome.name()));
                                noiseLogWriter.flush(); // Flush to ensure data is written
                            }
                        }
                    }
                } catch (IOException e) {
                    Chuachua.getInstance.getLogger().log(Level.SEVERE, "Error writing to noise log file.", e);
                    stopNoiseLogging(); // Stop logging on write error
                }

                try {
                    Thread.sleep(2000); // Log every 2 seconds
                } catch (InterruptedException e) {
                    Chuachua.getInstance.getLogger().info("Noise logging interrupted.");
                    Thread.currentThread().interrupt(); // Restore interrupt status
                    break;
                }
            }
            Chuachua.getInstance.getLogger().info("Noise logging stopped.");
            // Ensure writer is closed when thread stops
            if (noiseLogWriter != null) {
                try {
                    noiseLogWriter.close();
                } catch (IOException e) {
                    Chuachua.getInstance.getLogger().log(Level.SEVERE, "Error closing noise log file.", e);
                } finally {
                    noiseLogWriter = null;
                }
            }
        }, "ChuaChua-NoiseLogger");
        noiseLoggingThread.start();
        return logFile.getAbsolutePath();
    }

    /**
     * Stops the continuous noise logging thread and closes the file writer.
     */
    public static void stopNoiseLogging() {
        if (!isNoiseLoggingActive) {
            Chuachua.getInstance.getLogger().info("Noise logging is not active.");
            return;
        }
        isNoiseLoggingActive = false;
        if (noiseLoggingThread != null) {
            noiseLoggingThread.interrupt(); // Interrupt the thread to break sleep
            noiseLoggingThread = null;
        }
        // Writer will be closed by the thread itself upon stopping
    }

    /**
     * Locates the first occurrence of a specified biome within the biome map of the current chunk.
     * This function is useful for debugging and verifying biome distribution.
     *
     * @param context The GenerationContext containing the biomeMap.
     * @param targetBiome The Biome type to search for.
     * @return A string indicating the local (x, z) coordinates of the first found biome,
     * or "Biome not found in this chunk." if not present.
     */
    public static String locateBiome(GenerationContext context, Biome targetBiome) {
        if (context == null || context.biomeMap == null) {
            return "Error: GenerationContext or biomeMap not initialized.";
        }

        // Iterate through the biomeMap to find the target biome
        for (int localX = 0; localX < MAP_ARRAY_DIM; localX++) {
            for (int localZ = 0; localZ < MAP_ARRAY_DIM; localZ++) {
                if (context.biomeMap[localX][localZ] == targetBiome) {
                    // Convert local map coordinates to chunk-relative coordinates (0-15)
                    int chunkX = localX - MAP_ARRAY_BORDER;
                    int chunkZ = localZ - MAP_ARRAY_BORDER;
                    return String.format("Biome %s found at local chunk coordinates (x=%d, z=%d)",
                            targetBiome.name(), chunkX, chunkZ);
                }
            }
        }
        return String.format("Biome %s not found in this chunk.", targetBiome.name());
    }


    @Override
    public void generate(World world, Random random, int chunkX, int chunkZ, ChunkData chunkData, BiomeGrid biomeGrid) {
        if (context == null || context.regionBlendMap == null || context.biomeMap == null) {
            stageLogger.severe("ProtoBiomeGeneration: GenerationContext or maps not initialized! Context: " + (context == null ? "null" : "not null") + ", regionBlendMap: " + (context != null && context.regionBlendMap == null ? "null" : "not null") + ", biomeMap: " + (context != null && context.biomeMap == null ? "null" : "not null"));
            throw new IllegalStateException("GenerationContext must be set and maps initialized before generate is called.");
        }

        stageLogger.info("ProtoBiomeGeneration: Assigning biomes for chunk (" + chunkX + ", " + chunkZ + ").");

        double sumNormalizedRegionValue = 0.0; // Initialize sum for average calculation

        for (int localX = 0; localX < MAP_ARRAY_DIM; localX++) {
            for (int localZ = 0; localZ < MAP_ARRAY_DIM; localZ++) {
                // Use the new getBiomeAt method for consistency and direct calculation
                // This ensures the biome assignment here matches the predictive lookup.
                int worldX = chunkX * CHUNK_SIZE + (localX - MAP_ARRAY_BORDER);
                int worldZ = chunkZ * CHUNK_SIZE + (localZ - MAP_ARRAY_BORDER);
                Biome assignedBiome = getBiomeAt(world.getSeed(), worldX, worldZ); // This call already logs individual values


                // Set the biome in Bukkit's BiomeGrid (for 3D biomes, iterate y)
                if (biomeGrid != null) {
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                        biomeGrid.setBiome(localX - MAP_ARRAY_BORDER, y, localZ - MAP_ARRAY_BORDER, assignedBiome);
                    }
                } else {
                    stageLogger.fine("ProtoBiomeGeneration: biomeGrid is null. Skipping Bukkit BiomeGrid update for chunk (" + chunkX + ", " + chunkZ + ").");
                }

                // Store the assigned biome in the GenerationContext for later stages (e.g., terrain calculation)
                context.biomeMap[localX][localZ] = assignedBiome;

                // Add to sum for average calculation
                // Note: getBiomeAt calculates normalizedRegionValue internally, but we need to re-calculate it here
                // or pass it back if we want to avoid re-calculating noise. For simplicity, re-calculate for average.
                // A more efficient way would be for getBiomeAt to return a structure with biome and normalized value.
                double rawRegionValueForAverage = staticWorleyNoise.noise2D(
                        worldX * REGION_WORLEY_SCALE,
                        worldZ * REGION_WORLEY_SCALE,
                        WorleyNoise.FeatureType.F2_MINUS_F1
                );
                sumNormalizedRegionValue += rawRegionValueForAverage * 3.0; // Scale to [-3, 3] range
            }
        }

        // Calculate and log the average normalized region value for the chunk
        double averageNormalizedRegionValue = sumNormalizedRegionValue / (MAP_ARRAY_DIM * MAP_ARRAY_DIM);
        stageLogger.info(String.format("ProtoBiomeGeneration: Average normalizedRegionValue for chunk (%d, %d) = %.4f",
                chunkX, chunkZ, averageNormalizedRegionValue));

        stageLogger.info("ProtoBiomeGeneration: Biomes assigned for chunk (" + chunkX + ", " + chunkZ + ").");
    }

    @Override
    public void setContext(GenerationContext context) {
        this.context = context;
        stageLogger.info("ProtoBiomeGeneration: Context set.");
    }
}