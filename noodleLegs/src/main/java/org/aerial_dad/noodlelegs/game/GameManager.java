package org.aerial_dad.noodlelegs.game;

import org.aerial_dad.noodlelegs.Universe;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class GameManager {
    private static final GameManager instance = new GameManager();

    private Map<GameType, GameOrganizer> typeToOrganizerMap;

    private final Map<GameType, GameConfig> typeToConfigMap = new HashMap<GameType, GameConfig>() {
        {
            put(GameType.BW_1V1, new GameConfig(2, 1));
            put(GameType.BW_2V2, new GameConfig(2, 2));
        }
    };

    private GameManager() {}

    public static GameManager getInstance() {
        return instance;
    }

    public void queuePlayer(GameType type, Player player){
        // TODO
        if (this.typeToOrganizerMap == null) {
            this.typeToOrganizerMap = new HashMap<>();
        }

        if(!this.typeToOrganizerMap.containsKey(type)) {
            this.typeToOrganizerMap.put(type, new GameOrganizer(type, this.typeToConfigMap.get(type)));
        }

        getGameOrganizer(type).queuePlayer(player);

    }

    private GameOrganizer getGameOrganizer(GameType type) {
        return this.typeToOrganizerMap.get(type);
    }

    public void removeGameLauncher(GameLauncher gameLauncher) {
        getGameOrganizer(gameLauncher.getGameType()).removeGameLauncher(gameLauncher);
    }
}
