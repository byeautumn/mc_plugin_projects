package org.byeautumn.chuachua.noise;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Range:
 * Let's say octaves = 8 and persistence = 0.5.
 * Max Potential Value = (1 - 0.5^8) / (1 - 0.5) ≈ 1.992
 * Min Potential Value = -1.992
 * So, the potential range is approximately -1.992 to 1.992.
 */
public class Perlin2D {

    private final int[] permutation;

    public Perlin2D(long seed) {
        Random random = new Random(seed);
        permutation = new int[512];

        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        System.arraycopy(permutation, 0, permutation, 256, 256); // Duplicate for wrapping
    }

    public double noise(double x, double y) {
        double X = ((int) Math.floor(x)) & 255;
        double Y = ((int) Math.floor(y)) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);

        double u = fade(x);
        double v = fade(y);

        int A = permutation[(int)X] & 255;
        int AA = permutation[(int)(A + Y)] & 255;
        int AB = permutation[(int)(A + Y + 1)] & 255;
        int B = permutation[(int)(X + 1)] & 255;
        int BA = permutation[(int)(B + Y)] & 255;
        int BB = permutation[(int)(B + Y + 1)] & 255;

        return lerp(v, lerp(u, grad(permutation[AA], x, y),
                        grad(permutation[BA], x - 1, y)),
                lerp(u, grad(permutation[AB], x, y - 1),
                        grad(permutation[BB], x - 1, y - 1)));
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double a, double b, double x) {
        return a + (b - a) * x;
    }

    private double grad(int hash, double x, double y) {
        int h = hash & 3; // Take the last 2 bits
        double u = (h < 2) ? x : y;
        double v = (h < 1) ? y : x;
        return (((h & 1) == 0) ? u : -u) + (((h & 2) == 0) ? v : -v);
    }


    public double octavedNoise(double x, double y, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;

        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;
            frequency *= 2;
            amplitude *= persistence;
        }
        return total;
    }

    public static void main(String[] args) {
        int worldWidth = 1920;
        int worldDepth = 1920;
        long seed = 8282011;
        Perlin2D perlin = new Perlin2D(seed);
        double scale = 0.0001d;
        double persistence = 0.8d;
        int octaves = 28;

        double[][] noiseValues = new double[worldWidth][worldDepth];
        for (int xx = 0; xx < worldWidth; xx++) {
            for (int zz = 0; zz < worldDepth; zz++) {
                double yy = perlin.octavedNoise((double) xx * scale, (double) zz * scale, octaves, persistence);
//                double yy = perlin.noise(xx * scale, zz * scale);
                noiseValues[xx][zz] = (yy + 2.0) / 4.0;
            }
        }

        try {
            File ioDir = new File("/Users/qiangao/dev/own/minecraft_spigot_server_1.21.4/io");
            File imageFile = new File(ioDir, "perlin2D_noise_" + seed + ".png");
            NoiseImageViewer viewer = new NoiseImageViewer();
            BufferedImage image = viewer.createGreyScaleImage(worldWidth, worldDepth, noiseValues);
            ImageIO.write(image, "PNG", imageFile);
            System.out.println("Perlin noise image generated with seed: " + seed);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }
}
