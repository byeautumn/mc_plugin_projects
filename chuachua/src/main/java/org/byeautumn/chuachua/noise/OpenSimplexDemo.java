package org.byeautumn.chuachua.noise;

import org.byeautumn.chuachua.io.IOUntil;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;

public class OpenSimplexDemo
{
    private static final long SEED = 18;
    private static final int WIDTH = 512;
    private static final int HEIGHT = 512;
    private static final double FREQUENCY = 0.005f;

    public static void main(String[] args)
            throws IOException {

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double value = OpenSimplex2S.noise3_ImproveXY(SEED, x * FREQUENCY, y * FREQUENCY, 0.0);
//                SimplexUsageOctaves simplexUsageOctaves = new SimplexUsageOctaves(8,0.5f, 2f, FREQUENCY, SEED, -1, 1,2f, 4);
//                double value = simplexUsageOctaves.octaveSimplex(x,y,0 );
                int rgb = 0x010101 * (int) ((value + 1) * 127.5);
                image.setRGB(x, y, rgb);
            }
        }
        File ioDir = new File("/Users/alexgao/dev/minecraft/minecraft_spigot_server_1.21.4/io");
        try {
            File imageFile = new File(ioDir, "simplex_noise_'" + SEED + "'.png");
            ImageIO.write(image, "PNG", imageFile);
            System.out.println("Perlin noise image generated with seed: " + SEED);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
//        ImageIO.write(image, "png", new File("noise.png"));
        }
    }
}
