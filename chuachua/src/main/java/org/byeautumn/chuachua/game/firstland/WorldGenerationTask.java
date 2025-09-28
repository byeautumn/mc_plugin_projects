package org.byeautumn.chuachua.game.firstland;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.generate.world.pipeline.ChuaWorld;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

/**
 * A periodic task that orchestrates the creation of multiple worlds sequentially.
 * This task runs on the main thread and creates one world per tick to prevent server lag.
 */
public class WorldGenerationTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final Player player;
    private final Player ownerPlayer;
    private final int totalWorldsToCreate;
    private final Queue<WorldCreationRequest> creationQueue;

    private int worldsCompleted = 0;
    private int worldsInitiated = 0;

    /**
     * Constructor for the task.
     *
     * @param plugin The main plugin instance.
     * @param ownerPlayer The ownerPlayer who initiated the task.
     * @param totalWorldsToCreate The number of worlds to create.
     */
    public WorldGenerationTask(JavaPlugin plugin, Player ownerPlayer, int totalWorldsToCreate, UUID ownerUUID, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.ownerPlayer = ownerPlayer;
        this.totalWorldsToCreate = totalWorldsToCreate;
        this.creationQueue = new ConcurrentLinkedQueue<>();

        // Generate all the world creation requests upfront
        Random random = new Random();
        for (int i = 0; i < totalWorldsToCreate; i++) {
            long seed = random.nextLong();
            // Sanitize the internal name and make it unique by adding the loop counter
            String internalName = "First_Land_World_" + Universe.sendFormattedTimeMessage().replace('/', '_').replace(' ', '_').replace(':', '-') + "_" + i;
            String friendlyName = "First Land World " + (i + 1);

            creationQueue.add(new WorldCreationRequest(seed, internalName, friendlyName, ownerUUID));
        }

        if (player != null) {
            player.sendMessage(ChatColor.GRAY + "A total of " + totalWorldsToCreate + " worlds have been queued for creation.");
        }
    }

    /**
     * The main run method for the BukkitRunnable. This is called on the main thread every tick.
     */
    @Override
    public void run() {
        // If the queue is empty, we are done
        if (creationQueue.isEmpty()) {
            cancelAndNotify();
            return;
        }

        // Process one world creation request per run (per tick)
        final WorldCreationRequest request = creationQueue.poll();
        if (request == null) {
            return;
        }

        worldsInitiated++;

        try {
            // This part runs on the main server thread!
            ChuaWorld createdChuaWorld = Universe.createWorld(request.seed, request.worldName, plugin);

            if (createdChuaWorld != null) {
                // Get the actual UUID assigned by Bukkit
                UUID actualWorldUUID = createdChuaWorld.getID();

                if (request.ownerUUID != null) {
                    WorldDataAccessor.getInstance().createWorldData(
                            actualWorldUUID,
                            request.ownerUUID,
                            request.worldFriendlyName,
                            request.worldName,
                            request.seed,
                            Collections.singletonList(request.ownerUUID)
                    );
                } else {
                    WorldDataAccessor.getInstance().createWorldData(
                            actualWorldUUID,
                            null,
                            "Unowned World",
                            request.worldName,
                            request.seed,
                            new ArrayList<>()
                    );
                }

                worldsCompleted++;

                // Provide a ownerPlayer notification
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.GREEN + "✅ World " + worldsCompleted + "/" + totalWorldsToCreate + " created: " + request.worldFriendlyName);
                }
            } else {
                plugin.getLogger().log(Level.SEVERE, "Failed to create world: " + request.worldFriendlyName);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "❌ Failed to create world " + worldsInitiated + "/" + totalWorldsToCreate + ". See console for details.");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create world " + request.worldFriendlyName, e);
            if (player != null && player.isOnline()) {
                player.sendMessage(ChatColor.RED + "❌ Failed to create world " + worldsInitiated + "/" + totalWorldsToCreate + ". See console for details.");
            }
            // Ensure the task stops if a critical error occurs
            this.cancel();
        }
    }

    /**
     * Cancels the task and notifies all ownerPlayers that the generation is complete.
     */
    public void cancelAndNotify() {
        if (!this.isCancelled()) {
            this.cancel();

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle(
                        ChatColor.GREEN + "" + ChatColor.BOLD + "Generation Complete!",
                        ChatColor.AQUA + "The server is running as normal.",
                        10, 70, 20
                );
            }
            Bukkit.broadcastMessage(ChatColor.DARK_AQUA + "§l=======================================");
            Bukkit.broadcastMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "    ✅ World Generation Complete! ✅");
            Bukkit.broadcastMessage(ChatColor.AQUA + "       The server should now be running as normal.");
            Bukkit.broadcastMessage(ChatColor.DARK_AQUA + "§l=======================================");

            if (player != null) {
                player.sendMessage(ChatColor.BLUE + "=================================================");
                player.sendMessage(ChatColor.GREEN + "All " + worldsCompleted + " worlds have been successfully created!");
                player.sendMessage(ChatColor.BLUE + "=================================================");
            }
        }
    }

    /**
     * Simple data class to hold world creation request details.
     */
    private static class WorldCreationRequest {
        final long seed;
        final String worldName;
        final String worldFriendlyName;
        final UUID ownerUUID;

        WorldCreationRequest(long seed, String worldName, String worldFriendlyName, UUID ownerUUID) {
            this.seed = seed;
            this.worldName = worldName;
            this.worldFriendlyName = worldFriendlyName;
            this.ownerUUID = ownerUUID;
        }
    }
}