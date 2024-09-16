package org.aerial_dad.noodlelegs.game;

import org.aerial_dad.noodlelegs.Universe;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Team {

    private final String name;

    private final UUID id;

    private final Game game;

    private final List<Player> players;

    private final Set<UUID> eliminatedSet = new HashSet<>();

    private final Location teamSpawnLocation;

    public Team(String name, UUID id, Game game, List<Player> players, Location teamSpawnLocation){
        this.name = name;
        this.id = id;
        this.game = game;
        this.players = players;
        this.teamSpawnLocation = teamSpawnLocation;
        updateTeamPlayerTrackers(PlayerStatus.InGame);
    }

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Location getTeamSpawnLocation() {
        return teamSpawnLocation;
    }

    public Set<UUID> getEliminatedSet() {
        return eliminatedSet;
    }

    public void spawn(){
        for (Player player : this.players){
            Universe.teleport(player, getTeamSpawnLocation());
        }
    }
//
//    public void remove(Player player){
//        getPlayers().remove(player);
//        updatePlayerTracker(player, PlayerStatus.Unknown);
//    }

    public void eliminate(Player player) {
        getEliminatedSet().add(player.getUniqueId());

        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);

        updatePlayerTracker(player, PlayerStatus.Unknown);
    }

    public void displayTitle(String title, String subTitle) {
        for (Player player : this.players) {
            player.sendMessage(title);
            player.sendTitle(title, subTitle);
        }
    }

    public void disband(){
        System.out.println("Disbanding team '" + getName() + "' ...");
        World lobby = Universe.getLobby();
        for (Player player : getPlayers()){
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        updateTeamPlayerTrackers(PlayerStatus.Unknown);

//        // Teleport to lobby
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException ie) {
//            System.err.println(ie.getStackTrace());
//        } finally {
//            for(Player player : getPlayers()) {
//                System.out.println("Player '" + player.getDisplayName() + "' is going to lobby.");
//                Universe.teleport(player, Universe.getLobby().getSpawnLocation());
//            }
//        }

    }

    public void updateTeamPlayerTrackers(PlayerStatus playerStatus) {
        // Once player is in a team, it is illegal to change player status to InQueue
        if (playerStatus == PlayerStatus.InQueue) {
            System.out.println("Update player tracker to an illegal status: " + playerStatus.name());
            return;
        }
        for(Player player : this.players) {
            updatePlayerTracker(player, playerStatus);
        }
    }

    public void updatePlayerTracker(Player player, PlayerStatus playerStatus) {
        // Once player is in a team, it is illegal to change player status to InQueue
        if (playerStatus == PlayerStatus.InQueue) {
            System.out.println("Update player tracker to an illegal status: " + playerStatus.name());
            return;
        }
        PlayerTracker playerTracker = Universe.getPlayerTracker(player);
        if(playerStatus == PlayerStatus.InGame) {
            playerTracker.update(playerStatus, this.game, this);
        } else if(playerStatus == PlayerStatus.Unknown) {
            playerTracker.update(playerStatus);
        }
    }
}
