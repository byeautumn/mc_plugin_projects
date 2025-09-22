package org.byeautumn.chuachua.player;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.byeautumn.chuachua.common.PlayMode;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

public class PlayerTracker {
    private static final Logger LOGGER = Logger.getLogger(PlayerTracker.class.getName());

    private final Player player;

    private PlayerStatus status = PlayerStatus.Unknown;
    private PlayMode playMode = PlayMode.UNKNOWN;

    public PlayerTracker(Player player) {
        this.player = player;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public void setPlayMode(PlayMode playMode) {
        this.playMode = playMode;
    }

    public PlayMode getPlayMode() {
        return playMode;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * Resets the player tracker to its initial state.
     */
    public void reset() {
        setStatus(PlayerStatus.Unknown);
        setPlayMode(PlayMode.UNKNOWN);
    }
}
