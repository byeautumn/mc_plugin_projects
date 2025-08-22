package org.byeautumn.chuachua.game.firstland;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.game.firstland.FirstLandWorldConfigAccessor;

import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue; // Thread-safe queue
import java.util.logging.Level;

/**
 * A periodic task that orchestrates the creation of multiple worlds sequentially,
 * using Universe.createWorldAsync to prevent long server freezes.
 * This task runs on the main thread, but it only initiates one async world creation
 * at a time, ensuring responsiveness.
 */
public class WorldGenerationTask extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final FirstLandWorldConfigAccessor configAccessor;
    private final Player player;
    private final int totalWorldsToCreate;
    private final Queue<WorldCreationRequest> creationQueue; // Queue of worlds to create

    private int worldsInitiated = 0; // Number of worlds whose async creation has been started
    private int worldsCompleted = 0; // Number of worlds fully created and config updated
    private boolean isGeneratingCurrentWorld = false; // Flag to prevent multiple concurrent creations
    private final int initialStartingNumber; // For sequential naming

    /**
     * Constructs a new WorldGenerationTask.
     * @param plugin The main plugin instance.
     * @param configAccessor The accessor for world configuration.
     * @param player The player who initiated the command (for messages).
     * @param worldAmount The total number of worlds to create.
     */
    public WorldGenerationTask(JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor, Player player, int worldAmount) {
        this.plugin = plugin;
        this.configAccessor = configAccessor;
        this.player = player;
        this.totalWorldsToCreate = worldAmount;
        this.initialStartingNumber = configAccessor.findLowestAvailableWorldNumber();
        this.creationQueue = new ConcurrentLinkedQueue<>();

        // Populate the queue with all world creation requests upfront
        Random random = new Random();
        for (int i = 0; i < totalWorldsToCreate; i++) {
            long seed = random.nextLong();
            int nextWorldNumber = initialStartingNumber + i; // Assign sequential number based on initial count
            String formattedNumber = String.format("%02d", nextWorldNumber);
            String newWorldName = "First_Land_World_" + formattedNumber;
            UUID newWorldUUID = UUID.randomUUID(); // Generate unique UUID for each world
            creationQueue.add(new WorldCreationRequest(seed, newWorldName, newWorldUUID));
        }

        player.sendMessage(ChatColor.YELLOW + "Queueing " + totalWorldsToCreate + " worlds for generation...");
    }

    /**
     * This method runs periodically on the main thread. It initiates one world creation
     * at a time, making sure not to block the main thread unnecessarily.
     */
    @Override
    public void run() {
        // If all worlds are completed, or the queue is empty and no world is currently generating, cancel.
        if (worldsCompleted >= totalWorldsToCreate && !isGeneratingCurrentWorld) {
            cancelTaskAndNotify();
            return;
        }

        // If a world is currently being generated, we wait for its CompletableFuture to complete.
        // This ensures sequential processing and avoids overwhelming the main thread.
        if (isGeneratingCurrentWorld) {
            return;
        }

        // Dequeue the next world to create
        WorldCreationRequest request = creationQueue.poll();
        if (request == null) {
            // This should only happen if worldsCompleted < totalWorldsToCreate
            // but the queue somehow ran out before all worlds were successfully processed.
            // If the queue is truly empty and we haven't finished, there might be an issue.
            if (worldsCompleted < totalWorldsToCreate) {
                plugin.getLogger().warning("World generation queue unexpectedly empty before all worlds completed! Finalizing task.");
            }
            cancelTaskAndNotify(); // Finalize to prevent infinite runs if something went wrong
            return;
        }

        isGeneratingCurrentWorld = true; // Set flag: a world creation is in progress
        worldsInitiated++; // Increment count of initiated creations

        player.sendMessage(ChatColor.YELLOW + "Initiating generation for world " + worldsInitiated + "/" + totalWorldsToCreate + ": " + request.worldName + "...");

        // Call the asynchronous world creation method.
        // The blocking WorldCreator.createWorld() call will happen on the main thread
        // via Universe.createWorldAsync's internal BukkitRunnable, but this call itself
        // is non-blocking to *this* periodic task.
        Universe.createWorldAsync(request.seed, request.worldName, plugin).thenAccept(newlyCreatedChuaWorld -> {
            // This block runs on the MAIN THREAD after createWorldAsync's internal BukkitRunnable completes.
            if (newlyCreatedChuaWorld != null) {
                // Update config and cache on the main thread
                configAccessor.addNewWorld(
                        newlyCreatedChuaWorld.getID(),
                        request.worldName,
                        request.worldName, // Friendly name, can be customized later
                        null, // No player connected initially for batch creation
                        request.seed,
                        newlyCreatedChuaWorld.getWorld().getSpawnLocation()
                );
                worldsCompleted++; // Increment count of fully completed worlds
                player.sendMessage(ChatColor.GREEN + "World " + request.worldName + " created successfully (" + worldsCompleted + "/" + totalWorldsToCreate + ").");
            } else {
                plugin.getLogger().log(Level.WARNING, "Failed to create world: " + request.worldName);
                player.sendMessage(ChatColor.RED + "Failed to create world " + request.worldName + ". Skipping.");
            }
            isGeneratingCurrentWorld = false; // Reset flag: generation is complete for this world

            // Check if all worlds are done, if so, cancel the task.
            // This is important because the last world might finish between runs of this task.
            if (worldsCompleted >= totalWorldsToCreate) {
                cancelTaskAndNotify();
            }
        }).exceptionally(ex -> {
            // Handle any exceptions that occur during the async process
            plugin.getLogger().log(Level.SEVERE, "Error during async world creation for " + request.worldName + ": " + ex.getMessage(), ex);
            player.sendMessage(ChatColor.RED + "An error occurred while creating world " + request.worldName + ". This world will be skipped.");
            isGeneratingCurrentWorld = false; // Reset flag even on error

            // Even if one fails, we should still try to continue with other worlds or finalize.
            if (worldsCompleted + creationQueue.size() >= totalWorldsToCreate) { // If all are processed/attempted
                cancelTaskAndNotify();
            }
            return null; // Return null to complete exceptionally
        });
    }

    /**
     * Performs all final actions: cancels the task, sends completion messages,
     * saves config, and updates highest world number.
     * This method ensures these actions are run only once and on the main thread.
     */
    private void cancelTaskAndNotify() {
        if (!this.isCancelled()) { // Ensure it's not already cancelled
            this.cancel(); // Cancel the periodic runnable

            // Send completion titles and messages
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

            // Save config and update counters only ONCE at the very end
            configAccessor.saveConfig();
            configAccessor.updateAmountToHighestWorldNumber(); // Update based on all created worlds

            player.sendMessage(ChatColor.BLUE + "================================================");
            player.sendMessage(ChatColor.GREEN + "All " + worldsCompleted + " worlds have been successfully created!");
            player.sendMessage(ChatColor.BLUE + "================================================");
        }
    }
}