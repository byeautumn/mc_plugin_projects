package org.byeautumn.chuachua.scene;

import org.byeautumn.chuachua.noise.Perlin;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class SceneMapGenerator {

    public static void main(String[] args) {
        int worldWidth = 256;
        int worldDepth = 256;
        long seed = 1; // Default seed, can be changed
        Perlin perlinClass = new Perlin(seed);

        double[][] noiseValues = new double[worldWidth][worldDepth];

        for (int x = 0; x < worldWidth; x++) {
            for (int y = 0; y < worldDepth; y++) {
                float temperature = getTemperature(x, y, perlinClass);
                float altitude = getAltitude(x, y, perlinClass);
                float hydration = getHydration(x, y, perlinClass);

                BiomeType biome = getBiomeFromFactors(temperature, altitude, hydration, x, y, worldWidth, worldDepth, perlinClass);
                Color color = getBiomeColor(biome, x, y, perlinClass);

                noiseValues[x][y] = color.getRGB(); // Store color as RGB int

                // Debugging output:
                System.out.println("x: " + x + ", y: " + y + ", temp: " + temperature + ", alt: " + altitude + ", hyd: " + hydration + ", biome: " + biome);
            }
        }

        try {
            File ioDir = new File("/Users/qiangao/dev/own/minecraft_spigot_server_1.21.4/io");
            File imageFile = new File(ioDir, "scene_map_" + seed + ".png");
            NoiseImageViewer viewer = new NoiseImageViewer();
            BufferedImage image = viewer.createRGBImage(worldWidth, worldDepth, noiseValues); // RGB image
            BufferedImage blurredImage = viewer.blurImage(image, 0);
            ImageIO.write(blurredImage, "PNG", imageFile);
            System.out.println("Scene map image generated with seed: " + seed);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    private static float getTemperature(int x, int y, Perlin perlin) {
        float scale = 0.001f; // Adjust as needed
        return perlin.perlin(x * scale, y * scale);
    }

    private static float getAltitude(int x, int y, Perlin perlin) {
        int minHeight = 0;
        int maxHeight = 100;
        int octaves = 6;
        float persistence = 0.5f;

        float scale = 0.01f; // Adjust scale as needed

        float floatX = (float) x * scale;
        float floatY = (float) y * scale;

        float height = perlin.getHeight(floatX, floatY, minHeight, maxHeight, octaves, persistence);

        System.out.println("Debug: x=" + floatX + ", y=" + floatY + ", height=" + height);
        System.out.println("Debug: height before min/max application = " + perlin.getHeight(floatX, floatY, 0, 1, octaves, persistence));

        return height;
    }

    private static float getHydration(int x, int y, Perlin perlin) {
        float scale = 0.002f; // Adjust as needed
        return perlin.perlin(x * scale, y * scale);
    }

    private static BiomeType getBiomeFromFactors(float temperature, float altitude, float hydration, int x, int y, int width, int height, Perlin perlin) {
        BiomeType biome = BiomeType.PLAINS;


        // Biome Noise (Influence biome placement)
        float biomeNoise = perlin.perlin(x * 0.03f, y * 0.03f); // Increased scale
        float biomeNoiseThreshold = -0.2f; // Reduced threshold

        // Edge Noise (Influence biome edges)
        float edgeNoise = perlin.perlin(x * 0.1f, y * 0.1f); // Increased scale
        float edgeNoiseThreshold = -0.4f; // Reduced threshold

        if (altitude > 50.0f) {
            biome = BiomeType.SNOWY_MOUNTAINS;
        } else if (hydration < -0.085f) {
            biome = BiomeType.DESERT;
        } else if (hydration > -0.065f && temperature > -0.035f) {
            biome = BiomeType.TROPICAL_RAINFOREST;
        } else if (temperature > -0.055f && hydration > -0.075f) {
            biome = BiomeType.FOREST;
        } else if (temperature < -0.057f && altitude > 27.5f) {
            biome = BiomeType.TUNDRA; // Force Tundra
        } if (temperature < -0.057f && altitude > 27.5f && hydration < -0.081f) {
            biome = BiomeType.BARREN_WASTELAND; // Force Barren Wasteland
        }

        // Refined Blending Logic and Thresholds
        float blendFactor = 0.6f; // Adjust for smoother transitions

        // WOODLAND (Forest - Plains transition)
        if (temperature > -0.065f && temperature < -0.055f && hydration > -0.080f) {
            float blendedTemp = temperature * blendFactor + (-0.055f) * (1 - blendFactor);
            float blendedHyd = hydration * blendFactor + (-0.075f) * (1 - blendFactor);
            if (blendedTemp > -0.062f && blendedHyd > -0.077f && edgeNoise > edgeNoiseThreshold) {
                biome = BiomeType.WOODLAND;
            }
        }

        // SAVANNA (Plains - Desert transition)
        if (hydration < -0.080f && hydration > -0.085f && altitude < 30.0f) {
            float blendedHyd = hydration * blendFactor + (-0.085f) * (1 - blendFactor);
            if (blendedHyd < -0.083f && edgeNoise > edgeNoiseThreshold) {
                biome = BiomeType.SAVANNA;
            }
        }

        // BARREN_WASTELAND (Desert - Tundra transition)
        if (hydration < -0.082f && temperature < -0.058f && altitude > 28.0f && edgeNoise > -0.2f) { // Adjusted threshold
            biome = BiomeType.BARREN_WASTELAND;
        }

        // TAIGA (Tundra - Forest/Plains transition)
        if (temperature < -0.055f && temperature > -0.06f && altitude > 29.0f && altitude < 31.0f) {
            float blendedTemp = temperature * blendFactor + (-0.06f) * (1 - blendFactor);
            float blendedAlt = altitude * blendFactor + (30.0f) * (1 - blendFactor);
            if (blendedTemp < -0.059f && blendedAlt > 29.7f && edgeNoise > edgeNoiseThreshold) {
                biome = BiomeType.TAIGA;
            }
        }

        // TUNDRA (Gradual Biome Assignment)
        if (temperature < -0.056f && altitude > 27.0f) {
            float tundraBlend = (temperature - (-0.06f)) / (-0.056f - (-0.06f)) * (altitude - 30.0f) / (27.0f - 30.0f);
            tundraBlend = Math.max(0, Math.min(1, tundraBlend));

            if (biomeNoise > -0.1f && edgeNoise > -0.2f) { // Adjusted threshold
                biome = BiomeType.TUNDRA;
            }
        }

        return biome;
    }

    private static Color getBiomeColor(BiomeType biome, int x, int y, Perlin perlin) {
        switch (biome) {
            case SNOWY_MOUNTAINS:
                return new Color(220, 230, 255);
            case TUNDRA:
                return getTundraColor(x, y, perlin);
            case DESERT:
                return new Color(250, 240, 200);
            case TROPICAL_RAINFOREST:
                return new Color(0, 120, 0);
            case FOREST: return new Color(0, 160, 0);
            case PLAINS:
                return new Color(160, 210, 120);
            case WOODLAND:
                return new Color(120, 190, 120);
            case SAVANNA:
                return new Color(230, 220, 180);
            case BARREN_WASTELAND:
                return new Color(200, 200, 200);
            case TAIGA:
                return new Color(140, 170, 140);
            default:
                return Color.WHITE;
        }
    }

    private static Color getTundraColor(int x, int y, Perlin perlin) {
        // Return a constant gray color
        return new Color(180, 180, 180);
    }

    private enum BiomeType {
        SNOWY_MOUNTAINS, TUNDRA, DESERT, TROPICAL_RAINFOREST, FOREST, PLAINS, WOODLAND, SAVANNA, BARREN_WASTELAND, TAIGA
    }

    // Helper class for creating RGB images
    static class NoiseImageViewer {
        public BufferedImage createRGBImage(int width, int height, double[][] noiseValues) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, (int) noiseValues[x][y]);
                }
            }
            return image;
        }

        public BufferedImage blurImage(BufferedImage image, int radius) {
            int width = image.getWidth();
            int height = image.getHeight();
            BufferedImage blurredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int redSum = 0, greenSum = 0, blueSum = 0, count = 0;
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dy = -radius; dy <= radius; dy++) {
                            int nx = x + dx, ny = y + dy;
                            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                Color color = new Color(image.getRGB(nx, ny));
                                redSum += color.getRed();
                                greenSum += color.getGreen();
                                blueSum += color.getBlue();
                                count++;
                            }
                        }
                    }
                    if (count > 0) {
                        blurredImage.setRGB(x, y, new Color(redSum / count, greenSum / count, blueSum / count).getRGB());
                    } else {
                        blurredImage.setRGB(x, y, image.getRGB(x, y));
                    }
                }
            }
            return blurredImage;
        }
    }
}