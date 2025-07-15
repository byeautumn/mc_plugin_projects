package org.byeautumn.chuachua.generate.world.pipeline;

import java.util.logging.Logger;
import java.util.logging.Level;

public class GenerationContext {

    private final Logger logger;

    private static final int CHUNK_SIZE = 16;
    private static final int MAP_ARRAY_BORDER = 1;
    static final int MAP_ARRAY_DIM = CHUNK_SIZE + (2 * MAP_ARRAY_BORDER);

    public double[][] regionBlendMap;
    public double[][] heightmap;

    public GenerationContext(Logger parentLogger) {
        this.logger = Logger.getLogger(parentLogger.getName() + ".ChunkGenContext");
        this.logger.setLevel(Level.INFO); // Set to INFO for general logging, FINE for detailed
        this.logger.info("GenerationContext: Initializing for new chunk. Map dimensions: " + MAP_ARRAY_DIM + "x" + MAP_ARRAY_DIM);

        this.regionBlendMap = new double[MAP_ARRAY_DIM][MAP_ARRAY_DIM];
        this.heightmap = new double[MAP_ARRAY_DIM][MAP_ARRAY_DIM];
        this.logger.info("GenerationContext: regionBlendMap and heightmap arrays initialized.");
    }

    public Logger getLogger() {
        return logger;
    }
}