package org.byeautumn.chuachua.game.firstland;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.bukkit.inventory.meta.SkullMeta; // Import for player heads
import org.bukkit.profile.PlayerProfile; // For setting head texture
import org.bukkit.profile.PlayerTextures; // For getting/setting textures

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class FirstLandViewMenu implements Listener {
    private final Inventory inventory;
    private final JavaPlugin plugin;
    private final FirstLandWorldConfigAccessor configAccessor;
    private final Player menuOpener;
    private final FirstLandJoinMenu parentJoinMenu;

    // Static items for the menu
    private static ItemStack BLANK_ITEM_GENERAL_GRAY_GLASS; // Gray Stained Glass Pane for general blanks
    private static ItemStack BLANK_ITEM_PINK; // Pink Stained Glass Pane for bottom row
    private static ItemStack BACK_ITEM;
    private static ItemStack OWNED_WORLD_DISPLAY_ITEM; // Grass block for owned worlds (main display)
    private static ItemStack AVAILABLE_SLOT_DISPLAY_ITEM; // Paper for available slots (main display)
    private static ItemStack AVAILABLE_SLOTS_HEAD_ITEM; // Player head for overall slot count display

    public FirstLandViewMenu(JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor, Player player, FirstLandJoinMenu parentJoinMenu) {
        this.plugin = plugin;
        this.configAccessor = configAccessor;
        this.menuOpener = player;
        this.parentJoinMenu = parentJoinMenu;

        this.inventory = Bukkit.createInventory(null, 36, ChatColor.DARK_AQUA + "Your First Land Worlds");

        // Initialize static items
        if (BLANK_ITEM_GENERAL_GRAY_GLASS == null) {
            BLANK_ITEM_GENERAL_GRAY_GLASS = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "");
        }
        if (BLANK_ITEM_PINK == null) {
            BLANK_ITEM_PINK = createGuiItem(Material.PINK_STAINED_GLASS_PANE, " ", "");
        }
        if (BACK_ITEM == null) {
            BACK_ITEM = createGuiItem(Material.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Go back to main menu");
        }
        // Initialize the new slot indicator items for the main display area
        if (OWNED_WORLD_DISPLAY_ITEM == null) {
            OWNED_WORLD_DISPLAY_ITEM = createGuiItem(Material.GRASS_BLOCK, ChatColor.GREEN + "Owned World Slot", ChatColor.GRAY + "Represents a world you own.");
        }
        if (AVAILABLE_SLOT_DISPLAY_ITEM == null) {
            AVAILABLE_SLOT_DISPLAY_ITEM = createGuiItem(Material.PAPER, ChatColor.WHITE + "Available World Slot", ChatColor.GRAY + "Represents a slot you can use for a new world.", ChatColor.DARK_GREEN + "Click to create a new world!");
        }
        // Initialize the player head for displaying world slots
        if (AVAILABLE_SLOTS_HEAD_ITEM == null) {
            AVAILABLE_SLOTS_HEAD_ITEM = createPlayerHeadItem(
                    ChatColor.AQUA + "World Slots",
                    menuOpener // Pass the player object here to set their head
            );
        }

        populateWorlds();
    }

    /**
     * Opens this specific instance of the world view inventory for the player.
     */
    public void openInventory() {
        menuOpener.openInventory(inventory);
    }

    /**
     * Populates the inventory with items representing the player's owned worlds.
     * Each item displays the friendly name, creation date, and a "Click to join" lore.
     * It also dynamically displays owned/available slots using grass blocks and paper in the main area.
     */
    private void populateWorlds() {
        inventory.clear();

        List<UUID> ownedWorldUUIDs = configAccessor.getPlayerOwnedWorldUUIDs(menuOpener.getUniqueId());
        int ownedWorldsCount = ownedWorldUUIDs.size();
        int maxWorlds = configAccessor.getMaxWorldsPerPlayer();
        int availableSlotsCount = maxWorlds - ownedWorldsCount;

        // Update the lore of the AVAILABLE_SLOTS_HEAD_ITEM
        ItemMeta slotsMeta = AVAILABLE_SLOTS_HEAD_ITEM.getItemMeta();
        List<String> slotsLore = new ArrayList<>();
        slotsLore.add(ChatColor.GRAY + "You own: " + ChatColor.YELLOW + ownedWorldsCount + ChatColor.GRAY + " world(s)");
        slotsLore.add(ChatColor.GRAY + "Max worlds: " + ChatColor.YELLOW + maxWorlds);
        slotsLore.add(ChatColor.GRAY + "Available slots: " + ChatColor.GREEN + availableSlotsCount);
        slotsMeta.setLore(slotsLore);
        AVAILABLE_SLOTS_HEAD_ITEM.setItemMeta(slotsMeta);

        int currentDisplaySlot = 0; // Tracks the current slot for world/slot items

        // Place AVAILABLE_SLOTS_HEAD_ITEM at slot 0 (top-left)
        inventory.setItem(27, AVAILABLE_SLOTS_HEAD_ITEM);

        // Place actual world items (grass blocks) in the top 3 rows (starting from slot 1)
        if (!ownedWorldUUIDs.isEmpty()) {
            for (UUID worldUUID : ownedWorldUUIDs) {
                // Ensure we don't go past the main display area (slots 1-26 for interactive content)
                if (currentDisplaySlot >= 27) {
                    plugin.getLogger().warning("Too many worlds for player " + menuOpener.getName() + " to display in FirstLandViewMenu main area!");
                    break;
                }

                String friendlyName = configAccessor.getWorldFriendlyName(worldUUID);
                String internalWorldName = configAccessor.getWorldName(worldUUID);
                long createdAt = configAccessor.getConfig().getLong("worlds." + worldUUID.toString() + ".created-at", 0);
                String creationDate = (createdAt > 0) ? new Date(createdAt).toString() : "Unknown Date";

                ItemStack worldItem = createGuiItem(Material.GRASS_BLOCK,
                        ChatColor.YELLOW + friendlyName,
                        ChatColor.GRAY + "ID: " + worldUUID.toString(),
                        ChatColor.GRAY + "Internal Name: " + internalWorldName,
                        ChatColor.GRAY + "Created on: " + creationDate,
                        "",
                        ChatColor.GREEN + "Click to join!");

                inventory.setItem(currentDisplaySlot++, worldItem);
            }
        }

        // Place available slot display items (paper) after owned worlds, up to maxWorlds
        // Ensure they also stay within the top 3 rows (slots 1-26)
        // Also ensure not to place if there are no more slots available.
        for (int i = ownedWorldsCount; i < maxWorlds; i++) {
            if (currentDisplaySlot >= 27) { // Stop if we've filled the main display area
                break;
            }
            inventory.setItem(currentDisplaySlot++, AVAILABLE_SLOT_DISPLAY_ITEM);
        }

        // Fill remaining slots in the top 3 rows (1-26) with general blank items (gray stained glass)
        for (int i = 1; i < 27; i++) { // Start from 1, as 0 is taken by the head
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                inventory.setItem(i, BLANK_ITEM_GENERAL_GRAY_GLASS);
            }
        }

        fillBottomRowAndStaticItems(); // Fill the bottom row and place static items
    }

    /**
     * Fills the bottom row (slots 27-35) with PINK_STAINED_GLASS_PANE,
     * and places the BACK_ITEM.
     */
    private void fillBottomRowAndStaticItems() {
        // Fill the entire bottom row (slots 27-35) with PINK_STAINED_GLASS_PANE
        for (int i = 28; i < inventory.getSize(); i++) {
            inventory.setItem(i, BLANK_ITEM_PINK);
        }

        // Place BACK_ITEM in its specific slot, overwriting pink glass
        inventory.setItem(31, BACK_ITEM); // Slot 31 (middle of the bottom row)
        // AVAILABLE_SLOTS_HEAD_ITEM is now placed at slot 0 in populateWorlds()
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        int clickedSlot = event.getSlot(); // Get the clicked slot

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

        // Handle BLANK_ITEM click (for general gray stained glass and pink glass)
        if (clickedItem.equals(BLANK_ITEM_GENERAL_GRAY_GLASS) || clickedItem.equals(BLANK_ITEM_PINK)) {
            return; // Do nothing for blank items
        }

        // Handle OWNED_WORLD_DISPLAY_ITEM click (not interactive, just display)
        if (clickedItem.equals(OWNED_WORLD_DISPLAY_ITEM)) {
            return;
        }

        // Handle AVAILABLE_SLOT_DISPLAY_ITEM (Paper) click - now initiates world creation
        // Ensure this is explicitly the AVAILABLE_SLOT_DISPLAY_ITEM and not just any paper.
        if (clickedItem.equals(AVAILABLE_SLOT_DISPLAY_ITEM)) {
            player.closeInventory();
            HandlerList.unregisterAll(this); // Unregister this menu's listener

            // Check if player has available slots before starting naming process
            int ownedWorldsCount = configAccessor.getPlayerOwnedWorldUUIDs(player.getUniqueId()).size();
            int maxWorlds = configAccessor.getMaxWorldsPerPlayer();
            if (ownedWorldsCount < maxWorlds) {
                FirstLandWorldNameListener.startNamingProcess(player, configAccessor);
            } else {
                player.sendMessage(ChatColor.RED + "You have reached your maximum world limit (" + maxWorlds + ").");
                // Optionally, re-open the menu after a short delay so player can see this message
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        populateWorlds();
                        openInventory();
                    }
                }.runTaskLater(plugin, 5L);
            }
            return;
        }

        // Handle AVAILABLE_SLOTS_HEAD_ITEM click (just a display, no action)
        // Check if the clicked item is specifically our AVAILABLE_SLOTS_HEAD_ITEM
        if (clickedSlot == 0 && clickedItem.getType() == Material.PLAYER_HEAD && clickedItem.hasItemMeta() &&
                clickedItem.getItemMeta().getDisplayName().equals(ChatColor.AQUA + "World Slots")) {
            return; // Do nothing, it's just for display
        }


        ItemMeta meta = clickedItem.getItemMeta();
        List<String> lore = meta.getLore();
        UUID targetWorldUUID = null;

        // Only process clicks on actual world items (those with "ID: " in their lore)
        if (lore != null) {
            for (String line : lore) {
                if (line.contains("ID: ")) {
                    try {
                        String uuidString = ChatColor.stripColor(line.replace("ID: ", "").trim());
                        targetWorldUUID = UUID.fromString(uuidString);
                        break;
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID format found in lore: " + line);
                        player.sendMessage(ChatColor.RED + "Error: Invalid world ID. Please report this issue.");
                        return;
                    }
                }
            }
        }

        if (targetWorldUUID != null) {
            player.closeInventory();
            HandlerList.unregisterAll(this); // Unregister this menu's listener
            UUID finalTargetWorldUUID = targetWorldUUID;
            Universe.connectPlayerToSpecificWorld(player, plugin, configAccessor, Universe.getChuaWorldById(finalTargetWorldUUID).getWorld().getName());
        } else {
            // This else block handles clicks on non-interactive items that don't have "ID: " lore
            // or if the UUID parsing failed. It also covers the "No worlds found" item.
            player.sendMessage(ChatColor.RED + "That is not a world you can join. Please click on a world to join.");
            new BukkitRunnable() {
                @Override
                public void run() {
                    populateWorlds(); // Re-populate to refresh or correct display
                    openInventory();
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    /**
     * Helper method to create a generic GUI item with a specific material, name, and lore.
     * @param material The material of the item.
     * @param name The display name of the item.
     * @param lore An array of strings for the item's lore.
     * @return The created ItemStack.
     */
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
     * Helper method to create a player head item with a custom display name.
     * It uses the provided Player object to set the owner, displaying their actual head.
     * @param name The display name of the head.
     * @param player The Player whose head should be displayed.
     * @return The created ItemStack representing a player head.
     */
    private ItemStack createPlayerHeadItem(final String name, final Player player) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();

        meta.setDisplayName(name);
        meta.setOwningPlayer(player); // Set the owner profile to the actual player's profile

        head.setItemMeta(meta);
        return head;
    }
}
