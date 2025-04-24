package org.byeautumn.chuachua.generate.world.pipeline;

public class BiomeConstants {


    private float noiseScale = 0.04f;
    private float tempScale = 0.007f;
    private float hydrScale = 0.007f;
    private float altitudeScale = 0.02f;
    private float continentalScale = 0.01f;
    private float regionalScale = 0.05f;
    private float erosionScale = 0.05f;
    public float getTempScale() {
        return tempScale;
    }

    public float getHydrScale() {
        return hydrScale;
    }

    public float getAltitudeScale() {
        return altitudeScale;
    }

    public float getContinentalScale() {
        return continentalScale;
    }

    public float getRegionalScale() {
        return regionalScale;
    }

    public float getErosionScale() {
        return erosionScale;
    }
    public float getNoiseScale() {
        return noiseScale;
    }


}
