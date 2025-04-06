package org.byeautumn.chuachua.recipe;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class CarRecipe extends CustomRecipe{

    private static final int CAR_MODEL_DATA = 1001; // Example custom model data value
    private static final String CAR_NBT_KEY = "car_id";  // NBT key to identify the car
    private static final String CAR_NBT_VALUE = "unique_car_123";
    private static Plugin plugin;

    // Constructor for CarRecipe
    public CarRecipe(Plugin plugin) {
        super(plugin, "car_recipe", createCarItemStack(plugin)); // Pass the plugin instance
        CarRecipe.plugin = plugin;
    }

    // Static method to create the Car ItemStack with custom NBT data
    private static ItemStack createCarItemStack(Plugin plugin) {
        // Define the NBT data for the car.  This makes it unique!
        Map<String, String> nbtData = new HashMap<>();
        nbtData.put(CAR_NBT_KEY, CAR_NBT_VALUE);

        return CustomRecipe.createCustomItem(
                plugin,
                Material.STONE, // Base material (can be anything, texture is changed by resource pack)
                1,                // Amount
                "Car",            // Display name
                CAR_MODEL_DATA, // Custom model data (important for texture)
                nbtData           // Our custom NBT data
        );
    }

    // Implementation of the getRecipe() method
    @Override
    public ShapedRecipe getRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(getKey(), getResult());
        recipe.shape(
                "SSS",
                "SMS",
                "GGG"
        );
        recipe.setIngredient('S', Material.STONE);
        recipe.setIngredient('M', Material.IRON_INGOT);  // Changed to IRON_INGOT
        recipe.setIngredient('G', Material.GLASS);
        return recipe;
    }

    public static boolean isCar(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta.hasCustomModelData() && meta.getCustomModelData() == CAR_MODEL_DATA) {
            // Check for our specific NBT key-value pair.
            PersistentDataContainer container = meta.getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(plugin, CAR_NBT_KEY); // Use the key from the CarRecipe
            return container.has(key, PersistentDataType.STRING) &&
                    CAR_NBT_VALUE.equals(container.get(key, PersistentDataType.STRING));
        }
        return false;
    }

    // Method to spawn the car entity
    public void spawnCar(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // Car base (3x2)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 0; z++) {
                world.setType(location.clone().add(x, 0, z), Material.STONE); // Or DARK_GRAY_CONCRETE, etc.
            }
        }
        // Car roof
        for (int x = -1; x <= 1; x++) {
            world.setType(location.clone().add(x, 1, -1), Material.GLASS);
        }

        //add wheels
        world.setType(location.clone().add(-1, 0, -2), Material.BLACK_WOOL);
        world.setType(location.clone().add(1, 0, -2), Material.BLACK_WOOL);
        world.setType(location.clone().add(-1, 0, 1), Material.BLACK_WOOL);
        world.setType(location.clone().add(1, 0, 1), Material.BLACK_WOOL);
    }
}
