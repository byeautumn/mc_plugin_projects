package org.byeautumn.chuachua.player;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.undo.ActionRecorder;

public class PlayerUtil {
    public static void sendObjectToMainHand(Player player, Material material) {
        // 1. Create the ItemStack:
        ItemStack itemStack = new ItemStack(material, 1); // 1 is the quantity

        // 2. Set the player's hand:
        player.getInventory().setItemInMainHand(itemStack);  // Use setItemInMainHand for the main hand
        // OR
        // player.getInventory().setItemInOffHand(sword); // Use setItemInOffHand for the off-hand


        // 3. Important: Update the player's inventory (sometimes needed):
//        player.updateInventory(); // This is often necessary for the change to be visible immediately
    }
}
