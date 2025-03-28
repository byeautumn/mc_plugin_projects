package org.aerial_dad.noodlelegs.game;

import org.aerial_dad.noodlelegs.ShopConfig;
import org.aerial_dad.noodlelegs.Universe;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class GameLauncher {
    public final static String GAME_WORLD_KEYWORD = "Game_World_";
    private final GameType type;

    private final GameConfig config;

    private final Game game;

    private final GameQueue queue;

    private final World world;

    private static final Vector Queue_Spawn_Vector = new Vector(0, 119, 0);

    private static final String World_Default_Template_Name = "orchestra";

    public GameLauncher(String gameName, GameType gameType, GameConfig config){
        this.type = gameType;
        this.config = config;
        this.world = createWorld(GAME_WORLD_KEYWORD + gameName);

        this.game = createGame(gameType, gameName, this.world);
        Location queueLocation = new Location(this.world, Queue_Spawn_Vector.getX(), Queue_Spawn_Vector.getY(), Queue_Spawn_Vector.getZ());
        this.queue = createGameQueue(queueLocation);
    }

    private World createWorld(String worldName){
        boolean doesWorldExist = Universe.doesWorldExist(worldName);
        System.out.println("The world " + worldName + " exists: " + doesWorldExist);
        if(doesWorldExist) {
            return Bukkit.getWorld(worldName);
        }

        System.out.println("Creating world " + worldName + " from world template " + World_Default_Template_Name);
        return Universe.copyWorld(Bukkit.getWorld(World_Default_Template_Name), worldName);
    }

    private Game createGame(GameType gameType, String gameName, World world){
        // TODO
        return new Game(gameType, gameName, world, this.config, new GameReferee(this.config), new GameShop(ShopConfig.getInstance()));
    }

    private GameQueue createGameQueue(Location queueLocation){
        // TODO
        return new GameQueue(queueLocation);
    }

    public void queuePlayer(Player player) {
        // TODO
        if (!acceptNewPlayer()){
            System.out.println("Game is ongoing, request failed!" );
            System.out.println("Launcher did not accept " + player.getDisplayName());
            return;
        }
        player.getInventory().clear();
        player.setHealth(20.0);
        this.queue.addPlayer(player);
        System.out.println("Queuing player " + player.getDisplayName() + " into game " + this.game.getName());
        if(!acceptNewPlayer()) {
            launchGame();
        }

        PlayerTracker playerTracker = Universe.getPlayerTracker(player);
        playerTracker.update(this);
    }

    public boolean acceptNewPlayer() {
        // TODO
        int playerCountInQueue = this.queue.getPlayerCountInQueue();
        int totalPlayerAllowed = this.config.getPlayerCountPerTeam() * this.config.getTeamCount();
        return playerCountInQueue < totalPlayerAllowed;
    }

    public void launchGame(){
        if (this.game.getStatus() == GameStatus.TESTING) {
            System.out.println("Launching game " + this.game.getName() + " as TESTING mode.");
        }
        // Build teams and add teams into the game
        for (int idx = 0; idx < this.config.getTeamCount(); ++idx){
            List<Player> players = new ArrayList<>(this.config.getPlayerCountPerTeam());
            System.out.println("DEBUG: players in queue before creating teams: " + this.queue.printSelf());
            for (int idx1 = 0; idx1 < this.config.getPlayerCountPerTeam(); ++idx1){
                Player player = this.queue.pollPlayer();
                players.add(player);
            }
            String teamName = this.game.getName() + "_team_" + idx;
            Vector spawnVector = GameConfig.TYPE_TO_SPAWN_VECTOR_MAP.get(this.type).get(idx);
            Vector generationVector = GameConfig.TYPE_TO_GENERATOR_VECTOR_MAP.get(this.type).get(idx);
            Vector bedVector = GameConfig.TYPE_TO_BED_VECTOR_MAP.get(this.type).get(idx);
            Location bedLocation = new Location(this.world, bedVector.getX(), bedVector.getY(), bedVector.getZ());
            Team team = new Team(teamName, UUID.randomUUID(), this.game, players,
                    new Location(this.world, spawnVector.getX(), spawnVector.getY(), spawnVector.getZ()),
                    new Location(this.world, generationVector.getX(), generationVector.getY(), generationVector.getZ()),
                    bedLocation);
            this.game.addTeam(team);
            System.out.println("Team " + team.getName() + " has been created and its players include " + team.printPlayers());
        }
        System.out.println("DEBUG: print the queue after game starts: " + this.queue.printSelf());
        // Start the game
        System.out.println("The game " + this.game.getName() + " has been started! ");
        System.out.println("Adding game " + this.game.getName() + " to tracking system ...");
        Universe.trackGame(this, this.game);
        this.game.start();

    }

    public void launchGameWithTestingMode() {
        this.game.setStatus(GameStatus.TESTING);
        launchGame();
    }

//    public void updateGameTerminationStatus() {
//        Bukkit.unloadWorld(this.world.getName(), false);
//        this.updateLauncherTerminationStatus(this);
//    }

    public GameType getGameType() {
        return this.type;
    }

    public String printSelf() {
        StringBuffer sb = new StringBuffer();
        sb.append("{GameType: ").append(this.type.name()).append(", ");
        sb.append(this.config.printSelf()).append("}");
        return sb.toString();
    }
}
