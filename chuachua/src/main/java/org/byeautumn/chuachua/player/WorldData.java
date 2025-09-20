package org.byeautumn.chuachua.player;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents data specific to a player within a particular world.
 */
@Deprecated
public class WorldData {
    private final String worldName;
    private double temperature;

    public WorldData(String worldName) {
        this.worldName = worldName;
        this.temperature = 20.0;
    }

    public String getWorldName() { return worldName; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("worldName", worldName);
        json.put("temperature", temperature);
        return json;
    }

    public static WorldData fromJsonObject(JSONObject json) throws JSONException {
        String worldName = json.getString("worldName");
        WorldData data = new WorldData(worldName);
        data.setTemperature(json.optDouble("temperature", 20.0));
        return data;
    }
}
