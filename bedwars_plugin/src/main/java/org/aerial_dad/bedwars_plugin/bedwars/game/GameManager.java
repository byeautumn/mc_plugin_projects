package org.aerial_dad.bedwars_plugin.bedwars.game;

import org.aerial_dad.bedwars_plugin.bedwars.game.Constants.GameConfig;
import org.aerial_dad.bedwars_plugin.bedwars.game.Teams.BwTeam;
import org.aerial_dad.bedwars_plugin.bedwars.game.Teams.TeamManager;
import org.bukkit.World;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Queue;

public class GameManager {



    private BwGame createGame(@Nonnull World world, @Nonnull Queue<BwPlayer>Waiting_Queue, @Nonnull GameConfig config) {
        List<BwTeam> tm = new TeamManager().buildTeams(Waiting_Queue, config.getSizePerTeam(), config.getTeamCount());
        BwWorld wd = new BwWorld(world, world.getName());
        BwGame newGame = new BwGame(tm, wd );


        return newGame;
    }
}
