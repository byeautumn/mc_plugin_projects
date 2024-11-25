package org.aerial_dad.noodlelegs.game;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResourceGenerator {

    private Map<Material, Integer> materialGenerated;

    private final static double RECEIVER_RADIUS = 2.0d;

    private final Location location;

    public ResourceGenerator(Location location) {
        this.location = location;
    }

    public void generate() {
        World world = this.location.getWorld();
        List<Player> receivers = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            Location playerLocation = player.getLocation();
            double distance = this.location.distance(playerLocation);
            if (distance <= RECEIVER_RADIUS) {
                receivers.add(player);
            }
        }
        ItemStack itemstack = new ItemStack(Material.IRON_INGOT, 5);

        if (receivers.isEmpty()) {
            System.out.println("There is no player receiver in the range.");
            world.dropItem(this.location, itemstack);
            System.out.println("Generated: " + itemstack.getType() + ", " + itemstack.getAmount());
        } else {
            System.out.println("There are " + receivers.size() + " player receivers.");
            for (Player player : receivers){
                System.out.println("Sending " + itemstack.getType() + " to " + player.getDisplayName() + ".");
                player.getInventory().addItem(itemstack);
                player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 1.0f, 1.0f);
            }
        }
    }

}
