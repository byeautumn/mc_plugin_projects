package org.byeautumn.chuachua.noise;

import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

public class WorleyNoise {

    private final long seed;
    private final int numPointsPerCell; // Number of feature points to place in each cell

    // Enum to define different output types (F1, F2, F2-F1, etc.)
    public enum FeatureType {
        F1,     // Distance to the closest point
        F2,     // Distance to the second closest point
        F2_MINUS_F1 // Difference between second and first closest points (good for borders)
    }

    public WorleyNoise(long seed) {
        this(seed, 1); // Default to 1 point per cell
    }

    public WorleyNoise(long seed, int numPointsPerCell) {
        this.seed = seed;
        this.numPointsPerCell = Math.max(1, numPointsPerCell); // Ensure at least 1 point
    }

    /**
     * Generates 2D Worley noise for a given (x, z) coordinate.
     *
     * @param x The X coordinate.
     * @param z The Z coordinate.
     * @param featureType The desired feature type (e.g., F2_MINUS_F1 for borders).
     * @return A noise value, typically in a normalized range (e.g., 0.0 to 1.0 or -1.0 to 1.0 depending on feature).
     */
    public double noise2D(double x, double z, FeatureType featureType) {
        // Determine the current cell coordinates
        int cellX = (int) Math.floor(x);
        int cellZ = (int) Math.floor(z);

        // Store distances to feature points
        ArrayList<Double> distances = new ArrayList<>();

        // Search in a 3x3 grid of cells around the current cell
        // This ensures we capture feature points that might be in adjacent cells but closer
        // than points in the current cell (especially near cell boundaries).
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                // Get a seeded random generator for the current cell's feature points
                // We use a hash of cell coordinates and the global seed for consistency.
                Random cellRandom = new Random(hashCoords(cellX + i, cellZ + j, seed));

                // Generate feature points within this cell
                for (int k = 0; k < numPointsPerCell; k++) {
                    double pointX = (cellX + i) + cellRandom.nextDouble(); // Random point within cell
                    double pointZ = (cellZ + j) + cellRandom.nextDouble();

                    // Calculate distance from query point (x, z) to this feature point
                    double dx = x - pointX;
                    double dz = z - pointZ;
                    distances.add(dx * dx + dz * dz); // Store squared distance for efficiency (no sqrt yet)
                }
            }
        }

        // Sort distances to find F1, F2, etc.
        Collections.sort(distances);

        // Calculate and return the desired feature type
        switch (featureType) {
            case F1:
                return Math.sqrt(distances.get(0)); // Distance to closest point
            case F2:
                return Math.sqrt(distances.get(1)); // Distance to second closest point
            case F2_MINUS_F1:
                // Normalize F2-F1. Raw values can vary.
                // A common normalization approach is to divide by a max possible range.
                // For Worley (F2-F1), output usually ranges roughly from 0 to 0.7-1.0
                // depending on implementation/points per cell.
                // We'll normalize to roughly -1 to 1 for consistency with other noise.
                double rawF2_F1 = Math.sqrt(distances.get(1)) - Math.sqrt(distances.get(0));
                // Empirical normalization: for 1 point per cell, F2-F1 max is around 0.5-0.7.
                // Let's scale it to fit roughly -1 to 1 for terrain use.
                return (rawF2_F1 * 2.0) - 1.0; // Adjust scale and bias as needed for desired range
            default:
                return 0.0; // Should not happen
        }
    }

    // A simple hashing function to create a reproducible seed for each cell
    private long hashCoords(int x, int z, long seed) {
        long h = seed + (long)x * 36875931; // Large prime
        h = h ^ ((long)z * 741103597); // Another large prime
        h = h * 920409893; // Yet another large prime
        return h;
    }
}
