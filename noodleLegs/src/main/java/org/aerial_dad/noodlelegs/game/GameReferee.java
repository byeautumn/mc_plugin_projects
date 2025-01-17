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
                team.displayTitle(ChatColor.RED + "Defeated!", ChatColor.RED + "Come back stronger ...");
                System.out.println("Team '" + team.getName() + "' is defeated.");
            } else {
                team.displayTitle(ChatColor.GREEN + "Victory!", ChatColor.GREEN + "Great job!");
                System.out.println("Team '" + team.getName() + "' wins the game.");
            }
        }
        game.terminate();
    }

    public boolean shouldRespawn(PlayerTracker playerTracker) {
        Team team = playerTracker.getCurrentTeam();
        if (null == team) {
            System.err.println("Team is null when checking if player should be respawn. There must be something wrong with the game status.");
            return false;
        }
        return !team.isBedBroken();
    }

    public boolean isDefeated(Team team) {
        return team.getEliminatedSet().size() == team.getRealPlayerCount();
    }

    public boolean shouldGameBeTerminated(Game game) {
        return game.getEliminatedTeams().size() == game.getTeams().size() - 1;
    }
}
