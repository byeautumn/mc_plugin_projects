package org.aerial_dad.noodlelegs.game;

public class GameTracker {
    private final GameLauncher gameLauncher;
    private final Game game;

    public GameTracker(GameLauncher launcher, Game game) {
        this.gameLauncher = launcher;
        this.game = game;
    }

    public GameLauncher getGameLauncher() {
        return gameLauncher;
    }

    public Game getGame() {
        return game;
    }

//    public String printGameLauncherStatus() {
//        return getGameLauncher().printSelf();
//    }
//
//    public String printGameStatus() {
//        return getGame().getStatus().name();
//    }
}
