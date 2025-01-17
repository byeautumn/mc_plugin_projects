package org.byeautumn.chuachua.player;

import org.bukkit.entity.Player;

public class PlayerTracker {
    private final Player player;

    private PlayerStatus status = PlayerStatus.Unknown;

    private int chapterIndex = -1;

    public PlayerTracker(Player player) {
        this.player = player;
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

    public void reset() {
        setChapterIndex(-1);
        setStatus(PlayerStatus.Unknown);
    }

}
