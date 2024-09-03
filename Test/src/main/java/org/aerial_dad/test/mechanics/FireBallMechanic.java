package org.aerial_dad.test.mechanics;

import org.bukkit.Material;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class FireBallMechanic implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Replace "YOUR_FIREBALL_ITEM" with the item you want to use to throw the fireball
        if (item.isSimilar(new ItemStack(Material.FIRE_CHARGE))) {
            event.setCancelled(true); // Prevent default item usage
            Fireball fireball = player.launchProjectile(Fireball.class);
            fireball.setShooter(player);
            fireball.setDirection(player.getEyeLocation().getDirection().multiply(2)); // Adjust speed as needed
            fireball.setYield(3); // Adjust explosion size as needed
        }
    }
}
