package org.byeautumn.chuachua.noise;

import java.awt.image.BufferedImage;

public class NoiseImageViewer {

    public BufferedImage createGreyScaleImage(int width, int height, float[][] noiseValues) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        float[][] normalized = normalizeNoiseValues(noiseValues);
        // Set the pixel values based on the Perlin noise
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Normalize the Perlin noise value to 0-255 (grayscale range)
                int grayValue = (int) (normalized[x][y] * 255);  // Assuming noiseValues are between 0 and 1
                grayValue = Math.max(0, Math.min(255, grayValue)); // Clamp to 0-255

                // Set the pixel color (grayscale)
                image.setRGB(x, y, (grayValue << 16) | (grayValue << 8) | grayValue); // Efficient grayscale
            }
        }

        return image;
    }

    private float[][] normalizeNoiseValues(float[][] noiseValues) {
        if (null == noiseValues || noiseValues.length < 1) {
            System.out.println("The input matrix is either null or empty. Skip normalization ...");
            return noiseValues;
        }
        int rows = noiseValues.length;
        int cols = noiseValues[0].length;
        if (cols < 1) {
            System.out.println("The input matrix is empty. Skip normalization ...");
            return noiseValues;
        }

        float[][] normalized = new float[rows][cols];
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int xx = 0; xx < rows; ++xx) {
            for (int yy = 0; yy < cols; ++yy) {
                min = Math.min(min, noiseValues[xx][yy]);
                max = Math.max(max, noiseValues[xx][yy]);
            }
        }

        float scale = max - min;

        for (int xx = 0; xx < rows; ++xx) {
            for (int yy = 0; yy < cols; ++yy) {
                float value = noiseValues[xx][yy];
                normalized[xx][yy] = (value - min) / scale;
            }
        }

        return normalized;
    }
}
