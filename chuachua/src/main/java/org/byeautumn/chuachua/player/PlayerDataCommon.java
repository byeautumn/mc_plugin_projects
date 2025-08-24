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
import java.util.Date;

/**
 * Represents all the dynamic data associated with a player.
 * This class is designed to be easily serialized to and deserialized from a database or JSON file.
 */
public class PlayerDataCommon {
    private final UUID playerUUID;
    private String playerName;

    private double health;
    private double hydration;
    private double nutrition;
    private List<ItemStack> inventoryContents;
    private final Map<UUID, List<ItemStack>> inventoriesByWorld;
    private final Map<String, WorldData> worldSpecificData;

    /**
     * Constructor for new players or when loading from a database.
     * @param playerUUID The unique ID of the player.
     * @param playerName The current name of the player.
     */
    public PlayerDataCommon(UUID playerUUID, String playerName) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.health = 20.0;
        this.hydration = 20.0;
        this.nutrition = 20.0;
        // --- MODIFIED: Initialize the new map ---
        this.inventoriesByWorld = new HashMap<>();
        this.worldSpecificData = new HashMap<>();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }
    public String getPlayerName() {
        return playerName;
    }
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    public double getHealth() {
        return health;
    }
    public void setHealth(double health) {
        this.health = Math.max(0, Math.min(20.0, health));
    }
    public double getHydration() {
        return hydration;
    }
    public void setHydration(double hydration) {
        this.hydration = Math.max(0, Math.min(20.0, hydration));
    }
    public double getNutrition() {
        return nutrition;
    }
    public void setNutrition(double nutrition) {
        this.nutrition = Math.max(0, Math.min(20.0, nutrition));
    }
    public List<ItemStack> getInventoryContents() {
        return new ArrayList<>(inventoryContents);
    }
    public void setInventoryContents(List<ItemStack> inventoryContents) {
        this.inventoryContents = new ArrayList<>(inventoryContents);
    }
    public WorldData getWorldData(String worldName) {
        return worldSpecificData.computeIfAbsent(worldName, k -> new WorldData(worldName));
    }
    public void setWorldData(String worldName, WorldData data) {
        this.worldSpecificData.put(worldName, data);
    }

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

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("playerUUID", playerUUID.toString());
        json.put("playerName", playerName);
        json.put("health", health);
        json.put("hydration", hydration);
        json.put("nutrition", nutrition);

        // --- MODIFIED: Serialize per-world inventories ---
        JSONObject inventoriesJson = new JSONObject();
        for (Map.Entry<UUID, List<ItemStack>> entry : inventoriesByWorld.entrySet()) {
            JSONArray inventoryArray = new JSONArray();
            for (ItemStack item : entry.getValue()) {
                inventoryArray.put(new JSONObject(item.serialize()));
            }
            inventoriesJson.put(entry.getKey().toString(), inventoryArray);
        }
        json.put("inventoriesByWorld", inventoriesJson);

        // Serialize world-specific data
        JSONObject worldDataJson = new JSONObject();
        for (Map.Entry<String, WorldData> entry : worldSpecificData.entrySet()) {
            worldDataJson.put(entry.getKey(), entry.getValue().toJsonObject());
        }
        json.put("worldSpecificData", worldDataJson);

        return json;
    }

    // --- MODIFIED: fromJsonObject to handle map of inventories ---
    public static PlayerDataCommon fromJsonObject(JSONObject json) throws JSONException {
        UUID playerUUID = UUID.fromString(json.getString("playerUUID"));
        String playerName = json.getString("playerName");
        PlayerDataCommon data = new PlayerDataCommon(playerUUID, playerName);

        data.setHealth(json.optDouble("health", 20.0));
        data.setHydration(json.optDouble("hydration", 20.0));
        data.setNutrition(json.optDouble("nutrition", 20.0));

        // --- MODIFIED: Deserialize per-world inventories ---
        JSONObject inventoriesJson = json.optJSONObject("inventoriesByWorld");
        if (inventoriesJson != null) {
            for (String worldUUIDString : inventoriesJson.keySet()) {
                try {
                    UUID worldUUID = UUID.fromString(worldUUIDString);
                    JSONArray inventoryArray = inventoriesJson.getJSONArray(worldUUIDString);
                    List<ItemStack> deserializedInventory = new ArrayList<>();
                    for (int i = 0; i < inventoryArray.length(); i++) {
                        JSONObject itemJson = inventoryArray.getJSONObject(i);
                        deserializedInventory.add(ItemStack.deserialize(itemJson.toMap()));
                    }
                    data.setInventoryForWorld(worldUUID, deserializedInventory);
                } catch (IllegalArgumentException e) {
                    // Log error for invalid UUID strings in JSON if necessary
                }
            }
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

    /**
     * Retrieves the inventory contents for a specific world.
     * @param worldUUID The UUID of the world.
     * @return The list of ItemStacks for that world, or an empty list if not found.
     */
    public List<ItemStack> getInventoryForWorld(UUID worldUUID) {
        return inventoriesByWorld.getOrDefault(worldUUID, new ArrayList<>());
    }

    /**
     * Sets or updates the inventory contents for a specific world.
     * @param worldUUID The UUID of the world.
     * @param inventory The list of ItemStacks for that world.
     */
    public void setInventoryForWorld(UUID worldUUID, List<ItemStack> inventory) {
        this.inventoriesByWorld.put(worldUUID, new ArrayList<>(inventory));
    }

    /**
     * A helper method to capture a Bukkit Player's inventory for a specific world.
     * @param player The Bukkit Player object.
     * @param worldUUID The UUID of the world to associate this inventory with.
     */
    public void captureBukkitInventoryForWorld(Player player, UUID worldUUID) {
        List<ItemStack> currentInventory = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                currentInventory.add(item.clone());
            }
        }
        setInventoryForWorld(worldUUID, currentInventory);
    }

    /**
     * A helper method to apply a specific world's inventory back to a Bukkit Player.
     * @param player The Bukkit Player object.
     * @param worldUUID The UUID of the world whose inventory should be applied.
     */
    public void applyBukkitInventoryForWorld(Player player, UUID worldUUID) {
        player.getInventory().clear();
        List<ItemStack> worldInventory = getInventoryForWorld(worldUUID);
        for (ItemStack item : worldInventory) {
            player.getInventory().addItem(item.clone());
        }
        player.updateInventory();
    }

}