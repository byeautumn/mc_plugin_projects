package org.byeautumn.chuachua.noise;

import org.bukkit.Material;
import org.byeautumn.chuachua.io.ChunkExporter;
import org.byeautumn.chuachua.io.IOUntil;

import java.io.File;
import java.util.Locale;

public class Perlin {
    public float perlin(float x, float y){
        //Determine grid cell coordinates.
        int x0 = (int)x;
        int y0 = (int)y;
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        //Define interpolation weights.
        float sx = x -(float)x0;
        float sy = y -(float)y0;

        float n00 = dotGridGradient(x0, y0, x, y);
        float n10 = dotGridGradient(x1, y0, x, y );
        float ix0 = interpolate(n00, n10, sx);

        float n01 = dotGridGradient(x0, y1, x, y);
        float n11 = dotGridGradient(x1, y1, x, y);
        float ix1 = interpolate(n01, n11, sx);

        return interpolate(ix0, ix1, sy);
    }

    private float dotGridGradient(int ix, int iy, float x, float y){
         Vector2 gradient = randomGradient(ix, iy);

         float dx = x - (float)ix;
         float dy = y - (float) iy;

         return(dx * gradient.x + dy *gradient.y);
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
        return(a1 - a0) * (3 - w * 2) * w * w + a0;
    }

    public float layeredPerlin(float x, float z, int octaves, float persistence) {
        float total = 0;
        float frequency = 1;
        float amplitude = 1;

        for (int i = 0; i < octaves; i++) {
            total += perlin(x * frequency, z * frequency) * amplitude; // Use your perlin function here

            frequency *= 2;
            amplitude *= persistence;
        }
        return total;
    }

    public int getHeight(float x, float z) {
        int minHeight = 64;
        int maxHeight = 256;
        int octaves = 8; // Adjust as needed
        float persistence = 0.5f; // Adjust as needed

        float perlinValue = layeredPerlin(x, z, octaves, persistence);

        // Scale and translate the Perlin value to your height range:
        int height = (int) (perlinValue * (maxHeight - minHeight) + minHeight);

        // Clamp the height to the valid range:
        height = Math.max(minHeight, Math.min(maxHeight, height));

        return height;
    }

    public static void main(String[] args) {
        int worldWidth = 256;
        int worldDepth = 256;
        Perlin perlinClass = new Perlin();
        StringBuffer sb = new StringBuffer();
        for (int x1 = 0; x1 < worldWidth; x1++) {
            for (int z1 = 0; z1 < worldDepth; z1++) {

                int y1 = perlinClass.getHeight(x1, z1);

                sb.append(x1).append(IOUntil.CC_SPLITTER).append(y1).append(IOUntil.CC_SPLITTER).append(z1);
                sb.append(IOUntil.CC_SPLITTER).append("GRASS_BLOCK");
                sb.append(IOUntil.CC_SPLITTER).append("minecraft:grass_block").append("\n");
            }
        }

        File ioDir = new File("/Users/qiangao/dev/own/minecraft_spigot_server_1.21.4/io");
        IOUntil.saveExportIntoAIOFile(ioDir, "try_perlin", sb.toString());

    }
}
