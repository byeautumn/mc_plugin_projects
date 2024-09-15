package org.aerial_dad.noodlelegs.game;

import org.apache.commons.lang.NotImplementedException;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerTracker {

    private final Player player;

    private Game currentGame;

    private GameQueue currentGameQueue;

    private GameOrganizer currentGameOrganizer;

    private GameLauncher currentGameLauncher;

    private Team currentTeam;

    private PlayerStatus currentStatus = PlayerStatus.Unknown;

    public PlayerTracker(Player player){
        this.player = player;
    }

    public Game getCurrentGame() {
        return currentGame;
    }

    public void setCurrentGame(Game currentGame) {
        this.currentGame = currentGame;
    }

    public GameQueue getCurrentGameQueue() {
        return currentGameQueue;
    }

    public void setCurrentGameQueue(GameQueue currentGameQueue) {
        this.currentGameQueue = currentGameQueue;
    }

    public GameOrganizer getCurrentGameOrganizer() {
        return currentGameOrganizer;
    }

    public void setCurrentGameOrganizer(GameOrganizer currentGameOrganizer) {
        this.currentGameOrganizer = currentGameOrganizer;
    }

    public GameLauncher getCurrentGameLauncher() {
        return currentGameLauncher;
    }

    public void setCurrentGameLauncher(GameLauncher currentGameLauncher) {
        this.currentGameLauncher = currentGameLauncher;
    }

    public Team getCurrentTeam() {
        return currentTeam;
    }

    public void setCurrentTeam(Team currentTeam) {
        this.currentTeam = currentTeam;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerStatus getCurrentStatus() {
        return currentStatus;
    }

    public void update(GameLauncher launcher) {
        this.currentGameLauncher = launcher;
    }

    public void update(PlayerStatus status, Game game, Team team) {
        if (status != PlayerStatus.InGame) {
            System.err.println("Trying to update status to " + status.name() + " for player " + player.getDisplayName() + ", but wrong function is called. ");
            return;
        }
        if(null == game || null == team) {
            System.err.println("Invalid parameter - game or team should Not be null.");
            return;
        }
        this.currentStatus = status;
        this.currentGame = game;
        this.currentTeam = team;
        this.currentGameQueue = null;
    }

    public void update(PlayerStatus status, GameQueue queue) {
        if (status != PlayerStatus.InQueue) {
            System.err.println("Trying to update status to " + status.name() + " for player " + player.getDisplayName() + ", but wrong function is called. ");
            return;
        }
        if(null == queue) {
            System.err.println("Invalid parameter - game queue should Not be null.");
            return;
        }
        this.currentStatus = status;
        this.currentGameQueue = queue;
        this.currentGame = null;
    }

    public void update(GameOrganizer organizer) {
        this.currentGameOrganizer = organizer;
    }

    public void update(PlayerStatus status) {
        if(status == PlayerStatus.Unknown) {
            reset();
            return;
        }

        throw new NotImplementedException("Update player status to '" + status.name() + "' is Not implemented yet.");
    }

    private void setCurrentStatus(PlayerStatus status) {
        this.currentStatus = status;
    }

    public void reset() {
        setCurrentGame(null);
        setCurrentGameLauncher(null);
        setCurrentTeam(null);
        setCurrentGameOrganizer(null);
        setCurrentGameQueue(null);
        setCurrentStatus(PlayerStatus.Unknown);
        System.out.println(player.getDisplayName() + "'s status has been reset ");
    }
}
