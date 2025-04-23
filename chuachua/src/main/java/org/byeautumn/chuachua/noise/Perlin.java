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

    public float perlin(float x, float y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        float sx = x - (float) x0;
        float sy = y - (float) y0;

        float n00 = dotGridGradient(x0, y0, x, y);
        float n10 = dotGridGradient(x1, y0, x, y);
        float ix0 = interpolate(n00, n10, sx);

        float n01 = dotGridGradient(x0, y1, x, y);
        float n11 = dotGridGradient(x1, y1, x, y);
        float ix1 = interpolate(n01, n11, sx);

        return interpolate(ix0, ix1, sy);
    }

    private float dotGridGradient(int ix, int iy, float x, float y) {
        Vector2 gradient = randomGradient(ix, iy);
        float dx = x - (float) ix;
        float dy = y - (float) iy;
        return (dx * gradient.x + dy * gradient.y);
    }

    public Vector2 randomGradient(int ix, int iy) {
        final int w = 8 * Long.BYTES;
        final int s = w / 2;
        long a = ix;
        long b = iy;

        a *= 3284157443L;
        b ^= (a << s | a >>> w - s);
        b *= 1911520717L;
        a ^= (b << s | b >>> w - s);
        a *= 2048419325L;

        int index = (int) ((a ^ b) % 10240000);
        Random localRandom = new Random(seed + index);

        double randomValue = localRandom.nextDouble();

        Vector2 v = new Vector2(0, 0);
        v.x = (float) Math.sin(randomValue * 2 * Math.PI);
        v.y = (float) Math.cos(randomValue * 2 * Math.PI);

        return v;
    }

    private float interpolate(float a0, float a1, float w) {
        return (a1 - a0) * (3 - w * 2) * w * w + a0;
    }

    public float layeredPerlin(float x, float z, int octaves, float persistence) {
        float total = 0;
        float frequency = 1f;
        float amplitude = 3f;

        for (int i = 0; i < octaves; i++) {
            total += perlin(x * frequency, z * frequency) * amplitude;
            frequency *= 2.0f;
            amplitude *= persistence;
        }
        return total;
    }

    public int getHeight(float x, float z, int minHeight, int maxHeight, int octaves, float persistence) {

        float perlinValue = layeredPerlin(x, z, octaves, persistence);
        float normalizedPerlin = (perlinValue + 1.0f) / 2.0f;
        int height = (int) (normalizedPerlin * (maxHeight - minHeight) + minHeight);
        height = Math.max(minHeight, Math.min(maxHeight, height));

        return height;
    }

    public static void main(String[] args) {
        int worldWidth = 256;
        int worldDepth = 256;
        long seed = 8282011L;
        Perlin perlinClass = new Perlin(seed);
        float scale = 0.001f;

        StringBuffer sb = new StringBuffer();
        for (int x1 = 0; x1 < worldWidth; x1++) {
            for (int z1 = 0; z1 < worldDepth; z1++) {
                int y1 = perlinClass.getHeight((float)x1 * scale, (float)z1 * scale, 64, 256, 1, 0.0f);
                sb.append(x1).append(IOUntil.CC_SPLITTER).append(y1).append(IOUntil.CC_SPLITTER).append(z1);
                sb.append(IOUntil.CC_SPLITTER).append("GRASS_BLOCK");
                sb.append(IOUntil.CC_SPLITTER).append("minecraft:grass_block").append("\n");
            }
        }

        File ioDir = new File("/Users/alexgao/dev/minecraft/minecraft_spigot_server_1.21.4/io");
        IOUntil.saveExportIntoAIOFile(ioDir, "try_perlin" + seed, sb.toString());

        double[][] noiseValues = new double[worldWidth][worldDepth];
        for (int xx = 0; xx < worldWidth; xx++) {
            for (int zz = 0; zz < worldDepth; zz++) {
                int yy = perlinClass.getHeight((float) xx * scale, (float) zz * scale, 64, 256, 1, 0.0f);
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