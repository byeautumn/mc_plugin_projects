package org.byeautumn.chuachua.noise;
import org.byeautumn.chuachua.io.IOUntil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * This Perlin noise implementation will give the noise value between [-1, 1]
 */
public class Perlin2 {

    private final int[] permutation = new int[512];

    public Perlin2(long seed) {
        Random random = new Random(seed);

        // Initialize permutation array
        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        // Duplicate the permutation array to avoid index out of bounds
        System.arraycopy(permutation, 0, permutation, 256, 256);
    }

    public double noise(double x, double y, double z) {
        double X = (int) Math.floor(x) & 255;      // FIND INTEGER X
        double Y = (int) Math.floor(y) & 255;      // FIND INTEGER Y
        double Z = (int) Math.floor(z) & 255;      // FIND INTEGER Z

        x -= Math.floor(x);                             // FIND (FRACTIONAL) part of X
        y -= Math.floor(y);                             // FIND (FRACTIONAL) part of Y
        z -= Math.floor(z);                             // FIND (FRACTIONAL) part of Z

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);

        int A = permutation[(int)X] & 255;
        int AA = permutation[(int)(A + Y)] & 255;
        int AB = permutation[(int)(A + Y + 1)] & 255;
        int B = permutation[(int)(X + 1)] & 255;
        int BA = permutation[(int)(B + Y)] & 255;
        int BB = permutation[(int)(B + Y + 1)] & 255;

        double result = lerp(w, lerp(v, lerp(u, grad(permutation[AA], x, y, z),
                                grad(permutation[BA], x - 1, y, z)),
                        lerp(u, grad(permutation[AB], x, y - 1, z),
                                grad(permutation[BB], x - 1, y - 1, z))),
                lerp(v, lerp(u, grad(permutation[AA + 1], x, y, z - 1),
                                grad(permutation[BA + 1], x - 1, y, z - 1)),
                        lerp(u, grad(permutation[AB + 1], x, y - 1, z - 1),
                                grad(permutation[BB + 1], x - 1, y - 1, z - 1))));

        return result;
    }


    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double a, double b, double x) {
        return a + (b - a) * x;
    }

    private double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = (h < 8) ? x : y;
        double v = (h < 4) ? y : ((h == 12 || h == 14) ? x : z);
        return (((h & 1) == 0) ? u : -u) + (((h & 2) == 0) ? v : -v);
    }

    public static void main(String[] args) {
        int worldWidth = 256;
        int worldDepth = 256;
        long seed = 19771226;
        Perlin2 perlin2 = new Perlin2(seed);
        float scale = 0.001f;

        double[][] noiseValues = new double[worldWidth][worldDepth];
        for (int xx = 0; xx < worldWidth; xx++) {
            for (int zz = 0; zz < worldDepth; zz++) {
                double yy = perlin2.noise((double) xx * scale, (double) zz * scale, 0.0);
                noiseValues[xx][zz] = (yy + 1.0) / 2.0;
            }
        }

        try {
            File ioDir = new File("/Users/qiangao/dev/own/minecraft_spigot_server_1.21.4/io");
            File imageFile = new File(ioDir, "perlin2_noise_" + seed + ".png");
            NoiseImageViewer viewer = new NoiseImageViewer();
            BufferedImage image = viewer.createGreyScaleImage(worldWidth, worldDepth, noiseValues);
            ImageIO.write(image, "PNG", imageFile);
            System.out.println("Perlin noise image generated with seed: " + seed);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }
}
