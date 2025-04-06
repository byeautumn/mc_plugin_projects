package org.byeautumn.chuachua.recipe;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public abstract class CustomRecipe {
    // Unique key for the recipe (used for registration)
    private final NamespacedKey key;
    // Resulting ItemStack of the recipe
    private ItemStack result;
    //The plugin
    private final Plugin plugin;

    // Constructor for CustomRecipe
    public CustomRecipe(Plugin plugin, String keyName, ItemStack result) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, keyName);
        this.result = result;
    }

    // Getter for the key
    public NamespacedKey getKey() {
        return key;
    }

    // Getter for the result
    public ItemStack getResult() {
        return result;
    }

    // Method to set the result
    public void setResult(ItemStack result) {
        this.result = result;
    }

    // Abstract method to define the recipe's ingredients and shape
    public abstract ShapedRecipe getRecipe();

    // Method to register the recipe with the server
    public void registerRecipe() {
        ShapedRecipe recipe = getRecipe();
        if (recipe != null) {
            plugin.getServer().addRecipe(recipe);
        } else {
            plugin.getLogger().warning("Failed to register recipe: " + key.getKey());
        }
    }

    // Method to create an ItemStack with custom NBT data.  This is crucial for making
    // the item unique, especially when using custom model data for textures.
    public static ItemStack createCustomItem(Plugin plugin, Material material, int amount, String displayName, int customModelData, Map<String, String> nbtData) {
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            //Set Custom Model Data.
            meta.setCustomModelData(customModelData);

            // Store custom NBT data.
            PersistentDataContainer container = meta.getPersistentDataContainer();
            for (Map.Entry<String, String> entry : nbtData.entrySet()) {
                container.set(new NamespacedKey(plugin, entry.getKey()), PersistentDataType.STRING, entry.getValue());
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
}
