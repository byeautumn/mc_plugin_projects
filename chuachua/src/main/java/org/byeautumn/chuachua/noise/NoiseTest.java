package org.byeautumn.chuachua.noise; // Ensure this matches your package

public class NoiseTest {

    public static void main(String[] args) {
        long seed = 12345L; // Use a consistent seed
        // Let's try a significantly larger scale for the test, e.g., 0.01 or 0.1
        // This means the input coordinates to noise3_ImproveXY will be larger.
        double testScale = 0.01; // Changed from 0.0001 to 0.01

        System.out.println("--- Testing OpenSimplex2S.noise3_ImproveXY directly ---");
        System.out.println("Seed: " + seed + ", Test Scale: " + testScale);
        System.out.println("Expected range for raw noise: approx. [-1.0, 1.0]");
        // Removed lines that tried to print private static final constants:
        // System.out.println("OpenSimplex2S.NORMALIZER_3D: " + OpenSimplex2S.NORMALIZER_3D);
        // System.out.println("OpenSimplex2S.RSQUARED_3D: " + OpenSimplex2S.RSQUARED_3D);


        double minNoise = Double.MAX_VALUE;
        double maxNoise = Double.MIN_VALUE;

        // Sample a grid of points
        // Adjust loop increments to still get a good spread with the larger scale
        for (int x = -100; x <= 100; x += 5) { // Smaller step for more samples
            for (int z = -100; z <= 100; z += 5) {
                // Use a fixed Y coordinate, similar to your waterLevel
                double y = 63.0;

                // Apply the scale directly to the input coordinates
                float noiseValue = OpenSimplex2S.noise3_ImproveXY(seed, (float)(x * testScale), (float)(y * testScale), (float)(z * testScale));

                minNoise = Math.min(minNoise, noiseValue);
                maxNoise = Math.max(maxNoise, noiseValue);

                System.out.printf("Noise at (%.2f, %.2f, %.2f): %.8f%n", x * testScale, y * testScale, z * testScale, noiseValue);
            }
        }

        System.out.println("\n--- Summary ---");
        System.out.printf("Observed Noise Range: [%.8f, %.8f]%n", minNoise, maxNoise);
    }
}