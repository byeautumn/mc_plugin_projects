package org.aerial_dad.noodlelegs.game;

import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameConfig {

    private final int teamCount;

    private final int playerCountPerTeam;

    public static final Map<GameType, List<Vector>> TYPE_TO_SPAWN_VECTOR_MAP = new HashMap<GameType, List<Vector>>(){
        {
            put(GameType.BW_1V1, Arrays.asList(new Vector(-29, 82, 94),
                                                new Vector(31, 82, 94)));
            put(GameType.BW_2V2, Arrays.asList(new Vector(-29, 82, 94),
                                                new Vector(31, 82, 94)));
        }
    };

    public static final Map<GameType, List<Vector>> TYPE_TO_GENERATOR_VECTOR_MAP = new HashMap<GameType, List<Vector>>(){
        {
            put(GameType.BW_1V1, Arrays.asList(new Vector(-29.5, 81, 97.5),
                                                new Vector(30.5, 81, 97.5)));
            put(GameType.BW_2V2, Arrays.asList(new Vector(-29.5, 81, 97.5),
                                                new Vector(30.5, 81, 97.5)));
        }
    };

    public GameConfig(int teamCount, int playerCountPerTeam){
        this.teamCount = teamCount;
        this.playerCountPerTeam = playerCountPerTeam;
    }

    public int getTeamCount() {
        return teamCount;
    }

    public int getPlayerCountPerTeam() {
        return playerCountPerTeam;
    }

    public String printSelf() {
        return "[" + getTeamCount() + " | " + getPlayerCountPerTeam() + "]";
    }
}
