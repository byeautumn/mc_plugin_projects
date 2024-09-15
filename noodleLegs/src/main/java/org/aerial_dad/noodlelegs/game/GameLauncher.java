package org.aerial_dad.noodlelegs.game;

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
        this.world = createWorld("world_" + gameName);

        this.game = createGame(gameName, this.world);
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

    private Game createGame(String gameName, World world){
        // TODO
        return new Game(gameName, world, this.config, new GameReferee(this.config));
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
        // Build teams and add teams into the game
        for (int idx = 0; idx < this.config.getTeamCount(); ++idx){
            List<Player> players = new ArrayList<>(this.config.getPlayerCountPerTeam());
            for (int idx1 = 0; idx1 < this.config.getPlayerCountPerTeam(); ++idx1){
                Player player = this.queue.pollPlayer();
                players.add(player);
            }
            String teamName = this.game.getName() + "_team_" + idx;
            Vector spawnVector = GameConfig.TYPE_TO_SPAWN_VECTOR_MAP.get(this.type).get(idx);
            Team team = new Team(teamName, UUID.randomUUID(), this.game, players,
                    new Location(this.world, spawnVector.getX(), spawnVector.getY(), spawnVector.getZ()));
            this.game.addTeam(team);
            System.out.println("Team " + team.getName() + " has been created and its players include " + team.getPlayers());
        }
        // Start the game
        System.out.println("The game " + this.game.getName() + " has been started! ");

        this.game.start();

    }

//    public void updateGameTerminationStatus() {
//        Bukkit.unloadWorld(this.world.getName(), false);
//        this.updateLauncherTerminationStatus(this);
//    }

//    public List<BwTeam> buildTeams(Queue<BwPlayer> playerQueue, int teamSize, int teamCount) {
//        if (checkEnoughPlayers(playerQueue, teamSize, teamCount) == false ){
//            return null;
//        }
//
//        // Create and initialize a list to return - List<BwTeam> = new
//
//        List<BwTeam> bwTeams = new ArrayList<>();
//
//        // make a loop based on teamCount
//        for(int idx = 0; idx < teamCount; ++idx) {
//            BwTeam team = new BwTeam();
//            for (int idx1 = 0; idx1 < teamSize; ++idx1){
//                BwPlayer player = playerQueue.poll();
//                team.addPlayer(player);
//            }
//            bwTeams.add(team);
//        }
//        // Return the list
//        return bwTeams;
//    }
}
