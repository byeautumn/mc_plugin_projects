package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.block.Biome; // Import Bukkit's Biome enum
import java.util.logging.Logger;
import java.util.logging.Level;

// Import static constant for map dimensions from ProtoTerrainGeneration
import static org.byeautumn.chuachua.generate.world.pipeline.ProtoTerrainGeneration.MAP_ARRAY_DIM; // Ensure this import is present

public class GenerationContext {

    private final Logger logger;
    private final int chunkX;
    private final int chunkZ;

    public double[][] regionBlendMap;
    public double[][] heightmap;
    public Biome[][] biomeMap;
    public double[][] continentalnessMap; // Stores continentalness values

    /**
     * Constructor for GenerationContext.
     * Initializes all internal maps to the correct dimensions (MAP_ARRAY_DIM x MAP_ARRAY_DIM).
     *
     * @param chunkX The X coordinate of the chunk this context is for.
     * @param chunkZ The Z coordinate of the chunk this context is for.
     */
    public GenerationContext(int chunkX, int chunkZ) { // Simplified constructor
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        // Logger for this context instance
        this.logger = Logger.getLogger(GenerationContext.class.getName());
        this.logger.setLevel(Level.INFO);
//        this.logger.info("GenerationContext: Initializing for chunk (" + chunkX + ", " + chunkZ + "). Map dimensions: " + MAP_ARRAY_DIM + "x" + MAP_ARRAY_DIM);

        // Initialize all maps using the shared MAP_ARRAY_DIM constant
        this.regionBlendMap = new double[MAP_ARRAY_DIM][MAP_ARRAY_DIM];
        this.heightmap = new double[MAP_ARRAY_DIM][MAP_ARRAY_DIM];
        this.biomeMap = new Biome[MAP_ARRAY_DIM][MAP_ARRAY_DIM];
        this.continentalnessMap = new double[MAP_ARRAY_DIM][MAP_ARRAY_DIM];

//        this.logger.info("GenerationContext: regionBlendMap, heightmap, biomeMap, and continentalnessMap arrays initialized.");

        // Default map values (these will be overwritten by pipeline stages)
        for (int x = 0; x < MAP_ARRAY_DIM; x++) {
            for (int z = 0; z < MAP_ARRAY_DIM; z++) {
                this.regionBlendMap[x][z] = 0.5; // Default neutral blend
                this.biomeMap[x][z] = Biome.PLAINS; // Default biome
                this.continentalnessMap[x][z] = 0.4; // Default continentalness
            }
        }
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public Logger getLogger() {
        return logger;
    }
}