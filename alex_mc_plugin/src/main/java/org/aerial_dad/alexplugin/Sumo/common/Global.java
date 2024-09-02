package org.aerial_dad.alexplugin.Sumo.common;

import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.*;

public class Global {
    public static List<DuelGame> duelGames = new ArrayList<>();
    public static List<World> prebuiltDuelGameWorlds;

    public static World mainLobbyWorld;

    public static Map<UUID, DuelGame> playerToDuelGameMap =new HashMap<>();

    public static void loadPrebuiltDuelGameWorlds(List<World> worlds) {
        prebuiltDuelGameWorlds = worlds;
    }

    public static void loadMainLobbyWorld(World mainLobby) {
        mainLobbyWorld = mainLobby;
    }

    public static void addDuelGames(List<World> worlds) {
        for(World world : worlds) {
            if(null != world) {
                System.out.println("Constructing DuelGame with world " + world.getName());
            }
            duelGames.add(new DuelGame(world));
        }
    }
    public static DuelGame getNextOpenGame() {
        System.out.println("prebuiltWorlds is null: " + (null == prebuiltDuelGameWorlds));

        for(DuelGame game : Global.duelGames) {
            System.out.println("Game world " + game.getWorld().getName() + " | is full ? " + game.isFull() + " | " + game.getGameState().name());
            if(!game.isFull() && game.getGameState() != DuelGame.DuelGameState.TERMINATION_SCHEDULED) {
                System.out.println(game.getWorld().getName() + " is picked.");
                return game;
            }

        }
        System.err.println("All games are full, please wait in the lobby ...");
        return null;
    }

    public static DuelGame getDuelGameByPlayer(@Nonnull Player player) {

        if (!playerToDuelGameMap.containsKey(player.getUniqueId())) {
//            System.err.println("The Duel Game cannot be found by the player " + player.getDisplayName());
            printPlayerToDuelGameMap();
            return null;
        }

        return playerToDuelGameMap.get(player.getUniqueId());
    }

    private static void printPlayerToDuelGameMap() {
        for(Map.Entry<UUID, DuelGame> entry : playerToDuelGameMap.entrySet()) {
            System.out.println("[ " + entry.getKey() + " - " + entry.getValue().getWorld().getName() + " ]");
        }
    }
}
