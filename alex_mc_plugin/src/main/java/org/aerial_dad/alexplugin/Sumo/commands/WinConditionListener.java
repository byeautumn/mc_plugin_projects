package org.aerial_dad.alexplugin.Sumo.commands;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class WinConditionListener implements Listener {

    private SumoGame sumoGame;

    public WinConditionListener(SumoGame sumoGame) {
        this.sumoGame = sumoGame;
    }

    @EventHandler
    public void onPlayerMoveEvent(PlayerMoveEvent event) {
        // Check if player moved outside the arena boundaries and handle win conditions
    }
}
