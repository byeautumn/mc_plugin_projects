package org.byeautumn.chuachua.game.firstland;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
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
    // Updated to use the new accessor
    private final Player menuOpener;
    private final FirstLandJoinMenu parentJoinMenu;

    private static final java.util.Map<java.util.UUID, java.util.UUID> pendingDeletions = new java.util.HashMap<>();

    private static ItemStack BLANK_ITEM_GRAY;
    private static ItemStack BLANK_ITEM_PINK;
    private static ItemStack BACK_ITEM;

    // Updated constructor to accept the new WorldDataAccessor.getInstance()
    public FirstLandDeleteMenu(JavaPlugin plugin, Player player, FirstLandJoinMenu parentJoinMenu) {
        this.plugin = plugin;
        this.menuOpener = player;
        this.parentJoinMenu = parentJoinMenu;

        this.inventory = Bukkit.createInventory(null, 36, ChatColor.DARK_RED + "Delete Your Worlds");

        if (BLANK_ITEM_GRAY == null) {
            BLANK_ITEM_GRAY = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "");
        }
        if (BLANK_ITEM_PINK == null) {
            BLANK_ITEM_PINK = createGuiItem(Material.PINK_STAINED_GLASS_PANE, " ", "");
        }
        if (BACK_ITEM == null) {
            BACK_ITEM = createGuiItem(Material.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Go back to main menu");
        }

        populateWorlds();
    }

    public void openInventory() {
        menuOpener.openInventory(inventory);
    }

    private void populateWorlds() {
        inventory.clear();

        // Updated to use the new accessor method
        List<UUID> ownedWorldUUIDs = WorldDataAccessor.getInstance().getPlayerOwnedWorldUUIDs(menuOpener.getUniqueId());

        if (ownedWorldUUIDs.isEmpty()) {
            ItemStack noWorldsItem = createGuiItem(Material.PAPER, ChatColor.GRAY + "No worlds to delete.",
                    ChatColor.DARK_GRAY + "You don't own any First Land worlds.");
            inventory.setItem(13, noWorldsItem);
            fillEmptySlots();
            return;
        }

        int slot = 0;
        for (UUID worldUUID : ownedWorldUUIDs) {
            // Get WorldData object to retrieve friendly and internal names
            WorldData worldData = WorldDataAccessor.getInstance().getWorldData(worldUUID);

            if (worldData != null) {
                String friendlyName = worldData.getWorldFriendlyName();
                String internalName = worldData.getWorldInternalName();

                ItemStack worldItem = createGuiItem(Material.RED_WOOL,
                        ChatColor.RED + friendlyName,
                        ChatColor.GRAY + "ID: " + worldUUID.toString(),
                        ChatColor.GRAY + "Internal Name: " + internalName,
                        "",
                        ChatColor.DARK_RED + "Click to select for deletion.");

                if (slot < inventory.getSize()) {
                    inventory.setItem(slot++, worldItem);
                } else {
                    plugin.getLogger().warning("Too many worlds for player " + menuOpener.getName() + " to display in FirstLandDeleteMenu!");
                    break;
                }
            }
        }
        fillEmptySlots();
    }

    private void fillEmptySlots() {
        for (int i = 0; i < 27; i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                inventory.setItem(i, BLANK_ITEM_GRAY);
            }
        }
        for (int i = 27; i < inventory.getSize(); i++) {
            inventory.setItem(i, BLANK_ITEM_PINK);
        }

        inventory.setItem(31, BACK_ITEM);
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

        if (clickedItem.equals(BACK_ITEM)) {
            player.closeInventory();
            HandlerList.unregisterAll(this);
            parentJoinMenu.openInventory(player);
            return;
        }

        if (clickedItem.equals(BLANK_ITEM_GRAY) || clickedItem.equals(BLANK_ITEM_PINK)) {
            return;
        }

        ItemMeta meta = clickedItem.getItemMeta();
        List<String> lore = meta.getLore();
        UUID targetWorldUUID = null;

        if (lore != null) {
            for (String line : lore) {
                if (line.contains("ID: ")) {
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
            pendingDeletions.put(player.getUniqueId(), targetWorldUUID);
            player.closeInventory();
            WorldData worldData = WorldDataAccessor.getInstance().getWorldData(targetWorldUUID);
            String friendlyName = (worldData != null) ? worldData.getWorldFriendlyName() : "Unknown";
            openDeleteConfirmationMenu(player, targetWorldUUID, friendlyName);
        } else {
            player.sendMessage(ChatColor.RED + "Could not identify the world you clicked. Please try again.");
            new BukkitRunnable() {
                @Override
                public void run() {
                    populateWorlds();
                    openInventory();
                }
            }.runTaskLater(plugin, 5L);
        }
    }

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
        // Basic checks
        Player player = (Player) event.getWhoClicked();

        // Log the inventory click event
        plugin.getLogger().info("Player " + player.getName() + " clicked in a menu.");

        if (!event.getView().getTitle().startsWith(ChatColor.DARK_RED + "Confirm Delete:")) {
            return;
        }
        event.setCancelled(true);

        // Log that a confirm menu was clicked and cancelled
        plugin.getLogger().info("Player " + player.getName() + " clicked the Confirm Delete menu.");

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) {
            plugin.getLogger().info("Player " + player.getName() + " clicked an empty slot.");
            return;
        }

        UUID worldUUIDToDelete = pendingDeletions.get(player.getUniqueId());
        if (worldUUIDToDelete == null) {
            plugin.getLogger().warning("Player " + player.getName() + " attempted to click a confirm menu without a pending world deletion UUID.");
            player.sendMessage(ChatColor.RED + "No world selected for deletion. Please try again.");
            closeAndReturnToParentMenu(player);
            return;
        }

        // Log the world UUID that's about to be acted on
        plugin.getLogger().info("Found pending deletion UUID for " + player.getName() + ": " + worldUUIDToDelete);

        // Handle the confirm action

        if (clickedItem.getType() == Material.LIME_WOOL && Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName().equals(ChatColor.GREEN + "Confirm Delete")) {
            plugin.getLogger().info("Player " + player.getName() + " confirmed world deletion for UUID: " + worldUUIDToDelete);
            player.closeInventory();

            new BukkitRunnable() {
                @Override
                public void run() {
                    handleDeletionConfirmation(player, worldUUIDToDelete);

                }
            }.runTask(plugin); // Ensure deletion and post-deletion tasks run on main thread

            // Handle the cancel action
        } else if (clickedItem.getType() == Material.RED_WOOL && Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName().equals(ChatColor.RED + "Cancel")) {
            plugin.getLogger().info("Player " + player.getName() + " cancelled world deletion for UUID: " + worldUUIDToDelete);
            player.sendMessage(ChatColor.GRAY + "World deletion cancelled.");
            pendingDeletions.remove(player.getUniqueId());
            closeAndReturnToParentMenu(player);
        }
    }

    // Helper method to handle the confirmation logic
    private void handleDeletionConfirmation(Player player, UUID worldUUIDToDelete) {
        // Log the start of the deletion process
        plugin.getLogger().info("Starting deletion process for world with UUID: " + worldUUIDToDelete);

        WorldData worldData = WorldDataAccessor.getInstance().getWorldData(worldUUIDToDelete);
        String friendlyName = (worldData != null) ? worldData.getWorldFriendlyName() : "Unknown";

        player.sendMessage(ChatColor.YELLOW + "Attempting to delete world: " + friendlyName + "...");

        boolean deletionSuccess = Universe.deleteFirstLandWorld(
                plugin,
                worldUUIDToDelete
        );

        // Log the outcome of the deletion attempt
        if (deletionSuccess) {
            plugin.getLogger().info("Deletion of world '" + friendlyName + "' (UUID: " + worldUUIDToDelete + ") succeeded.");
            player.sendMessage(ChatColor.GREEN + "World '" + friendlyName + "' has been successfully deleted!");
            plugin.getLogger().info("Successfully completed full deletion process for world (UUID: " + worldUUIDToDelete + ")");
        } else {
            plugin.getLogger().severe("Deletion of world '" + friendlyName + "' (UUID: " + worldUUIDToDelete + ") failed.");
            player.sendMessage(ChatColor.RED + "Failed to delete world '" + friendlyName + "'. Check console for details.");
            plugin.getLogger().log(Level.SEVERE, "Deletion failed for world (UUID: " + worldUUIDToDelete + ")");
        }

        pendingDeletions.remove(player.getUniqueId());

        new BukkitRunnable() {
            @Override
            public void run() {
                FirstLandDeleteMenu.this.populateWorlds();
                FirstLandDeleteMenu.this.openInventory();
            }
        }.runTaskLater(plugin, 5L);
    }

    // Helper method to close the inventory and return to the parent menu
    private void closeAndReturnToParentMenu(Player player) {
        player.closeInventory();
        new BukkitRunnable() {
            @Override
            public void run() {
                FirstLandDeleteMenu.this.parentJoinMenu.openInventory(player);
            }
        }.runTaskLater(plugin, 5L);
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