package org.byeautumn.chuachua.player;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

// JSON library imports
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Represents all the dynamic data associated with a player.
 * This class is designed to be easily serialized to and deserialized from a database or JSON file.
 */
@Deprecated
public class PlayerDataCommon {
    private final UUID playerUUID;
    private String playerName;
    private double health;
    private double hydration;
    private double nutrition;
    private List<ItemStack> inventoryContents;
    private final Map<String, WorldData> worldSpecificData;

    public PlayerDataCommon(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.health = 20.0;
        this.hydration = 20.0;
        this.nutrition = 20.0;
        this.inventoryContents = new ArrayList<>();
        this.worldSpecificData = new HashMap<>();
    }

    // Getters and Setters
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = Math.max(0, Math.min(20.0, health)); }
    public double getHydration() { return hydration; }
    public void setHydration(double hydration) { this.hydration = Math.max(0, Math.min(20.0, hydration)); }
    public double getNutrition() { return nutrition; }
    public void setNutrition(double nutrition) { this.nutrition = Math.max(0, Math.min(20.0, nutrition)); }
    public List<ItemStack> getInventoryContents() { return new ArrayList<>(inventoryContents); }
    public void setInventoryContents(List<ItemStack> inventoryContents) { this.inventoryContents = new ArrayList<>(inventoryContents); }
    public WorldData getWorldData(String worldName) { return worldSpecificData.computeIfAbsent(worldName, k -> new WorldData(worldName)); }
    public void setWorldData(String worldName, WorldData data) { this.worldSpecificData.put(worldName, data); }

    // Helper methods for inventory
    public void captureBukkitInventory(Player player) {
        List<ItemStack> currentInventory = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                currentInventory.add(item.clone());
            }
        }
        setInventoryContents(currentInventory);
    }

    public void applyBukkitInventory(Player player) {
        player.getInventory().clear();
        for (ItemStack item : inventoryContents) {
            player.getInventory().addItem(item.clone());
        }
        player.updateInventory();
    }

    // JSON Serialization and Deserialization Methods
    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("playerUUID", playerUUID.toString());
        json.put("playerName", playerName);
        json.put("health", health);
        json.put("hydration", hydration);
        json.put("nutrition", nutrition);

        JSONArray inventoryArray = new JSONArray();
        for (ItemStack item : inventoryContents) {
            inventoryArray.put(new JSONObject(item.serialize()));
        }
        json.put("inventoryContents", inventoryArray);

        JSONObject worldDataJson = new JSONObject();
        for (Map.Entry<String, WorldData> entry : worldSpecificData.entrySet()) {
            worldDataJson.put(entry.getKey(), entry.getValue().toJsonObject());
        }
        json.put("worldSpecificData", worldDataJson);

        return json;
    }

    public static PlayerDataCommon fromJsonObject(JSONObject json) throws JSONException {
        UUID playerUUID = UUID.fromString(json.getString("playerUUID"));
        String playerName = json.getString("playerName");
        PlayerDataCommon data = new PlayerDataCommon(playerUUID, playerName);

        data.setHealth(json.optDouble("health", 20.0));
        data.setHydration(json.optDouble("hydration", 20.0));
        data.setNutrition(json.optDouble("nutrition", 20.0));

        JSONArray inventoryArray = json.optJSONArray("inventoryContents");
        if (inventoryArray != null) {
            List<ItemStack> deserializedInventory = new ArrayList<>();
            for (int i = 0; i < inventoryArray.length(); i++) {
                JSONObject itemJson = inventoryArray.getJSONObject(i);
                deserializedInventory.add(ItemStack.deserialize(itemJson.toMap()));
            }
            data.setInventoryContents(deserializedInventory);
        }

        JSONObject worldDataJson = json.optJSONObject("worldSpecificData");
        if (worldDataJson != null) {
            for (String worldName : worldDataJson.keySet()) {
                JSONObject singleWorldJson = worldDataJson.getJSONObject(worldName);
                data.setWorldData(worldName, WorldData.fromJsonObject(singleWorldJson));
            }
        }

        return data;
    }

    public void saveToJsonFile(String filePath) throws IOException, JSONException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(toJsonObject().toString(4));
        }
    }

    public static PlayerDataCommon loadFromJsonFile(String filePath) throws IOException, JSONException {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        String jsonString = new String(Files.readAllBytes(Paths.get(filePath)));
        return fromJsonObject(new JSONObject(jsonString));
    }
}