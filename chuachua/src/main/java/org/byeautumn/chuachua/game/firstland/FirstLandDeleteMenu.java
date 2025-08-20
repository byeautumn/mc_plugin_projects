package org.byeautumn.chuachua.game.firstland;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList; // Import for unregistering listener
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.byeautumn.chuachua.Universe;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class FirstLandDeleteMenu implements Listener {
    private final Inventory inventory;
    private final JavaPlugin plugin;
    private final FirstLandWorldConfigAccessor configAccessor;
    private final Player menuOpener;
    private final FirstLandJoinMenu parentJoinMenu; // Added to store the parent menu instance

    private static final java.util.Map<java.util.UUID, java.util.UUID> pendingDeletions = new java.util.HashMap<>(); // Changed value type to UUID

    // New static items for the menu
    private static ItemStack BLANK_ITEM_GRAY; // Renamed for clarity
    private static ItemStack BLANK_ITEM_PINK; // New item for pink glass
    private static ItemStack BACK_ITEM;


    public FirstLandDeleteMenu(JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor, Player player, FirstLandJoinMenu parentJoinMenu) {
        this.plugin = plugin;
        this.configAccessor = configAccessor;
        this.menuOpener = player;
        this.parentJoinMenu = parentJoinMenu; // Initialize the parent menu instance

        // Create an inventory for displaying worlds to delete.
        this.inventory = Bukkit.createInventory(null, 36, ChatColor.DARK_RED + "Delete Your Worlds"); // Changed size to 36

        // Initialize static items if they haven't been already
        if (BLANK_ITEM_GRAY == null) {
            BLANK_ITEM_GRAY = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", ""); // Gray for general blanks
        }
        if (BLANK_ITEM_PINK == null) {
            BLANK_ITEM_PINK = createGuiItem(Material.PINK_STAINED_GLASS_PANE, " ", ""); // Pink for bottom row
        }
        if (BACK_ITEM == null) {
            BACK_ITEM = createGuiItem(Material.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Go back to main menu");
        }

        // Populate the menu with the player's worlds
        populateWorlds();
    }

    public void openInventory() {
        menuOpener.openInventory(inventory);
    }

    /**
     * Populates the inventory with items representing the player's owned worlds,
     * using UUIDs for identification in the lore.
     */
    private void populateWorlds() {
        inventory.clear(); // Clear existing items

        // Get the list of worlds owned by the player, now using UUIDs
        List<UUID> ownedWorldUUIDs = configAccessor.getPlayerOwnedWorldUUIDs(menuOpener.getUniqueId());

        if (ownedWorldUUIDs.isEmpty()) {
            ItemStack noWorldsItem = createGuiItem(Material.PAPER, ChatColor.GRAY + "No worlds to delete.",
                    ChatColor.DARK_GRAY + "You don't own any First Land worlds.");
            inventory.setItem(13, noWorldsItem); // Place in center
            // Set filler and back items even if no worlds
            fillEmptySlots();
            return;
        }

        int slot = 0;
        for (UUID worldUUID : ownedWorldUUIDs) { // Iterate using UUID
            String friendlyName = configAccessor.getWorldFriendlyName(worldUUID); // Get friendly name by UUID
            String internalName = configAccessor.getWorldName(worldUUID); // Get internal name by UUID

            // Use Material.RED_WOOL for deletable worlds to visually distinguish
            ItemStack worldItem = createGuiItem(Material.RED_WOOL,
                    ChatColor.RED + friendlyName,
                    ChatColor.GRAY + "ID: " + worldUUID.toString(), // Store World UUID in lore
                    ChatColor.GRAY + "Internal Name: " + internalName, // For debugging/information
                    "",
                    ChatColor.DARK_RED + "Click to select for deletion.");

            if (slot < inventory.getSize()) {
                inventory.setItem(slot++, worldItem);
            } else {
                plugin.getLogger().warning("Too many worlds for player " + menuOpener.getName() + " to display in FirstLandDeleteMenu!");
                break;
            }
        }
        fillEmptySlots(); // Fill remaining slots after adding worlds
    }

    /**
     * Fills the remaining inventory slots with BLANK_ITEMs and adds the BACK_ITEM.
     * Bottom row (slots 27-35) uses pink glass, others use gray.
     */
    private void fillEmptySlots() {
        // Fill slots with GRAY_STAINED_GLASS_PANE first (slots 0-26)
        for (int i = 0; i < 27; i++) { // Only iterate up to row before bottom
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                inventory.setItem(i, BLANK_ITEM_GRAY);
            }
        }
        // Fill the bottom row (slots 27-35) with PINK_STAINED_GLASS_PANE
        for (int i = 27; i < inventory.getSize(); i++) {
            inventory.setItem(i, BLANK_ITEM_PINK);
        }

        // Place BACK_ITEM at a specific slot in the bottom row
        inventory.setItem(31, BACK_ITEM); // Slot 31 is the middle of the bottom row (for a 36-slot inv)
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir() || !clickedItem.hasItemMeta() || !Objects.requireNonNull(clickedItem.getItemMeta()).hasDisplayName()) {
            return;
        }

        // Handle BACK_ITEM click
        if (clickedItem.equals(BACK_ITEM)) {
            player.closeInventory();
            HandlerList.unregisterAll(this); // Unregister this menu's listener
            parentJoinMenu.openInventory(player); // Open the parent menu
            return;
        }

        // Handle BLANK_ITEM click (for both gray and pink)
        if (clickedItem.equals(BLANK_ITEM_GRAY) || clickedItem.equals(BLANK_ITEM_PINK)) {
            return; // Do nothing for blank items
        }

        ItemMeta meta = clickedItem.getItemMeta();
        List<String> lore = meta.getLore();
        UUID targetWorldUUID = null; // Expect a UUID now

        if (lore != null) {
            for (String line : lore) {
                if (line.contains("ID: ")) { // Look for the UUID in the lore
                    try {
                        targetWorldUUID = UUID.fromString(ChatColor.stripColor(line.replace("ID: ", "").trim()));
                        break;
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID format found in lore for delete menu item: " + line);
                    }
                }
            }
        }

        if (targetWorldUUID != null) {
            // Store the world UUID for pending deletion
            pendingDeletions.put(player.getUniqueId(), targetWorldUUID);
            player.closeInventory();
            // Get friendly name using UUID for display
            openDeleteConfirmationMenu(player, targetWorldUUID, configAccessor.getWorldFriendlyName(targetWorldUUID));
        } else {
            player.sendMessage(ChatColor.RED + "Could not identify the world you clicked. Please try again.");
            new BukkitRunnable() {
                @Override
                public void run() {
                    populateWorlds(); // Refresh worlds in case something changed
                    openInventory();
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    /**
     * Opens a small confirmation menu for deleting a specific world.
     * Accepts a world UUID for identification.
     * @param player The player.
     * @param worldUUID The UUID of the world to delete.
     * @param worldFriendlyName The friendly name of the world to display.
     */
    private void openDeleteConfirmationMenu(Player player, UUID worldUUID, String worldFriendlyName) {
        Inventory confirmInv = Bukkit.createInventory(null, 9, ChatColor.DARK_RED + "Confirm Delete: " + worldFriendlyName);

        ItemStack confirmItem = createGuiItem(Material.LIME_WOOL, ChatColor.GREEN + "Confirm Delete",
                ChatColor.GRAY + "Click to permanently delete", ChatColor.GRAY + worldFriendlyName);

        ItemStack cancelItem = createGuiItem(Material.RED_WOOL, ChatColor.RED + "Cancel",
                ChatColor.GRAY + "Go back to the world list.");

        confirmInv.setItem(2, confirmItem);
        confirmInv.setItem(6, cancelItem);

        player.openInventory(confirmInv);
    }

    @EventHandler
    public void onConfirmMenuClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (!event.getView().getTitle().startsWith(ChatColor.DARK_RED + "Confirm Delete:")) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        UUID worldUUIDToDelete = pendingDeletions.get(player.getUniqueId());
        if (worldUUIDToDelete == null) {
            player.sendMessage(ChatColor.RED + "No world selected for deletion. Please try again.");
            player.closeInventory();
            HandlerList.unregisterAll(this); // Unregister this menu's listener
            new BukkitRunnable() {
                @Override
                public void run() {
                    FirstLandDeleteMenu.this.parentJoinMenu.openInventory(player); // Go back to parent menu
                }
            }.runTaskLater(plugin, 5L);
            return;
        }

        if (clickedItem.getType() == Material.LIME_WOOL && Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName().equals(ChatColor.GREEN + "Confirm Delete")) {
            player.closeInventory();
            HandlerList.unregisterAll(this); // Unregister this menu's listener

            // Retrieve friendly name BEFORE calling deletion
            String friendlyName = configAccessor.getWorldFriendlyName(worldUUIDToDelete);
            if (friendlyName == null) { // Fallback if friendly name is somehow not available
                friendlyName = worldUUIDToDelete.toString(); // Use UUID as fallback name
            }

            player.sendMessage(ChatColor.YELLOW + "Attempting to delete world: " + friendlyName + "...");

            UUID finalWorldUUIDToDelete = worldUUIDToDelete;
            String finalFriendlyName = friendlyName; // Make final for runnable
            new BukkitRunnable() {
                @Override
                public void run() {
                    Universe.removePlayerConnectedSpecificChuaWorld(player.getUniqueId(), worldUUIDToDelete);
                    performWorldDeletion(player, finalWorldUUIDToDelete, finalFriendlyName); // Pass UUID and friendly name
                    pendingDeletions.remove(player.getUniqueId());
                }
            }.runTask(plugin);

        } else if (clickedItem.getType() == Material.RED_WOOL && Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName().equals(ChatColor.RED + "Cancel")) {
            player.sendMessage(ChatColor.GRAY + "World deletion cancelled.");
            pendingDeletions.remove(player.getUniqueId());
            player.closeInventory();
            HandlerList.unregisterAll(this); // Unregister this menu's listener
            new BukkitRunnable() {
                @Override
                public void run() {
                    FirstLandDeleteMenu.this.parentJoinMenu.openInventory(player); // Go back to parent menu
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    private ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        List<String> loreList = new ArrayList<>(Arrays.asList(lore));
        meta.setLore(loreList);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Handles the actual deletion of the world.
     * This method must be called on the main server thread.
     * @param player The player initiating the deletion.
     * @param worldUUIDToDelete The UUID of the world to delete.
     * @param friendlyName The friendly name of the world (retrieved prior to deletion attempt).
     */
    private void performWorldDeletion(Player player, UUID worldUUIDToDelete, String friendlyName) {
        // Call Universe.deleteFirstLandWorld which handles:
        // - Getting the internal world name (if needed for Bukkit operations)
        // - Unloading the world and teleporting players
        // - Clearing connections from Universe's in-memory map
        // - Deleting the config entry via configAccessor
        // - Deleting the world folder
        boolean deletionSuccess = Universe.deleteFirstLandWorld(plugin, worldUUIDToDelete, configAccessor);

        if (deletionSuccess) {
            player.sendMessage(ChatColor.GREEN + "World '" + friendlyName + "' has been successfully deleted!");
            plugin.getLogger().info("Successfully completed full deletion process for world (UUID: " + worldUUIDToDelete + ")");
        } else {
            // This message will appear if Universe.deleteFirstLandWorld returned false,
            // which could happen if the world wasn't found or an I/O error occurred during folder deletion.
            player.sendMessage(ChatColor.RED + "Failed to delete world '" + friendlyName + "'. Check console for details.");
            plugin.getLogger().log(Level.SEVERE, "Deletion failed for world (UUID: " + worldUUIDToDelete + ")");
        }

        // After deletion attempt, refresh the menu to reflect changes
        new BukkitRunnable() {
            @Override
            public void run() {
                FirstLandDeleteMenu.this.populateWorlds(); // Re-populate the menu with current worlds
                FirstLandDeleteMenu.this.openInventory(); // Re-open the menu
            }
        }.runTaskLater(plugin, 5L);
    }

    /**
     * Recursively deletes a directory and its contents.
     * This is a critical operation and should be used with extreme caution.
     * This method is kept for completeness but is now only potentially used if
     * Universe.deleteFirstLandWorld does not handle the file deletion directly
     * or if you have other scenarios needing direct folder deletion.
     * @param path The file or directory to delete.
     * @return true if successful, false otherwise.
     * @throws IOException if an I/O error occurs.
     */
    private boolean deleteWorldFolder(File path) throws IOException {
        if (!path.exists()) return true;

        if (path.isDirectory()) {
            File[] files = path.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!deleteWorldFolder(file)) {
                        return false;
                    }
                }
            }
        }
        return path.delete();
    }
}
