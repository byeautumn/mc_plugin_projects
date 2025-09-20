package org.byeautumn.chuachua.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonUtils {
    private static final Logger LOGGER = Logger.getLogger(JsonUtils.class.getName());

    /**
     * Serializes a single ItemStack to a JSONObject.
     *
     * @param item The ItemStack to serialize.
     * @return A JSONObject representing the item.
     */
    public static JSONObject serializeItem(ItemStack item) {
        if (item == null) {
            return new JSONObject();
        }
        JSONObject itemObject = new JSONObject();
        itemObject.put("type", item.getType().name());
        itemObject.put("amount", item.getAmount());
        // You can add more data here, like enchantments, durability, etc.
        return itemObject;
    }

    /**
     * Deserializes a JSONObject into a single ItemStack.
     *
     * @param jsonObject The JSONObject to deserialize.
     * @return An ItemStack representing the object.
     */
    public static ItemStack deserializeItem(JSONObject jsonObject) {
        if (jsonObject == null || !jsonObject.has("type")) {
            return null;
        }
        try {
            Material type = Material.getMaterial(jsonObject.getString("type"));
            int amount = jsonObject.optInt("amount", 1);
            if (type != null) {
                return new ItemStack(type, amount);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to deserialize item: " + jsonObject.toString(), e);
        }
        return null;
    }
}
