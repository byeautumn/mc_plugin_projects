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
    private final FirstLandWorldConfigAccessor configAccessor;
    private final Player menuOpener;
    private final FirstLandJoinMenu parentJoinMenu;

    private static final java.util.Map<java.util.UUID, java.util.UUID> pendingDeletions = new java.util.HashMap<>();

    private static ItemStack BLANK_ITEM_GRAY;
    private static ItemStack BLANK_ITEM_PINK;
    private static ItemStack BACK_ITEM;


    public FirstLandDeleteMenu(JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor, Player player, FirstLandJoinMenu parentJoinMenu) {
        this.plugin = plugin;
        this.configAccessor = configAccessor;
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

        List<UUID> ownedWorldUUIDs = configAccessor.getPlayerOwnedWorldUUIDs(menuOpener.getUniqueId());

        if (ownedWorldUUIDs.isEmpty()) {
            ItemStack noWorldsItem = createGuiItem(Material.PAPER, ChatColor.GRAY + "No worlds to delete.",
                    ChatColor.DARK_GRAY + "You don't own any First Land worlds.");
            inventory.setItem(13, noWorldsItem);
            fillEmptySlots();
            return;
        }

        int slot = 0;
        for (UUID worldUUID : ownedWorldUUIDs) {
            String friendlyName = configAccessor.getWorldFriendlyName(worldUUID);
            String internalName = configAccessor.getWorldName(worldUUID);

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
            openDeleteConfirmationMenu(player, targetWorldUUID, configAccessor.getWorldFriendlyName(targetWorldUUID));
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
            new BukkitRunnable() {
                @Override
                public void run() {
                    FirstLandDeleteMenu.this.parentJoinMenu.openInventory(player);
                }
            }.runTaskLater(plugin, 5L);
            return;
        }

        // --- COMPLETED LOGIC START ---
        if (clickedItem.getType() == Material.LIME_WOOL && Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName().equals(ChatColor.GREEN + "Confirm Delete")) {
            player.closeInventory();

            String friendlyName = configAccessor.getWorldFriendlyName(worldUUIDToDelete);
            if (friendlyName == null) {
                friendlyName = worldUUIDToDelete.toString(); // Fallback if friendly name is not found
            }

            player.sendMessage(ChatColor.YELLOW + "Attempting to delete world: " + friendlyName + "...");

            UUID finalWorldUUIDToDelete = worldUUIDToDelete;
            String finalFriendlyName = friendlyName;
            new BukkitRunnable() {
                @Override
                public void run() {
                    boolean deletionSuccess = Universe.deleteFirstLandWorld(
                            plugin,
                            finalWorldUUIDToDelete,
                            configAccessor
                    );

                    if (deletionSuccess) {
                        player.sendMessage(ChatColor.GREEN + "World '" + finalFriendlyName + "' has been successfully deleted!");
                        plugin.getLogger().info("Successfully completed full deletion process for world (UUID: " + finalWorldUUIDToDelete + ")");
                    } else {
                        player.sendMessage(ChatColor.RED + "Failed to delete world '" + finalFriendlyName + "'. Check console for details.");
                        plugin.getLogger().log(Level.SEVERE, "Deletion failed for world (UUID: " + finalWorldUUIDToDelete + ")");
                    }

                    pendingDeletions.remove(player.getUniqueId()); // Clear pending deletion

                    // Refresh the menu after the deletion attempt
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            FirstLandDeleteMenu.this.populateWorlds();
                            FirstLandDeleteMenu.this.openInventory();
                        }
                    }.runTaskLater(plugin, 5L);
                }
            }.runTask(plugin); // Ensure deletion and post-deletion tasks run on main thread

        } else if (clickedItem.getType() == Material.RED_WOOL && Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName().equals(ChatColor.RED + "Cancel")) {
            player.sendMessage(ChatColor.GRAY + "World deletion cancelled.");
            pendingDeletions.remove(player.getUniqueId()); // Clear pending deletion
            player.closeInventory();

            // Return to the main delete menu
            new BukkitRunnable() {
                @Override
                public void run() {
                    FirstLandDeleteMenu.this.populateWorlds();
                    FirstLandDeleteMenu.this.openInventory();
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
}