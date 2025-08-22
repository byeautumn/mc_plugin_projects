package org.byeautumn.chuachua.player;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Example of a central manager (not in immersive as it's for explanation)
public class PlayerDataManager {
    private final Map<UUID, PlayerTracker> trackedPlayers = new HashMap<>();

    public void addPlayer(Player player) {
        trackedPlayers.put(player.getUniqueId(), new PlayerTracker(player));
    }

    public PlayerTracker getTracker(Player player) {
        return trackedPlayers.get(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        trackedPlayers.remove(player.getUniqueId());
    }

    // For future DB integration:
    // public void loadAllPlayersFromDatabase() { ... }
    // public void saveAllPlayersToDatabase() { ... }
}
