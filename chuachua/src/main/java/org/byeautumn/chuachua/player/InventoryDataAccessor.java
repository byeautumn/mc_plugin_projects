package org.byeautumn.chuachua.player;

import org.bukkit.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONObject;
import org.byeautumn.chuachua.accessor.Accessor;
import org.byeautumn.chuachua.util.JsonUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InventoryDataAccessor implements Accessor {

    private final File baseDir;
    private static final Logger LOGGER = Logger.getLogger(InventoryDataAccessor.class.getName());
    private static final String HUB_FILE_NAME = "hub.json";

    public InventoryDataAccessor(File baseDir) {
        this.baseDir = new File(baseDir, "inventory-data");
        createDirectories(this.baseDir);
    }

    @Override
    public void createDirectories(File baseDir) {
        try {
            if (!baseDir.exists()) {
                Files.createDirectories(baseDir.toPath());
                LOGGER.info("Created inventory data base directory: " + baseDir.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create inventory data directories: " + e.getMessage());
            throw new RuntimeException("Could not create inventory data directories", e);
        }
    }

    public void saveInventory(UUID playerUUID, String worldInternalName, ItemStack[] contents) {
        File playerDir = new File(this.baseDir, playerUUID.toString());
        createDirectories(playerDir);

        String fileName = worldInternalName.equalsIgnoreCase("world") ? HUB_FILE_NAME : worldInternalName + ".json";
        File file = new File(playerDir, fileName);
        JSONObject inventoryJson = new JSONObject();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null) {
                // Serialize item with its slot index
                inventoryJson.put(String.valueOf(i), JsonUtils.serializeItem(item));
            }
        }

        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(inventoryJson.toString(4));
            LOGGER.info("Successfully saved inventory for player " + playerUUID + " in world " + worldInternalName);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save inventory for player " + playerUUID + ": " + e.getMessage(), e);
        }
    }


    public ItemStack[] loadInventory(UUID playerUUID, String worldInternalName) {
        File playerDir = new File(this.baseDir, playerUUID.toString());
        String fileName = worldInternalName.equalsIgnoreCase("world") ? HUB_FILE_NAME : worldInternalName + ".json";
        File file = new File(playerDir, fileName);
        ItemStack[] inventory = new ItemStack[41]; // Size of a standard player inventory

        if (!file.exists()) {
            LOGGER.warning("Inventory file not found for player " + playerUUID + " in world " + worldInternalName);
            return inventory;
        }

        try (FileReader fileReader = new FileReader(file)) {
            StringBuilder sb = new StringBuilder();
            int character;
            while ((character = fileReader.read()) != -1) {
                sb.append((char) character);
            }
            JSONObject inventoryJson = new JSONObject(sb.toString());

            for (String key : inventoryJson.keySet()) {
                try {
                    int slot = Integer.parseInt(key);
                    JSONObject itemJson = inventoryJson.getJSONObject(key);
                    inventory[slot] = JsonUtils.deserializeItem(itemJson);
                } catch (NumberFormatException e) {
                    LOGGER.warning("Invalid key in inventory file: " + key);
                }
            }

            LOGGER.info("Successfully loaded inventory for player " + playerUUID + " in world " + worldInternalName);
            return inventory;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load inventory for player " + playerUUID + ": " + e.getMessage(), e);
            return new ItemStack[41];
        }
    }

}