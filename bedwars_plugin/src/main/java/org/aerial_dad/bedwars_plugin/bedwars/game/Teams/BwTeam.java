package org.aerial_dad.bedwars_plugin.bedwars.game.Teams;

import org.aerial_dad.bedwars_plugin.bedwars.game.BwPlayer;

import java.util.ArrayList;
import java.util.List;

public class BwTeam {
    private List<BwPlayer> players = new ArrayList<>();

    public List<BwPlayer> getPlayers() {
        return players;
    }

    public void addPlayer(BwPlayer player) {
        this.players.add(player);
    }

    public String printSelf() {
        StringBuffer sb = new StringBuffer();
        for(BwPlayer player : players) {
            sb.append(player.getBwName());
            sb.append(" | ");
        }
        return sb.toString();
    }
}
