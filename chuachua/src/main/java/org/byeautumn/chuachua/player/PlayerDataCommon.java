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
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Represents all the dynamic data associated with a player.
 * This class is designed to be easily serialized to and deserialized from a database or JSON file.
 */
@Deprecated
public class PlayerDataCommon {
    private static final Logger LOGGER = Logger.getLogger(PlayerDataCommon.class.getName());

    private final UUID playerUUID;
    private String playerName;

    private double health;
    private double hydration;
    private double nutrition;
    // This field is deprecated and should not be used. Use InventoryDataAccessor instead.
    @Deprecated
    private List<ItemStack> inventoryContents;
    @Deprecated
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
        this.inventoriesByWorld = new HashMap<>();
        this.worldSpecificData = new HashMap<>();
        LOGGER.log(Level.WARNING, "PlayerDataCommon is deprecated. Use PlayerData and InventoryDataAccessor.");
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
        this.health = health;
    }

    public double getHydration() {
        return hydration;
    }

    public void setHydration(double hydration) {
        this.hydration = hydration;
    }

    public double getNutrition() {
        return nutrition;
    }

    public void setNutrition(double nutrition) {
        this.nutrition = nutrition;
    }

    public Map<UUID, List<ItemStack>> getInventoriesByWorld() {
        return inventoriesByWorld;
    }

    @Deprecated
    public List<ItemStack> getInventoryForWorld(UUID worldUUID) {
        return inventoriesByWorld.getOrDefault(worldUUID, new ArrayList<>());
    }

    @Deprecated
    public void setInventoryForWorld(UUID worldUUID, List<ItemStack> inventory) {
        this.inventoriesByWorld.put(worldUUID, new ArrayList<>(inventory));
    }

    @Deprecated
    public void captureBukkitInventoryForWorld(Player player, UUID worldUUID) {
        List<ItemStack> currentInventory = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                currentInventory.add(item.clone());
            }
        }
        setInventoryForWorld(worldUUID, currentInventory);
    }

    @Deprecated
    public void applyBukkitInventoryForWorld(Player player, UUID worldUUID) {
        player.getInventory().clear();
        List<ItemStack> worldInventory = getInventoryForWorld(worldUUID);
        for (ItemStack item : worldInventory) {
            player.getInventory().addItem(item.clone());
        }
        player.updateInventory();
    }
}
