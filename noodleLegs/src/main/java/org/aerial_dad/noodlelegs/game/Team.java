package org.aerial_dad.noodlelegs.game;

import org.aerial_dad.noodlelegs.NoodleLegs;
import org.aerial_dad.noodlelegs.SpawnNpc;
import org.aerial_dad.noodlelegs.Universe;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Team {

    private static final Vector SPAWN_GENERATOR_OFFSET = new Vector(0, 0, 5);

    private final String name;

    private final UUID id;

    private final Game game;

    private final List<Player> players;

    private final Set<UUID> eliminatedSet = new HashSet<>();

    private final Location teamSpawnLocation;

    private ShopNpc shopNpc;


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

    public int getRealPlayerCount() {
        int count = 0;
        for(Player player : getPlayers()) {
            if(null != player) {
                ++count;
            }
        }
        return count;
    }

    public Location getTeamSpawnLocation() {
        return teamSpawnLocation;
    }

    public Set<UUID> getEliminatedSet() {
        return eliminatedSet;
    }

    public void setShopNpc(ShopNpc shopNpc) {
        this.shopNpc = shopNpc;
    }

    public void spawn(){
        System.out.println("Team " + getName() + " is spawning players: " + printPlayers());
        for (Player player : this.players){
            if (null == player) {
                if (this.game.getStatus() != GameStatus.TESTING) {
                    System.err.println("null player was found in a non-testing game.");
                }
                continue;
            }
            System.out.println("Teleporting player " + player.getDisplayName() + " when team spawning.");
            Universe.teleport(player, getTeamSpawnLocation());
        }
        if(this.shopNpc != null) {
            this.shopNpc.spawn(getTeamSpawnLocation());
        }

    }

    public void startResourceGeneration() {
        final ResourceGenerator generator = new ResourceGenerator();
        Location generatiorLocation = getTeamSpawnLocation().add(SPAWN_GENERATOR_OFFSET);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(NoodleLegs.getInstance, new Runnable() {
            @Override
            public void run() {
                generator.generate(generatiorLocation);
            }
        }, 0L, 40L);
    }
//
//    public void remove(Player player){
//        getPlayers().remove(player);
//        updatePlayerTracker(player, PlayerStatus.Unknown);
//    }

    public void eliminate(Player player) {
        System.out.println("Eliminating player " + player.getDisplayName());
        getEliminatedSet().add(player.getUniqueId());

        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);

        updatePlayerTracker(player, PlayerStatus.Unknown);
    }

    public void displayTitle(String title, String subTitle) {
        for (Player player : this.players) {
            if (null == player) {
                if (this.game.getStatus() != GameStatus.TESTING) {
                    System.err.println("null player was found in a non-testing game.");
                }
                continue;
            }
            player.sendMessage(title);
            player.sendTitle(title, subTitle);
        }
    }

    public void disband(){
        System.out.println("Disbanding team '" + getName() + "' ...");

        for (Player player : getPlayers()){
            if (null == player) {
                if (this.game.getStatus() != GameStatus.TESTING) {
                    System.out.println("Game status is " + this.game.getStatus().name());
                    System.err.println("null player was found in a non-testing game.");
                }
                continue;
            }
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        updateTeamPlayerTrackers(PlayerStatus.Unknown);
        if(this.shopNpc != null) {
            this.shopNpc.release();
        }
        System.out.println("Team " + getName() + " is disbanded.");

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

        if (null == player) {
            if (this.game.getStatus() != GameStatus.TESTING) {
                System.err.println("null player was found in a non-testing game.");
            }
            return;
        }

        PlayerTracker playerTracker = Universe.getPlayerTracker(player);
        if(playerStatus == PlayerStatus.InGame) {
            playerTracker.update(playerStatus, this.game, this);
        } else if(playerStatus == PlayerStatus.Unknown) {
            playerTracker.update(playerStatus);
        }
    }

    public String printPlayers() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        if (null != getPlayers()) {
            for (Player player : getPlayers()) {
                sb.append((null == player ? null : player.getDisplayName())).append(" | ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
