package org.aerial_dad.alexplugin.Sumo.commands;

import org.bukkit.entity.Player;

public class MessageUtils {

    public static void broadcastMessage(Iterable<Player> players, String message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }
}