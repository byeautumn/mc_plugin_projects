package org.aerial_dad.noodlelegs.game;

import org.aerial_dad.noodlelegs.Universe;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NoodleListener implements Listener {

    @EventHandler
    private void onPlayerDamageEvent(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            PlayerTracker playerTracker = Universe.getPlayerTracker(player);
            if (playerTracker.getCurrentStatus() != PlayerStatus.InGame) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerTracker playerTracker = Universe.getPlayerTracker(player);
        if (playerTracker.getCurrentStatus() == PlayerStatus.InGame) {
            Game currGame = playerTracker.getCurrentGame();
            if (null == currGame) {
                System.err.println("Player " + player.getDisplayName() + " is currently in a game, but the game cannot be found.");
                return;
            }
            currGame.checkPlayerElimination(playerTracker);
        }

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerTracker playerTracker = Universe.getPlayerTracker(player);

        System.out.println("The Player is leaving minecraft: " + player.getDisplayName());

        if (playerTracker.getCurrentStatus() == PlayerStatus.InGame) {
            System.out.println("Start game quiting process for player " + player.getDisplayName());

            Game currGame = playerTracker.getCurrentGame();
            if (null == currGame) {
                System.err.println("Player " + player.getDisplayName() + " is currently in a game, but the game cannot be found.");
                return;
            }
            currGame.checkPlayerQuit(playerTracker);
        }
        else if (playerTracker.getCurrentStatus() == PlayerStatus.InQueue) {
            System.out.println("Start game queue quiting process for player " + player.getDisplayName());

            GameQueue currQueue = playerTracker.getCurrentGameQueue();
            if (null == currQueue) {
                System.err.println("Player " + player.getDisplayName() + " is currently in a game queue, but the queue cannot be found.");
                return;
            }
            currQueue.removePlayer(player);
        }

    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        World fromWorld = event.getFrom().getWorld(); // Location before teleport
        World toWorld = event.getTo().getWorld();
        checkPlayerPortal(player, fromWorld, toWorld);
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        World fromWorld = event.getFrom().getWorld(); // Location before teleport
        World toWorld = event.getTo().getWorld();
        checkPlayerPortal(player, fromWorld, toWorld);
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        System.out.println("Player '" + player.getDisplayName() + "' is going to lobby.");

        World lobby = Universe.getLobby();
        Universe.teleport(player, lobby.getSpawnLocation());
    }

    private void checkPlayerPortal(Player player, World fromWorld, World toWorld) {
        System.out.println("Player " + player.getDisplayName() + " is onPlayerTeleport ...");
        PlayerTracker playerTracker = Universe.getPlayerTracker(player);
        if (playerTracker.getCurrentStatus() == PlayerStatus.InGame) {
            Game currGame = playerTracker.getCurrentGame();
            if (null == currGame) {
                System.err.println("Player " + player.getDisplayName() + " is currently in a game, but the game cannot be found.");
                return;
            }
            if(!fromWorld.getName().equals(currGame.getWorld().getName())) {
                System.err.println("Player " + player.getDisplayName() + " is currently in world '"
                        + fromWorld.getName() + "' which is not in the game world '"
                        + currGame.getWorld().getName() + "'.");
                return;
            }
            if(toWorld.getName().equals(fromWorld.getName())) {
                System.out.println("Player " + player.getDisplayName() + " is teleporting in the same world '" + fromWorld + "'.");
                return;
            }
            currGame.checkPlayerQuit(playerTracker);
        }
        else if (playerTracker.getCurrentStatus() == PlayerStatus.InQueue) {
            GameQueue gameQueue = playerTracker.getCurrentGameQueue();
            gameQueue.removePlayer(player);
        }
    }
}


