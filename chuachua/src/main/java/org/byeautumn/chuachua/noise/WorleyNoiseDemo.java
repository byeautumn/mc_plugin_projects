package org.byeautumn.chuachua.noise;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.byeautumn.chuachua.noise.WorleyNoise;
import org.byeautumn.chuachua.noise.WorleyNoise.FeatureType;

public class WorleyNoiseDemo {
    private static final long SEED = 10;
    private static final int WIDTH = 512;
    private static final int HEIGHT = 512;
    private static final double FREQUENCY = 0.0000005;

    public static void main(String[] args) throws IOException {

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        WorleyNoise worleyNoiseGenerator = new WorleyNoise(SEED);

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double value = worleyNoiseGenerator.noise2D(
                        x * FREQUENCY,
                        y * FREQUENCY,
                        FeatureType.F2_MINUS_F1
                );

                // --- NEW: Clamp the 'value' to ensure it stays within [-1.0, 1.0] ---
                value = Math.max(-1.0, Math.min(1.0, value));

                // Convert the clamped noise value to an RGB color for visualization.
                int rgb = 0x010101 * (int) ((value + 1) * 127.5);
                image.setRGB(x, y, rgb);
            }
        }

        File ioDir = new File("/Users/alexgao/dev/minecraft/minecraft_spigot_server_1.21.4/io");
        try {
            File imageFile = new File(ioDir, "worley_noise_'" + SEED + "'.png");
            ImageIO.write(image, "PNG", imageFile);
            System.out.println("Worley noise image generated with seed: " + SEED);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }
}