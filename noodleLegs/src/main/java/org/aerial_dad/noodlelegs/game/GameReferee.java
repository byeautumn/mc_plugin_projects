package org.aerial_dad.noodlelegs.game;

import org.bukkit.ChatColor;

public class GameReferee {

    private final GameConfig gameConfig;

    public GameReferee(GameConfig gameConfig){
        this.gameConfig = gameConfig;
    }

    public void judge(Game game){
        for(Team team : game.getTeams()) {
            if(game.getEliminatedTeams().contains(team.getId())) {
                System.out.println();
                team.displayTitle(ChatColor.RED + "Defeated!");
                System.out.println("Team '" + team.getName() + "' is defeated.");
            } else {
                team.displayTitle(ChatColor.GREEN + "Victory!");
                System.out.println("Team '" + team.getName() + "' wins.");
            }
        }
        game.terminate();
    }

    public boolean shouldRespawn(PlayerTracker playerTracker) {
        // TODO
        return false;
    }

    public boolean isDefeated(Team team) {
        return team.getEliminatedSet().size() == team.getPlayers().size();
    }

    public boolean shouldGameBeTerminated(Game game) {
        return game.getEliminatedTeams().size() == game.getTeams().size() - 1;
    }
}
