package org.byeautumn.chuachua.generate.world;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

import java.awt.Color;
import java.util.Random;

public class BiomeWorldGenerator extends ChunkGenerator {

    private final BiomeGenerator biomeGenerator;

    public BiomeWorldGenerator(long seed) {
        this.biomeGenerator = new BiomeGenerator(seed);
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
        ChunkData chunkData = createChunkData(world);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;

                Color color = biomeGenerator.getBiomeColor(worldX, worldZ);
                Material concreteColor = colorToConcreteMaterial(color);
                int altitude = (int) biomeGenerator.getAltitude(worldX, worldZ); // get altitude without biome type.

                for (int y = 0; y < altitude; y++) {
                    chunkData.setBlock(x, y, z, concreteColor);
                }
                for (int y = altitude; y < 64; y++) {
                    chunkData.setBlock(x, y, z, Material.STONE);
                }
            }
        }
        return chunkData;
    }


    private Material colorToConcreteMaterial(Color color) {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();

        // Simple Color Matching Logic (Expand as needed)
        if (red > 200 && green > 200 && blue > 200) {
            return Material.WHITE_CONCRETE;
        } else if (red > 200 && green > 200) {
            return Material.YELLOW_CONCRETE;
        } else if (red > 200 && blue > 200) {
            return Material.MAGENTA_CONCRETE;
        } else if (green > 200 && blue > 200) {
            return Material.CYAN_CONCRETE;
        } else if (red > 200) {
            return Material.RED_CONCRETE;
        } else if (green > 200) {
            return Material.LIME_CONCRETE;
        } else if (blue > 200) {
            return Material.BLUE_CONCRETE;
        } else if (red > 100 && green > 100 && blue > 100) {
            return Material.LIGHT_GRAY_CONCRETE;
        } else if (red > 100 && green > 100) {
            return Material.ORANGE_CONCRETE;
        } else if (red > 100 && blue > 100) {
            return Material.PURPLE_CONCRETE;
        } else if (green > 100 && blue > 100) {
            return Material.LIGHT_BLUE_CONCRETE;
        } else if (red > 100) {
            return Material.BROWN_CONCRETE;
        } else if (green > 100) {
            return Material.GREEN_CONCRETE;
        } else if (blue > 100) {
            return Material.BLUE_CONCRETE;
        } else {
            return Material.GRAY_CONCRETE;
        }
    }
}