package org.aerial_dad.bedwars_plugin.bedwars.game;

import org.aerial_dad.bedwars_plugin.bedwars.game.Teams.BwTeam;
import org.aerial_dad.bedwars_plugin.bedwars.game.Teams.TeamManager;

import javax.annotation.Nonnull;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BwGame {

    public enum GameStatus {

        ONGOING,

        QUEUEING,

        TERMINATED
    }
    private GameStatus status = GameStatus.QUEUEING;

    private List<BwTeam> teams;
    private final BwWorld world;

    public BwGame( @Nonnull List<BwTeam> teams, @Nonnull BwWorld world) {
        if (teams.size() < 2) {
            throw new InvalidParameterException("There is not enough teams to build the BW game.");
        }
        this.teams = teams;
        this.world = world;
    }

    public BwWorld getBwWorld() {
        return world;
    }

    public List<BwTeam> getTeams() {
        return teams;
    }

    public GameStatus getStatus() {
        return status;
    }

    public static void main(String[] args) {
        Queue<BwPlayer> playerQueue = new ConcurrentLinkedQueue<>();
        for (int idx = 0; idx < 8; ++idx){
            BwPlayer player = new BwPlayer("p" + (idx + 1), null);
            playerQueue.add(player);
        }


        TeamManager tm = new TeamManager();
        List<BwTeam> teams = tm.buildTeams(playerQueue, 2, 2);

        BwGame firstGame = new BwGame(teams, null);
    }
}
