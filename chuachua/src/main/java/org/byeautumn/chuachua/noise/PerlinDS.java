package org.byeautumn.chuachua.noise;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class PerlinDS {
    private final int[] p = new int[512];

    public PerlinDS(long seed) {
        generatePermutation(seed);
    }

    private void generatePermutation(long seed) {
        Random random = new Random(seed);
        int[] permutation = new int[256];
        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }
        // Shuffle the array using the seed
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        // Copy the permutation array to the p array twice
        for (int i = 0; i < 256; i++) {
            p[256 + i] = p[i] = permutation[i];
        }
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y) {
        int h = hash & 15; // Convert low 4 bits of hash code into 12 gradient directions
        double grad = 1 + (h & 7); // Gradient value is one of 1, 2, ..., 8
        if ((h & 8) != 0) grad = -grad; // Randomly invert half of them
        return (h < 4) ? (grad * x) : (grad * y);
    }

    public double noise(double x, double y) {
        // Determine grid cell coordinates
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;

        // Relative x, y coordinates within the grid cell
        x -= Math.floor(x);
        y -= Math.floor(y);

        // Compute fade curves for x and y
        double u = fade(x);
        double v = fade(y);

        // Hash coordinates of the 4 cube corners
        int A = p[X] + Y;
        int AA = p[A];
        int AB = p[A + 1];
        int B = p[X + 1] + Y;
        int BA = p[B];
        int BB = p[B + 1];

        // Add blended results from 4 corners of the grid cell
        return lerp(v, lerp(u, grad(p[AA], x, y),
                        grad(p[BA], x - 1, y)),
                lerp(u, grad(p[AB], x, y - 1),
                        grad(p[BB], x - 1, y - 1)));
    }

    public static void main(String[] args) {
        int worldWidth = 1920;
        int worldDepth = 1920;
        long seed = 8282011;
        PerlinDS perlin = new PerlinDS(seed);
        double scale = 0.001d;

        double[][] noiseValues = new double[worldWidth][worldDepth];
        for (int xx = 0; xx < worldWidth; xx++) {
            for (int zz = 0; zz < worldDepth; zz++) {
//                double yy = perlin.octavedNoise((double) xx * scale, (double) zz * scale, octaves, persistence);
                double yy = perlin.noise(xx * scale, zz * scale);
                noiseValues[xx][zz] = (yy + 1.0) / 2.0;
            }
        }

        try {
            File ioDir = new File("/Users/qiangao/dev/own/minecraft_spigot_server_1.21.4/io");
            File imageFile = new File(ioDir, "perlinDS_noise_" + seed + ".png");
            NoiseImageViewer viewer = new NoiseImageViewer();
            BufferedImage image = viewer.createGreyScaleImage(worldWidth, worldDepth, noiseValues);
            ImageIO.write(image, "PNG", imageFile);
            System.out.println("Perlin noise image generated with seed: " + seed);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }
}
