package org.byeautumn.chuachua.noise;

public class SceneNoise {

    private Perlin perlin;

    public SceneNoise(long seed) {
        this.perlin = new Perlin(seed);
    }

    public float getBiomeNoise(float x, float z) {
        float scale1 = 0.005f;
        float scale2 = 0.01f;
        float scale3 = 0.02f;

        float noise1 = perlin.perlin(x * scale1, z * scale1);
        float noise2 = perlin.perlin(x * scale2, z * scale2);
        float noise3 = perlin.perlin(x * scale3, z * scale3);

        // Combine noise layers for more complex patterns
        float combinedNoise = (noise1 + noise2 * 0.7f + noise3 * 0.4f) / 2.1f; // Adjust weights as needed

        // Add some post-processing or adjustments if desired
        combinedNoise = (combinedNoise + 1.0f) / 2.0f; // Normalize to 0-1 range (optional)

        return combinedNoise;
    }
}