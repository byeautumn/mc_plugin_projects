package org.byeautumn.chuachua.noise;

import org.bukkit.Material;
import org.byeautumn.chuachua.io.ChunkExporter;
import org.byeautumn.chuachua.io.IOUntil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class Perlin {
    public float perlin(float x, float y){
        //Determine grid cell coordinates.
//        System.out.println("Perlin(x=" + x + ", y=" + y + ")");
        int x0 = (int)x;
        int y0 = (int)y;
        int x1 = x0 + 1;
        int y1 = y0 + 1;
//        System.out.println("Perlin(x0=" + x0 + "x1=" + x1 + ", y0=" + y0 + "y1=" + y1 + ")");

        //Define interpolation weights.
        float sx = x - (float) x0;
        float sy = y - (float) y0;
//        System.out.println("Perlin(sx=" + sx + ", sy=" + sy + ")." );

        float n00 = dotGridGradient(x0, y0, x, y);
        float n10 = dotGridGradient(x1, y0, x, y );
        float ix0 = interpolate(n00, n10, sx);

        float n01 = dotGridGradient(x0, y1, x, y);
        float n11 = dotGridGradient(x1, y1, x, y);
        float ix1 = interpolate(n01, n11, sx);
//        System.out.println("Perlin(x=" + x + ", y=" + y + ") = " + interpolate(ix0, ix1, sy));

        return interpolate(ix0, ix1, sy);
    }

    private float dotGridGradient(int ix, int iy, float x, float y){
         Vector2 gradient = randomGradient(ix, iy);

         float dx = x - (float)ix;
         float dy = y - (float) iy;

        float result = (dx * gradient.x + dy * gradient.y);

//        System.out.println("dotGridGradient(ix=" + ix + ", iy=" + iy + ", x=" + x + ", y=" + y + ") = " + result);
//        System.out.println("  Gradient: (" + gradient.x + ", " + gradient.y + "), dx: " + dx + ", dy: " + dy);

        return result;

    }

    public static Vector2 randomGradient(int ix, int iy) {
        final int w = 8 * Long.BYTES;
        final int s = w / 2;
        long a = ix;
        long b = iy;

        a *= 3284157443L;
        b ^= (a << s | a >>> w - s);
        b *= 1911520717L;
        a ^= (b << s | b >>> w - s);
        a *= 2048419325L;

        double random = (double) a * (Math.PI / (Long.MAX_VALUE * 2.0 + 1.0));

        Vector2 v = new Vector2(0, 0);
        v.x = (float) Math.sin(random); // Cast to float
        v.y = (float) Math.cos(random); // Cast to float

        return v;
    }

    private float interpolate(float a0, float a1, float w){
        float result = (a1 - a0) * (3 - w * 2) * w * w + a0;
//        System.out.println("interpolate(a0=" + a0 + ", a1=" + a1 + ", w=" + w + ") = " + result);
        return result;
    }

    public float layeredPerlin(float x, float z, int octaves, float persistence) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;

        for (int i = 0; i < octaves; i++) {
            total += perlin(x * frequency, z * frequency) * amplitude; // Use your perlin function here
//            System.out.println("Octave " + i + ", Total: " + total);

            frequency *= 2;
            amplitude *= persistence;
        }
        return total;
    }

    public int getHeight(float x, float z) {
//        System.out.println("getHeight(x=" + x + "z=" + z + ")");
        int minHeight = 64;
        int maxHeight = 256;
        int octaves = 8;
        float persistence = 0.5f;

        float perlinValue = layeredPerlin(x, z, octaves, persistence);

        // Scale from -1 to 1 to 0 to 1
        float normalizedPerlin = (perlinValue + 1.0f) / 2.0f;

        // Scale from 0 to 1 to minHeight to maxHeight
        int height = (int) (normalizedPerlin * (maxHeight - minHeight) + minHeight);

        height = Math.max(minHeight, Math.min(maxHeight, height));

        return height;
    }

    public static void main(String[] args) {
        int worldWidth = 10;
        int worldDepth = 10;
        Perlin perlinClass = new Perlin();
//        StringBuffer sb = new StringBuffer();
        float scale = 0.01f; // Adjust this scale as needed
//        for (int x1 = 0; x1 < worldWidth; x1++) {
//            for (int z1 = 0; z1 < worldDepth; z1++) {
//
//                int y1 = perlinClass.getHeight((float)x1 * scale, (float)z1 * scale);
//                sb.append(x1).append(IOUntil.CC_SPLITTER).append(y1).append(IOUntil.CC_SPLITTER).append(z1);
//                sb.append(IOUntil.CC_SPLITTER).append("GRASS_BLOCK");
//                sb.append(IOUntil.CC_SPLITTER).append("minecraft:grass_block").append("\n");
//            }
//        }
//
        File ioDir = new File("/Users/alexgao/dev/minecraft/minecraft_spigot_server_1.21.4/io");
//        IOUntil.saveExportIntoAIOFile(ioDir, "try_perlin", sb.toString());

        float[][] noiseValues = new float[worldWidth][worldDepth];
        for (int xx = 0; xx < worldWidth; xx++) {
            for (int zz = 0; zz < worldDepth; zz++) {

                int yy = perlinClass.getHeight((float)xx * scale, (float)zz * scale);
//                System.out.println("****** yy: " + yy + " ******");
                noiseValues[xx][zz] = yy;
            }
        }
        try {
            File imageFile = new File(ioDir, "perlin_noise.png");
            NoiseImageViewer viewer = new NoiseImageViewer();
            BufferedImage image = viewer.createGreyScaleImage(worldWidth, worldDepth, noiseValues);
            ImageIO.write(image, "PNG", imageFile);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }

    }
}
