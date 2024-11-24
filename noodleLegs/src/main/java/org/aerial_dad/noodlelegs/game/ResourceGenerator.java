package org.aerial_dad.noodlelegs.game;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResourceGenerator {

    private static Player operator;

    private Map<Material, Integer> materialGenerated;

    private int interval;

    private final static double RECEIVER_RADIUS = 5.0d;


    public void generate(Location generatorLocation) {
        World world = generatorLocation.getWorld();
        List<Player> receivers = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            Location playerLocation = player.getLocation();
            double distance = generatorLocation.distance(playerLocation);
            if (distance <= RECEIVER_RADIUS) {
                receivers.add(player);
            }
        }
        ItemStack itemstack = new ItemStack(Material.IRON_INGOT, 5);

        if (receivers.isEmpty()) {
            world.dropItem(generatorLocation, itemstack);
        } else {
            for (Player player : receivers){
                player.getInventory().addItem(itemstack);


            }
            
        }
    }

}
