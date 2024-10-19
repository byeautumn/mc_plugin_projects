package org.aerial_dad.noodlelegs.game;

import org.aerial_dad.noodlelegs.NoodleLegs;
import org.aerial_dad.noodlelegs.SpawnNpc;
import org.aerial_dad.noodlelegs.Universe;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.*;

public class Game {

    private final World world;

    private final GameConfig config;

    private final GameReferee referee;

    private GameStatus status = GameStatus.PREGAME;

    private List<Team> teams;

    private final Set<UUID> eliminatedTeams = new HashSet<>();;

    private final String name;

    public Game(String name, World world, GameConfig config, GameReferee referee){
        this.name = name;
        this.world = world;
        this.config = config;
        this.referee = referee;
    }

    public World getWorld() {
        return world;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public void addTeam(Team team){
        if(this.teams == null){
            this.teams = new ArrayList<>(this.config.getTeamCount());
        }
        this.teams.add(team);
    }

    public String getName() {
        return name;
    }

    public GameReferee getReferee() {
        return referee;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public Set<UUID> getEliminatedTeams() {
        return eliminatedTeams;
    }

    public void start(){
        if (this.status == GameStatus.INGAME) {
            System.err.println("This game status is '" + getStatus().name() + "' so it cannot be started again.");
            return;
        }
        for (Team team : this.teams){
            ShopNpc shopNpc = new ShopNpc("Item_shop");
            team.setShopNpc(shopNpc);
            team.spawn();
        }
        this.status = GameStatus.INGAME;

    }

    public void terminate() {
        setStatus(GameStatus.POSTGAME);
        System.out.println("Game " + this.name + " has just been terminated.");
        for (Team team : getTeams()) {
            team.disband();
        }
    }

    public void checkPlayerElimination(PlayerTracker playerTracker) {
        if(getReferee().shouldRespawn(playerTracker)) {
            // TODO
            return;
        }
        checkGameTermination(playerTracker);
    }

    public void checkPlayerQuit(PlayerTracker playerTracker) {
        checkGameTermination(playerTracker);
    }

    private void checkGameTermination(PlayerTracker playerTracker) {
        GameOrganizer gameOrganizer = playerTracker.getCurrentGameOrganizer();
        Team targetTeam = playerTracker.getCurrentTeam();
        targetTeam.eliminate(playerTracker.getPlayer());
        if(getReferee().isDefeated(targetTeam)) {
            System.out.println("Team '" + targetTeam.getName() + "' is defeated.");
            updateTeamDefeatedStatus(targetTeam);
            if(getReferee().shouldGameBeTerminated(this)) {
                System.out.println("Game is over now.");
                if(null == gameOrganizer) {
                    System.err.println("Cannot find game organizer when a game being terminated.");
                }
                else {
                    gameOrganizer.removeGameLauncher(playerTracker.getCurrentGameLauncher());
                }

                getReferee().judge(this);

                // Reset world
                Universe.softResetWorld(getWorld());
            }
        }
    }

    private void updateTeamDefeatedStatus(Team team) {
        getEliminatedTeams().add(team.getId());
    }

}
