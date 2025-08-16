package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.block.Biome;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class to continuously log noise and biome data to a CSV file.
 * This runs on a separate thread to avoid blocking the main server thread.
 *
 * The logged data includes X, Z coordinates, Continentalness, Climate, and assigned Biome.
 */
public class NoiseLogger implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(NoiseLogger.class.getName());

    private final long worldSeed;
    private final int startX; // Starting X coordinate for logging area
    private final int startZ; // Starting Z coordinate for logging area
    private final String logFilePath;
    private final int LOG_AREA_RADIUS = 32; // Logs a 64x64 area around the center (32 blocks in each direction)
    private final long LOG_INTERVAL_MS = 200; // How often to log data (milliseconds)

    private BufferedWriter writer;
    private Thread loggingThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Constructs a new NoiseLogger.
     *
     * @param worldSeed The seed of the world for which to log noise data.
     * @param centerX The center X coordinate of the area to log.
     * @param centerZ The center Z coordinate of the area to log.
     * @param logFilePath The full path including filename where the CSV log should be saved.
     * @throws IOException If the log file cannot be opened or created.
     */
    public NoiseLogger(long worldSeed, int centerX, int centerZ, String logFilePath) throws IOException {
        this.worldSeed = worldSeed;
        this.startX = centerX - LOG_AREA_RADIUS;
        this.startZ = centerZ - LOG_AREA_RADIUS;
        this.logFilePath = logFilePath;

        File logFile = new File(logFilePath);
        File parentDir = logFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // Create parent directories if they don't exist
        }

        // Open in append mode, but write header only if file is new/empty
        boolean fileExists = logFile.exists() && logFile.length() > 0;
        this.writer = new BufferedWriter(new FileWriter(logFile, true)); // 'true' for append mode

        if (!fileExists) {
            writer.write("X,Z,Continentalness,Climate,Biome\n");
            writer.flush();
        }
    }

    /**
     * Starts the logging process in a new thread.
     */
    public void startLogging() {
        if (!running.get()) {
            running.set(true);
            loggingThread = new Thread(this, "NoiseLoggerThread");
            loggingThread.start();
            LOGGER.info("Noise logging started to: " + logFilePath);
        }
    }

    /**
     * Stops the logging process.
     */
    public void stopLogging() {
        if (running.getAndSet(false)) { // Set running to false and return previous value
            if (loggingThread != null) {
                try {
                    loggingThread.join(5000); // Wait for the thread to finish gracefully (max 5 seconds)
                    if (loggingThread.isAlive()) {
                        LOGGER.warning("NoiseLoggerThread did not terminate gracefully.");
                        // Consider loggingThread.interrupt() if it's critical, but can cause issues with I/O
                    }
                } catch (InterruptedException e) {
                    LOGGER.log(Level.WARNING, "NoiseLoggerThread interrupted while stopping.", e);
                    Thread.currentThread().interrupt(); // Restore interrupt status
                }
            }
            try {
                if (writer != null) {
                    writer.close();
                    LOGGER.info("Noise log file closed.");
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error closing noise log file.", e);
            }
        }
    }

    /**
     * The main run method for the logging thread.
     * It continuously samples noise data and writes it to the CSV.
     */
    @Override
    public void run() {
        int currentX = startX;
        int currentZ = startZ;
        int maxDim = LOG_AREA_RADIUS * 2; // e.g., 64 for a 64x64 area

        try {
            while (running.get()) {
                // Log data for the current block
                double continentalness = ContinentalGenerationHandler.generateContinentalness(worldSeed, currentX, currentZ);
                double climate = ClimateGenerationHandler.generateClimate(worldSeed, currentX, currentZ);
                Biome biome = ProtoBiomeAssignment.getBiomeAt(worldSeed, currentX, currentZ, continentalness, climate);

                writer.write(String.format("%d,%d,%.4f,%.4f,%s\n",
                        currentX,
                        currentZ,
                        continentalness,
                        climate,
                        biome.name()));
                writer.flush(); // Flush frequently to ensure data is written

                // Move to the next block in the logging area
                currentX++;
                if (currentX >= startX + maxDim) {
                    currentX = startX;
                    currentZ++;
                    if (currentZ >= startZ + maxDim) {
                        currentZ = startZ; // Reset to start of area after one full pass
                    }
                }

                // Pause to prevent excessive CPU usage
                Thread.sleep(LOG_INTERVAL_MS);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error writing to noise log file, stopping logging.", e);
            running.set(false); // Stop logging on IO error
        } catch (InterruptedException e) {
            LOGGER.info("NoiseLoggerThread interrupted, stopping.");
            Thread.currentThread().interrupt(); // Restore interrupt status
            running.set(false); // Stop logging
        } finally {
            // Ensure writer is closed even if an exception occurs
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error closing noise log file in finally block.", e);
            }
        }
    }

    public String getLogFilePath() {
        return logFilePath;
    }
}