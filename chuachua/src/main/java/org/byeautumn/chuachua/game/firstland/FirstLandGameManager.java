package org.byeautumn.chuachua.game.firstland;

import org.bukkit.World;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FirstLandGameManager {

    // Singleton pattern to ensure only one manager instance exists
    private static final FirstLandGameManager INSTANCE = new FirstLandGameManager();

    private final Map<UUID, FirstLandGame> activeGames = new HashMap<>();

    // Private constructor prevents direct instantiation
    private FirstLandGameManager() {}

    public static FirstLandGameManager getInstance() {
        return INSTANCE;
    }

    /**
     * Gets an existing game for a world or creates a new one if it doesn't exist.
     * @param worldData The data for the world.
     * @return The active FirstLandGame instance.
     */
    public FirstLandGame getOrCreateGame(WorldData worldData) {
        UUID worldUUID = worldData.getWorldUUID(); // Assuming WorldData has this method

        // Check if a game for this world already exists
        if (activeGames.containsKey(worldUUID)) {
            System.out.println("Retrieved existing FirstLandGame for world: " + worldUUID);
            return activeGames.get(worldUUID);
        }

        // If not, create a new one, store it, and start it
        System.out.println("Creating new FirstLandGame for world: " + worldUUID);
        FirstLandGame newGame = new FirstLandGame(worldData);
        activeGames.put(worldUUID, newGame);

        // Crucial: Start the game's matrix check only once
        if (!newGame.getGameStatus().equals(GameStatus.ONLINE)){
            newGame.start();
        }

        return newGame;
    }

    /**
     * Removes and stops a game instance when the world is unloaded or the game ends.
     * @param worldUUID The UUID of the world to stop the game for.
     */
    public void stopAndRemoveGame(UUID worldUUID) {
        FirstLandGame game = activeGames.remove(worldUUID);
        if (game != null) {
            System.out.println("Stopping and removing FirstLandGame for world: " + worldUUID);
            game.pauseMatrixChecking(); // Cancel the repeating task
        }
    }

    // You might also want a method to get a game if you know it exists
    public FirstLandGame getGame(UUID worldUUID) {
        return activeGames.get(worldUUID);
    }
}