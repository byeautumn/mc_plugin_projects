package org.aerial_dad.noodlelegs.game;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResourceGenerator {

    private static Player operator;

    private Map<Material, Integer> materialGenerated;

    private int interval;

    private Location generatiorLocation;

    private final static double RECEIVER_RADIUS = 5.0d;

    public void Generator(Player itemReceiver){

        

    }

    private void generate() {
        World world = generatiorLocation.getWorld();
        List<Player> receivers = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            Location playerLocation = player.getLocation();
            double distance = this.generatiorLocation.distance(playerLocation);
            if (distance <= RECEIVER_RADIUS) {
                receivers.add(player);
            }
        }

        if (receivers.isEmpty()) {

        } else {
            
        }
    }

}
