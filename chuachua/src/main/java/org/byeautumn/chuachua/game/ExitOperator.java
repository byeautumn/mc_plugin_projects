package org.byeautumn.chuachua.game;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ExitOperator {
    private static final double EXIT_RADIUS = 20.0;
    private static final int EXIT_LOAD_LIMIT = 2;
    private final Chapter chapter;

    public ExitOperator(Chapter chapter) {
        this.chapter = chapter;
    }

    public void takeOff() {
        Location exit = chapter.getExitLocation();
        World world = chapter.getWorld();
        List<Player> players = world.getPlayers();
        List<Player> luckyPlayers = new ArrayList<>();
        for (Player player : players) {
            double distance = exit.distance(player.getLocation());
            if (distance <= EXIT_RADIUS) {
                luckyPlayers.add(player);
            }
        }

        if (luckyPlayers.size() > EXIT_LOAD_LIMIT) {
            System.out.println("The lucky players number (" + luckyPlayers.size() + ") at the exit exceeds the limit (" + EXIT_LOAD_LIMIT + ").");
            for(Player player : luckyPlayers) {
                String title = "No one can leave the chapter!!!";
                String subtitle = "The lucky players number (" + luckyPlayers.size() + ") at the exit exceeds the limit (" + EXIT_LOAD_LIMIT + ").";
                player.sendTitle(title, subtitle, 10, 20, 10);
            }
            return;
        }

        for(Player player : luckyPlayers) {
            String title = "Congratulations!!!";
            String subtitle = "You have passed this chapter.";
            player.sendTitle(title, subtitle, 10, 20, 10);
            GameOrganizer.getInstance().guidePlayer(player);
        }

    }
}
