package org.byeautumn.chuachua.scene;

import org.byeautumn.chuachua.generate.world.BiomeGenerator;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class SceneMapGenerator {

    public static void main(String[] args) {
        int worldWidth = 256;
        int worldDepth = 256;
        long seed = 9876;
        BiomeGenerator biomeGenerator = new BiomeGenerator(seed);

        BufferedImage image = new BufferedImage(worldWidth, worldDepth, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < worldWidth; x++) {
            for (int y = 0; y < worldDepth; y++) {
                Color color = biomeGenerator.getBiomeColor(x, y);
                image.setRGB(x, y, color.getRGB());
                System.out.println("x: " + x + ", y: " + y + ", color: " + color.toString()); //Debugging
            }
        }

        try {
            File ioDir = new File("/Users/alexgao/dev/minecraft/minecraft_spigot_server_1.21.4/io");
            File imageFile = new File(ioDir, "scene_map_" + seed + ".png");
            ImageIO.write(image, "PNG", imageFile);
            System.out.println("Scene map image generated with seed: " + seed);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }
}