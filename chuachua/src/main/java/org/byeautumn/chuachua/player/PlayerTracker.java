package org.byeautumn.chuachua.player;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.byeautumn.chuachua.common.PlayMode;

import java.util.List;
import java.util.UUID;

public class PlayerTracker {
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

    // --- NEW: Convenience methods for per-world inventory access ---
    /**
     * Gets the player's inventory for a specific world from their PlayerDataCommon.
     * @param worldUUID The UUID of the world.
     * @return The inventory for that world.
     */
    public List<ItemStack> getInventoryForWorld(UUID worldUUID) {
        return playerDataCommon.getInventoryForWorld(worldUUID);
    }

    /**
     * Sets the player's inventory for a specific world in their PlayerDataCommon.
     * @param worldUUID The UUID of the world.
     * @param inventory The inventory to set.
     */
    public void setInventoryForWorld(UUID worldUUID, List<ItemStack> inventory) {
        playerDataCommon.setInventoryForWorld(worldUUID, inventory);
    }


    public void reset() {
        setChapterIndex(-1);
        setStatus(PlayerStatus.Unknown);
        setPlayMode(PlayMode.UNKNOWN);
        this.playerDataCommon = new PlayerDataCommon(player.getUniqueId(), player.getName());
    }
}