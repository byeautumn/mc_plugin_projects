package org.aerial_dad.alexplugin.Sumo.listeners;

import org.aerial_dad.alexplugin.Sumo.common.DuelGame;
import org.aerial_dad.alexplugin.Sumo.common.Global;
import org.aerial_dad.alexplugin.Sumo.common.constants.GameConstants;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class DuelGameListener implements Listener {
//    private final DuelGame game;
//    private final double defeatYLevel;

//    public DuelGameListener(DuelGame game) {
//        game = game;
//        defeatYLevel = game.getCenter().getY() - GameConstants.DEFEAT_Y_DIFF_VALUE;
//    }

    public DuelGameListener() {
    }

    private double getDefeatYLevel(DuelGame game) {
        return game.getCenter().getY() - GameConstants.DEFEAT_Y_DIFF_VALUE;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        DuelGame game = Global.getDuelGameByPlayer(player);
        if(null == game || !game.isPlayerInGame(player)) return;

        double playerY = player.getLocation().getY();
        double defeatYLevel = getDefeatYLevel(game);
        if (playerY <= defeatYLevel) {
            System.out.println("================== Player dropped below defeat level! ==================");
            if(game.getGameState() == DuelGame.DuelGameState.ONGOING || game.getGameState() == DuelGame.DuelGameState.COUNTING_DOWN) {
                game.defeat(player);
            } else {
                game.spawn(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DuelGame game = Global.getDuelGameByPlayer(player);
        if(null == game || !game.isPlayerInGame(player)) return;

        game.removePlayer(player);
        System.out.println("The Player is leaving minecraft: " + player.getDisplayName());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        DuelGame game = Global.getDuelGameByPlayer(player);
        if(null == game || !game.isPlayerInGame(player)) return;

        System.out.println("Player " + player.getDisplayName() + " is onPlayerTeleport ...");
        Location fromLocation = event.getFrom(); // Location before teleport
        World fromWorld = fromLocation.getWorld();
        if(!game.getWorld().equals(fromWorld)) return;

        Location toLocation = event.getTo(); // Location after teleport
        if(toLocation == null || !fromWorld.equals(toLocation.getWorld())) {
            if(game.getGameState() == DuelGame.DuelGameState.ONGOING || game.getGameState() == DuelGame.DuelGameState.COUNTING_DOWN) {
                System.out.println("Game " + game.getWorld().getName() + " is defeating " + player.getDisplayName());
                game.defeat(player);
            }
            else if (!game.isFull()) {
                System.out.println("Game " + game.getWorld().getName() + " is removing " + player.getDisplayName());
                game.removePlayer(player);
            } else {
                System.out.println("Game " + game.getWorld().getName() + " is spawning " + player.getDisplayName());
                game.spawn(player);
            }
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        DuelGame game = Global.getDuelGameByPlayer(player);
        if(null == game || !game.isPlayerInGame(player)) return;

        System.out.println("Player " + player.getDisplayName() + " is onPlayerPortal ...");

        World fromWorld = player.getWorld();
        if(!fromWorld.equals(game.getWorld())) return;

        Location toLocation = event.getTo();

        if(toLocation == null || !fromWorld.equals(toLocation.getWorld())) {
            if(game.getGameState() == DuelGame.DuelGameState.ONGOING || game.getGameState() == DuelGame.DuelGameState.COUNTING_DOWN) {
                System.out.println("Game " + game.getWorld().getName() + " is defeating " + player.getDisplayName());
                game.defeat(player);
            }
            else {
                System.out.println("Game " + game.getWorld().getName() + " is removing " + player.getDisplayName());
                game.removePlayer(player);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) { // Check if the damaged entity is a player
            DuelGame game = Global.getDuelGameByPlayer(player);
            if(null == game || !game.isPlayerInGame(player)) return;

            if(game.isPlayerInGame(player)) {
                event.setDamage(0.0);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        DuelGame game = Global.getDuelGameByPlayer(player);
        if(null == game || !game.isPlayerInGame(player)) return;

        if(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            ItemStack item = event.getItem();
            if(item == null || item.getItemMeta() == null) return;


            if(item.getItemMeta().getDisplayName().equals(GameConstants.GAME_ACTION_REPLAY_DISPLAY_NAME)) {
                DuelGame nextGame = Global.getNextOpenGame();


                System.out.println("REPLAY was clicked.");
                if(nextGame != null &&
                        !nextGame.isFull() &&
                        nextGame.getGameState() != DuelGame.DuelGameState.TERMINATION_SCHEDULED) {
                    System.out.println("Player " + player.getDisplayName() + " is joining game " + nextGame.getWorld().getName());
                    nextGame.admit(player);

                }
                else {
                    System.err.println("Server is full, the play cannot be admitted to a game: " + player.getDisplayName());
                    System.out.println("Player is sent back to lobby: " + player.getDisplayName());
                    player.teleport(Global.mainLobbyWorld.getSpawnLocation());
                }
            } else if(item.getItemMeta().getDisplayName().equals(GameConstants.GAME_ACTION_QUIT_DISPLAY_NAME)) {
                player.teleport(Global.mainLobbyWorld.getSpawnLocation());

                System.out.println("QUIT was clicked.");
            }
            // Remove player need to be after the next game is picked; otherwise the last player will remain in the original game
            game.removePlayer(player);

        }
    }

}
