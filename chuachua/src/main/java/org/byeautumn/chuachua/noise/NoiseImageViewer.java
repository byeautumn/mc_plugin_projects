package org.byeautumn.chuachua.noise;

import java.awt.image.BufferedImage;

public class NoiseImageViewer {

    public BufferedImage createGreyScaleImage(int width, int height, double[][] noiseValues, int overlap) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        double[][] normalized = normalizeNoiseValues(noiseValues);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int grayValue = (int) (normalized[x][y] * 255);
                grayValue = Math.max(0, Math.min(255, grayValue));
                image.setRGB(x, y, (grayValue << 16) | (grayValue << 8) | grayValue);
            }
        }

        return image;
    }

    public BufferedImage createGreyScaleImage(int width, int height, double[][] noiseValues) {
        return createGreyScaleImage(width, height, noiseValues, 0);
    }

    private double[][] normalizeNoiseValues(double[][] noiseValues) {
        if (noiseValues == null || noiseValues.length == 0 || noiseValues[0].length == 0) {
            return noiseValues;
        }

        int rows = noiseValues.length;
        int cols = noiseValues[0].length;
        double[][] normalized = new double[rows][cols];
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (int xx = 0; xx < rows; ++xx) {
            for (int yy = 0; yy < cols; ++yy) {
                min = Math.min(min, noiseValues[xx][yy]);
                max = Math.max(max, noiseValues[xx][yy]);
            }
        }

        double scale = max - min;
        if (scale == 0) {
            for (int xx = 0; xx < rows; ++xx) {
                for (int yy = 0; yy < cols; ++yy) {
                    normalized[xx][yy] = 0.0;
                }
            }
        } else {
            for (int xx = 0; xx < rows; ++xx) {
                for (int yy = 0; yy < cols; ++yy) {
                    normalized[xx][yy] = (noiseValues[xx][yy] - min) / scale;
                }
            }
        }

        return normalized;
    }
}