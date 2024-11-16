package org.aerial_dad.noodlelegs.game;

import org.aerial_dad.noodlelegs.Universe;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GameOrganizer {
    private final GameType type;
    private final GameConfig config;

    private List<GameLauncher> gameLaunchers;

    public GameOrganizer(GameType gameType, GameConfig config){
        this.type = gameType;
        this.config = config;
    }

    public void queuePlayer(Player player) {
        if (this.gameLaunchers == null) {
            this.gameLaunchers = new ArrayList<>();
        }
        System.out.println("Game launcher count: " + this.gameLaunchers.size());
        for(GameLauncher launcher : this.gameLaunchers) {
            if (launcher.acceptNewPlayer()) {
                launcher.queuePlayer(player);
                System.out.println("Locate a launcher who accept new players.");
                return;
            }
        }
        System.out.println("There is no launcher available currently. Generating a new one ...");
        GameLauncher launcher = new GameLauncher(createGameName(this.type, this.gameLaunchers.size()), this.type, this.config);
        launcher.queuePlayer(player);
        this.gameLaunchers.add(launcher);

        PlayerTracker playerTracker = Universe.getPlayerTracker(player);
        playerTracker.update(this);
    }

    public void removeGameLauncher(GameLauncher gameLauncher) {
        this.gameLaunchers.remove(gameLauncher);
        System.out.println("A game launcher was just removed. And the number of remaining game launchers is: " + this.gameLaunchers.size());
    }

    private String createGameName(GameType type, int launcherIndex) {
        return this.type.name() + "_" + this.gameLaunchers.size() + "_" + System.currentTimeMillis();
    }

}
