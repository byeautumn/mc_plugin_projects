package org.byeautumn.chuachua.game;

import org.bukkit.entity.Player;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.player.PlayerStatus;
import org.byeautumn.chuachua.player.PlayerTracker;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class GameOrganizer {
    private Game game = new Game();
    private static final GameOrganizer organizer = new GameOrganizer();
    private GameOrganizer() {}

    public static GameOrganizer getInstance() {
        return organizer;
    }

    public void guidePlayer(@Nonnull Player player) {
        int chapterIndex = 0;
        PlayerTracker playerTracker = Universe.getPlayerTracker(player);
        if (playerTracker.getChapterIndex() >= 0) {
            chapterIndex = playerTracker.getChapterIndex() + 1;
        }

        Chapter chapter = this.game.getChapter(chapterIndex);
        if (null == chapter) {
            System.out.println("There is no available chapter for player " + player.getDisplayName() + ".");
            System.out.println("Player " + player.getDisplayName() + " will be teleport to lobby.");
            Universe.teleportToLobby(player);
            Universe.resetPlayerTracker(player);
            return;
        }

        System.out.println("Game organizer is guiding player " + player.getDisplayName() + " to chapter " + chapterIndex + ".");
        chapter.spawnPlayer(player);

        playerTracker.setStatus(PlayerStatus.InGame);
        playerTracker.setChapterIndex(chapterIndex);

    }
}
