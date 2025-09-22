package org.byeautumn.chuachua.game;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent; // Import for PlayerQuitEvent
import org.bukkit.inventory.ItemStack;
import org.byeautumn.chuachua.Chuachua;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.common.PlayMode;
import org.byeautumn.chuachua.game.firstland.FirstLandWorldConfigAccessor;
import org.byeautumn.chuachua.game.firstland.WorldDataAccessor;
import org.byeautumn.chuachua.game.firstland.WorldGenerationTask;
import org.byeautumn.chuachua.generate.world.pipeline.ChuaWorld; // Import for ChuaWorld
import org.byeautumn.chuachua.player.InventoryDataAccessor;
import org.byeautumn.chuachua.player.PlayerData;
import org.byeautumn.chuachua.player.PlayerDataAccessor;
import org.byeautumn.chuachua.player.matrix.PlayerActivityMatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class GameListener implements Listener {
    private static final Block GAME_ENTRANCE_BLOCK = Universe.getLobby().getBlockAt(24, -59, 14);

    private final WorldDataAccessor configAccessor;
    private final PlayerDataAccessor playerDataAccessor;
    private final Chuachua plugin;
    private final InventoryDataAccessor inventoryDataAccessor;

    public GameListener(Chuachua plugin, WorldDataAccessor configAccessor, PlayerDataAccessor playerDataAccessor, InventoryDataAccessor inventoryDataAccessor) {
        this.plugin = plugin;
        this.configAccessor = configAccessor;
        this.playerDataAccessor = playerDataAccessor;
        this.inventoryDataAccessor = inventoryDataAccessor;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        if (clickedBlock.getState() instanceof Sign) {
            Sign sign = (Sign) clickedBlock.getState();
            String firstLine = ChatColor.stripColor(sign.getLine(0));

            // Logic to open the GUI if the sign says "[Join Game]"
            if (firstLine.equalsIgnoreCase("[Join Game]")) {
                plugin.getFirstLandMenu().openInventory(event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        System.out.println("Player " + player.getDisplayName() + " is just going to lobby.");

        // Always teleport the player to the lobby first
        Universe.teleportToLobby(player, playerDataAccessor, inventoryDataAccessor);

        // Attempt to load the player's data for the lobby world.
        PlayerData playerData = playerDataAccessor.getPlayerData(playerUUID, Universe.getLobby().getUID(), Universe.getLobby().getName());

        if (playerData != null) {
            // Data exists, so load the player's saved state
            player.setHealth(playerData.getHealth());
            player.setFoodLevel(playerData.getHunger());
            Universe.getPlayerTracker(player).setPlayMode(playerData.getPlayMode());
            player.setGameMode(playerData.getGameMode());
            player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "Welcome back! Your data has been loaded.");
            System.out.println("Loaded player data for " + player.getDisplayName() + ".");
        } else {
            // Player is joining for the first time or their data doesn't exist.
            // Create new default data.
            PlayerData newPlayerData = PlayerData.builder()
                    .playerUUID(playerUUID)
                    .worldUUID(Universe.getLobby().getUID())
                    .worldInternalName(Universe.getLobby().getName())
                    .playMode(PlayMode.UNKNOWN) // Default play mode
                    .gameMode(GameMode.ADVENTURE) // Default game mode
                    .health(player.getHealth())
                    .hunger(player.getFoodLevel())
                    // Assuming you have other methods to get default values for these stats
                    .build();

            // Save the new data. This will create the directory if it doesn't exist.
            playerDataAccessor.savePlayerData(newPlayerData);
            player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "Welcome! Your player data has been initialized.");
            System.out.println("Game mode is set to ADVENTURE for new player " + player.getDisplayName() + ".");
        }

        // Always reset the player tracker if not an op
        if (!player.isOp()) {
            Universe.resetPlayerTracker(player);

            if (playerData.getPlayMode() == PlayMode.EDIT) {
                System.out.println(player.getUniqueId() + "was in Edit mode change them to unknown");
                Universe.getPlayerTracker(player).setPlayMode(PlayMode.UNKNOWN);
            }
            if (playerData.getGameMode() == GameMode.CREATIVE || playerData.getGameMode() == GameMode.SPECTATOR) {
                System.out.println(player.getUniqueId() + "was in Game mode " + playerData.getGameMode() + "changing them to ADVENTURE");
                player.setGameMode(GameMode.ADVENTURE);
            }
        } else {
            player.sendMessage("* " + ChatColor.YELLOW + "You are op-ed and have automatically been set into " + ChatColor.AQUA + "'EDIT'" + ChatColor.YELLOW + " mode.");
            Universe.getPlayerTracker(player).setPlayMode(playerData.getPlayMode());
            player.setGameMode(playerData.getGameMode());
        }

        System.out.println("Game mode is set to " + player.getGameMode() + " for player " + player.getDisplayName() + ".");
    }

    @EventHandler
    private void onPlayerQuitEvent(PlayerQuitEvent event) {
        // Get the player instance from the event
        Player player = event.getPlayer();

        // Step 1: Get the player's current state from the Bukkit API
        double currentHealth = player.getHealth();
        int currentHunger = player.getFoodLevel();

        // Get the player's current location to save
        Location lastKnownLocation = player.getLocation();

        // Save the player's inventory
        inventoryDataAccessor.saveInventory(player.getUniqueId(), player.getWorld().getUID().toString(), player.getInventory().getContents());
        // Step 2: Create a PlayerData object with the current state, including the last known log-off location

        playerDataAccessor.updatePlayerData(player);
    }

    @EventHandler
    private void onLostHungerEvent(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (player.getWorld().getUID().equals(Universe.getLobby().getUID())) {
                player.setSaturation(20.0f);
                player.setFoodLevel(20);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onPlayerLoseHealthEvent(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (player.getWorld().getUID().equals(Universe.getLobby().getUID())) {
                player.setHealth(20);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onPlayerDamageOtherEvent(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getEntity();
            if (damager.getWorld().getUID().equals(Universe.getLobby().getUID())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onPlayerChangedWorldEvent(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorldName = event.getFrom().getName();
        String toWorldName = player.getWorld().getName();

        // 1. Save the inventory from the old world
        inventoryDataAccessor.saveInventory(player.getUniqueId(), fromWorldName, player.getInventory().getContents());

        // 2. Load the inventory for the new world and set it directly
        ItemStack[] newInventory = inventoryDataAccessor.loadInventory(player.getUniqueId(), toWorldName);
        player.getInventory().clear();
        player.getInventory().setContents(newInventory);
        player.updateInventory(); // This ensures the client sees the change
    }
}