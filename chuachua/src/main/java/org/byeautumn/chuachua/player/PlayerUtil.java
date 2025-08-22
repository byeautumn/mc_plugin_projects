package org.byeautumn.chuachua.player;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PlayerUtil {
    public static void sendObjectToMainHand(Player player, Material material) {
        ItemStack itemStack = new ItemStack(material, 1);
        player.getInventory().setItemInMainHand(itemStack);
    }
}