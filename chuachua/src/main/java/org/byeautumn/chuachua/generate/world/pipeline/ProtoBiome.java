package org.byeautumn.chuachua.generate.world.pipeline;


//Added ProtoBiome class
class ProtoBiome {
    private final String name;
    private final float temperature;
    private final float humidity;
    private final float altitude;
    private final float continental;
    private final float regional;
    private final float erosion;

    public ProtoBiome(String name, float temperature, float humidity, float altitude, float continental, float regional, float erosion) {
        this.name = name;
        this.temperature = temperature;
        this.humidity = humidity;
        this.altitude = altitude;
        this.continental = continental;
        this.regional = regional;
        this.erosion = erosion;
    }

    public String getName() {
        return name;
    }

    public float getTemperature() {
        return temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public float getAltitude() {
        return altitude;
    }

    public float getContinental() {
        return continental;
    }

    public float getRegional() {
        return regional;
    }

    public float getErosion() {
        return erosion;
    }
}