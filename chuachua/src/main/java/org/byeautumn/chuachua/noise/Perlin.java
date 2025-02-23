package org.byeautumn.chuachua.noise;

import org.byeautumn.chuachua.io.IOUntil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class Perlin {

    private final Random random;
    private final long seed;

    public Perlin(long seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    private double perlin(double x, double y) {
        double x0 = Math.floor(x);
        double y0 = Math.floor(y);
        double x1 = Math.ceil(x);
        double y1 = Math.ceil(y);

        double sx = x - x0;
        double sy = y - y0;

        double n00 = dotGridGradient(x0, y0, x, y);
        double n10 = dotGridGradient(x1, y0, x, y);
        double ix0 = interpolate(n00, n10, sx);

        double n01 = dotGridGradient(x0, y1, x, y);
        double n11 = dotGridGradient(x1, y1, x, y);
        double ix1 = interpolate(n01, n11, sx);

        return interpolate(ix0, ix1, sy);
    }

    private double dotGridGradient(double ix, double iy, double x, double y) {
        Vector2 gradient = randomGradient(ix, iy);
        double dx = x - (double) ix;
        double dy = y - (double) iy;
        return (dx * gradient.x + dy * gradient.y);
    }

    private Vector2 randomGradient(double ix, double iy) {
        final int w = 8 * Long.BYTES;
        final int s = w / 2;
        long a = (long)ix;
        long b = (long)iy;

        a *= 3284157443L;
        b ^= (a << s | a >>> w - s);
        b *= 1911520717L;
        a ^= (b << s | b >>> w - s);
        a *= 2048419325L;

        int index = (int) ((a ^ b) % 1024);
        random.setSeed(this.seed + index);

        double randomValue = random.nextDouble();

        Vector2 v = new Vector2(0, 0);
        v.x = Math.sin(randomValue * 2 * Math.PI);
        v.y = Math.cos(randomValue * 2 * Math.PI);

        return v;
    }

    private double interpolate(double a0, double a1, double w) {
        return (a1 - a0) * (3 - w * 2) * w * w + a0;
    }

    private double layeredPerlin(double x, double z, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;

        for (int i = 0; i < octaves; i++) {
            total += perlin(x * frequency, z * frequency) * amplitude;
            frequency *= 2;
            amplitude *= persistence;
        }
        return total;
    }

    public int getHeight(double x, double z) {
        int minHeight = 64;
        int maxHeight = 256;
        int octaves = 8;
        double persistence = 0.5d;

        double perlinValue = layeredPerlin(x, z, octaves, persistence);
        double normalizedPerlin = (perlinValue + 1.0f) / 2.0d;
        int height = (int) (normalizedPerlin * (maxHeight - minHeight) + minHeight);
        height = Math.max(minHeight, Math.min(maxHeight, height));

        return height;
    }

    public static void main(String[] args) {
        int worldWidth = 256;
        int worldDepth = 256;
        long seed = 8282011;
        Perlin perlinClass = new Perlin(seed);
        double scale = 0.001d;

        StringBuffer sb = new StringBuffer();
        for (int x1 = 0; x1 < worldWidth; x1++) {
            for (int z1 = 0; z1 < worldDepth; z1++) {

                int y1 = perlinClass.getHeight((double)x1 * scale, (double)z1 * scale);
                sb.append(x1).append(IOUntil.CC_SPLITTER).append(y1).append(IOUntil.CC_SPLITTER).append(z1);
                sb.append(IOUntil.CC_SPLITTER).append("GRASS_BLOCK");
                sb.append(IOUntil.CC_SPLITTER).append("minecraft:grass_block").append("\n");
            }
        }
//
        File ioDir = new File("/Users/alexgao/dev/minecraft/minecraft_spigot_server_1.21.4/io");
        IOUntil.saveExportIntoAIOFile(ioDir, "try_perlin" + seed, sb.toString());


        double[][] noiseValues = new double[worldWidth][worldDepth];
        for (int xx = 0; xx < worldWidth; xx++) {
            for (int zz = 0; zz < worldDepth; zz++) {
                int yy = perlinClass.getHeight((double) xx * scale, (double) zz * scale);
                noiseValues[xx][zz] = yy;
            }
        }

        try {
            File imageFile = new File(ioDir, "perlin_noise_" + seed + ".png");
            NoiseImageViewer viewer = new NoiseImageViewer();
            BufferedImage image = viewer.createGreyScaleImage(worldWidth, worldDepth, noiseValues);
            ImageIO.write(image, "PNG", imageFile);
            System.out.println("Perlin noise image generated with seed: " + seed);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }
}