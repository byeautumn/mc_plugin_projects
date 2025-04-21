package org.byeautumn.chuachua.generate.world.pipeline;

import org.byeautumn.chuachua.noise.NoiseImageViewer;
import org.byeautumn.chuachua.noise.Perlin;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class HeightMapMask {

    private final long seed;
    private final int width;
    private final int depth;
    private final float scale;
    private final int octaves;
    private final float persistence;

    private double[][] maskValues;

    public HeightMapMask(long seed, int width, int depth, float scale, int octaves, float persistence) {
        this.seed = seed;
        this.width = width;
        this.depth = depth;
        this.scale = scale;
        this.octaves = octaves;
        this.persistence = persistence;
        this.maskValues = generateMask();
    }

    public double[][] getMaskValues() {
        return maskValues;
    }

    public float getScale() {
        return scale;
    }

    public int getOctaves() {
        return octaves;
    }

    public float getPersistence() {
        return persistence;
    }

    private double[][] generateMask() {
        Perlin perlin = new Perlin(seed);
        double[][] mask = new double[width][depth];

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                float perlinValue = perlin.layeredPerlin((float) x * scale, (float) z * scale, octaves, persistence);
                float normalizedValue = (perlinValue + 1.0f) / 2.0f;
                System.out.println("normalizedValue: '" + normalizedValue + "'.");
                mask[x][z] = normalizedValue;
            }
        }
        return mask;
    }

    public void saveMaskImage(String filePath) {
        try {
            NoiseImageViewer viewer = new NoiseImageViewer();
            BufferedImage image = viewer.createGreyScaleImage(width, depth, maskValues);
            File imageFile = new File(filePath);
            ImageIO.write(image, "PNG", imageFile);
            System.out.println("Heightmap mask image generated: " + filePath);
        } catch (IOException ioe) {
            System.out.println("Error saving heightmap mask image: " + ioe.getMessage());
        }
    }

    public static void main(String[] args) {
        long seed = 8282011L;
        int width = 256;
        int depth = 256;
        float scale = 0.01f;
        int octaves = 4;
        float persistence = 0.5f;
        String savePath = "/Users/alexgao/dev/minecraft/minecraft_spigot_server_1.21.4/io/heightmap_mask_" + seed + ".png";

        HeightMapMask maskGenerator = new HeightMapMask(seed, width, depth, scale, octaves, persistence);
        maskGenerator.saveMaskImage(savePath);

        double[][] mask = maskGenerator.getMaskValues();
        System.out.println("Heightmap mask generated with dimensions: " + mask.length + "x" + mask[0].length);
    }
}