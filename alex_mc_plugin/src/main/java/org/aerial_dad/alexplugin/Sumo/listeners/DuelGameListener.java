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
    private final DuelGame game;
    private final double defeatYLevel;

    public DuelGameListener(DuelGame game) {
        this.game = game;
        defeatYLevel = game.getCenter().getY() - GameConstants.DEFEAT_Y_DIFF_VALUE;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(!this.game.isPlayerInGame(player)) {
            return;
        }

        double playerY = player.getLocation().getY();
        if (playerY <= defeatYLevel) {
            System.out.println("================== Player dropped below defeat level! ==================");
            if(this.game.getGameState() == DuelGame.DuelGameState.ONGOING || this.game.getGameState() == DuelGame.DuelGameState.COUNTING_DOWN) {
                this.game.defeat(player);
            } else {
                this.game.spawn(player);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if(!this.game.isPlayerInGame(player)) return;

        this.game.removePlayer(player);
        System.out.println("The Player is leaving minecraft: " + player.getDisplayName());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        DuelGame game = Global.getDuelGameByPlayer(player);
        if(null == game || game != this.game) return;

        Location fromLocation = event.getFrom(); // Location before teleport
        World fromWorld = fromLocation.getWorld();
        if(!this.game.getWorld().equals(fromWorld)) return;

        Location toLocation = event.getTo(); // Location after teleport
        if(toLocation == null || !fromWorld.equals(toLocation.getWorld())) {
            if(this.game.getGameState() == DuelGame.DuelGameState.ONGOING || this.game.getGameState() == DuelGame.DuelGameState.COUNTING_DOWN) {
                this.game.defeat(player);
            }
            else if (!this.game.isFull()) {
                this.game.removePlayer(player);
            } else {
                this.game.spawn(player);
            }
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        World fromWorld = player.getWorld();
        if(!fromWorld.equals(this.game.getWorld())) return;

        Location toLocation = event.getTo();
        DuelGame game = Global.getDuelGameByPlayer(player);
        if(this.game != game) return;

        if(toLocation == null || !fromWorld.equals(toLocation.getWorld())) {
            if(this.game.getGameState() == DuelGame.DuelGameState.ONGOING || this.game.getGameState() == DuelGame.DuelGameState.COUNTING_DOWN) {
                this.game.defeat(player);
            }
            else {
                this.game.removePlayer(player);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) { // Check if the damaged entity is a player
            if(this.game.isPlayerInGame(player)) {
                event.setDamage(0.0);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            ItemStack item = event.getItem();
            if(item == null || item.getItemMeta() == null) return;

            Player player = event.getPlayer();
            if(item.getItemMeta().getDisplayName().equals(GameConstants.GAME_ACTION_REPLAY_DISPLAY_NAME)) {
                this.game.removePlayer(player);
                DuelGame game = Global.getNextOpenGame();
                System.out.println("REPLAY was clicked.");
                if(game != null &&
                        !game.isFull() &&
                        game.getGameState() != DuelGame.DuelGameState.TERMINATION_SCHEDULED) {
                    game.admit(player);
                }
                else {
                    System.err.println("Server is full, the play cannot be admitted to a game: " + player.getDisplayName());
                    System.out.println("Player is sent back to lobby: " + player.getDisplayName());
                    player.teleport(Global.mainLobbyWorld.getSpawnLocation());
                }
            } else if(item.getItemMeta().getDisplayName().equals(GameConstants.GAME_ACTION_QUIT_DISPLAY_NAME)) {
                player.teleport(Global.mainLobbyWorld.getSpawnLocation());
                this.game.removePlayer(player);
                System.out.println("QUIT was clicked.");
            }

        }
    }

}
