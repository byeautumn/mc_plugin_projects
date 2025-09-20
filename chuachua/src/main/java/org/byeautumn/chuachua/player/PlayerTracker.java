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
    private PlayerDataCommon playerDataCommon;

    private PlayerStatus status = PlayerStatus.Unknown;
    private PlayMode playMode = PlayMode.UNKNOWN;
    private int chapterIndex = -1;

    public PlayerTracker(Player player) {
        this.player = player;
        this.playerDataCommon = new PlayerDataCommon(player.getUniqueId(), player.getName());
    }

    public PlayerDataCommon getPlayerDataCommon() {
        return playerDataCommon;
    }

    public void setPlayerDataCommon(PlayerDataCommon playerDataCommon) {
        this.playerDataCommon = playerDataCommon;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public int getChapterIndex() {
        return chapterIndex;
    }

    public void setChapterIndex(int chapterIndex) {
        this.chapterIndex = chapterIndex;
    }

    public void setPlayMode(PlayMode playMode) {
        this.playMode = playMode;
    }

    public PlayMode getPlayMode() {
        return playMode;
    }

    /**
     * Resets the player tracker to its initial state.
     */
    public void reset() {
        setChapterIndex(-1);
        setStatus(PlayerStatus.Unknown);
        setPlayMode(PlayMode.UNKNOWN);
        this.playerDataCommon = new PlayerDataCommon(player.getUniqueId(), player.getName());
    }
}
