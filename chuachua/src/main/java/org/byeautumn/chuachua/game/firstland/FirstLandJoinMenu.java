package org.byeautumn.chuachua.game.firstland;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.byeautumn.chuachua.player.PlayerDataAccessor;

import java.util.*;

public class FirstLandJoinMenu implements Listener {
    private final Inventory inventory;
    private final JavaPlugin plugin;
    // Removed configAccessor as it is now redundant
    private final WorldDataAccessor worldDataAccessor;
    private final PlayerDataAccessor playerDataAccessor;


    private final ItemStack YOUR_WORLDS_ITEM;
    private final ItemStack CREATE_NAMED_WORLD_ITEM;
    private final ItemStack DELETE_WORLD_ITEM;
    private final ItemStack FILLER_ITEM;

    // Cooldown map to prevent spamming
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MILLIS = 3000; // 3 seconds

    // Updated constructor to remove the now redundant FirstLandWorldConfigAccessor
    public FirstLandJoinMenu(JavaPlugin plugin, WorldDataAccessor worldDataAccessor, PlayerDataAccessor playerDataAccessor) {
        this.plugin = plugin;
        this.worldDataAccessor = worldDataAccessor;
        this.playerDataAccessor = playerDataAccessor;
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "First Land Menu");

        YOUR_WORLDS_ITEM = createGuiItem(
                Material.GRASS_BLOCK,
                ChatColor.AQUA + "Your Worlds",
                ChatColor.GRAY + "View and join your existing",
                ChatColor.GRAY + "First Land worlds."
        );

        CREATE_NAMED_WORLD_ITEM = createGuiItem(
                Material.WRITABLE_BOOK,
                ChatColor.GREEN + "Create New World",
                ChatColor.GRAY + "Create a new world with a",
                ChatColor.GRAY + "custom name."
        );

        DELETE_WORLD_ITEM = createGuiItem(
                Material.BARRIER,
                ChatColor.RED + "Delete World",
                ChatColor.GRAY + "Permanently delete one of",
                ChatColor.GRAY + "your First Land worlds."
        );

        FILLER_ITEM = createGuiItem(
                Material.GRAY_STAINED_GLASS_PANE,
                " " // Display name is just a space to make it invisible
        );

        setupInventory();
    }

    private void setupInventory() {
        // Place the main items
        inventory.setItem(11, YOUR_WORLDS_ITEM);
        inventory.setItem(13, CREATE_NAMED_WORLD_ITEM);
        inventory.setItem(15, DELETE_WORLD_ITEM);

        // Fill the rest of the inventory with the filler item
        setupFillerItems();
    }

    private void setupFillerItems() {
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType().isAir()) {
                inventory.setItem(i, FILLER_ITEM);
            }
        }
    }

    public void openInventory(Player player) {
        player.openInventory(inventory);
    }

    /**
     * Checks if a player has reached their maximum number of owned worlds.
     *
     * @param player The player to check.
     * @param configAccessor The accessor for world configuration.
     * @return true if the player has reached the world limit, false otherwise.
     */
    public static boolean checkIfPlayerReachedMaxWorlds(Player player, WorldDataAccessor configAccessor, JavaPlugin plugin) {
        UUID id = player.getUniqueId();
        return configAccessor.getPlayerOwnedWorldUUIDs(id).size() >= configAccessor.getMaxWorldsPerPlayer(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) {
            return;
        }
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        UUID playerId = player.getUniqueId();

        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }

        // --- Anti-Spam Cooldown Check ---
        long now = System.currentTimeMillis();
        long lastClickTime = cooldowns.getOrDefault(playerId, 0L);

        if (now - lastClickTime < COOLDOWN_MILLIS) {
            long remainingTimeSeconds = (COOLDOWN_MILLIS - (now - lastClickTime)) / 1000 + 1;
            player.sendMessage(ChatColor.RED + "Please wait " + remainingTimeSeconds + " more seconds before clicking again.");
            return;
        }

        // Update the cooldown for the player
        cooldowns.put(playerId, now);

        // We only care about clicks on the main menu items, not the glass panes
        if (clickedItem.equals(YOUR_WORLDS_ITEM)) {
            player.closeInventory();
            // Updated constructor call to remove configAccessor
            FirstLandViewMenu firstLandViewMenu = new FirstLandViewMenu(plugin, worldDataAccessor, playerDataAccessor, player, this);
            plugin.getServer().getPluginManager().registerEvents(firstLandViewMenu, plugin);
            firstLandViewMenu.openInventory();
        } else if (clickedItem.equals(CREATE_NAMED_WORLD_ITEM)) {
            player.closeInventory();
            FirstLandWorldNameListener.startNamingProcess(player, worldDataAccessor, plugin);
        } else if (clickedItem.equals(DELETE_WORLD_ITEM)) {
            player.closeInventory();
            // Updated constructor call to remove configAccessor
            FirstLandDeleteMenu firstLandDeleteMenu = new FirstLandDeleteMenu(plugin, worldDataAccessor, player, this);
            plugin.getServer().getPluginManager().registerEvents(firstLandDeleteMenu, plugin);
            firstLandDeleteMenu.openInventory();
        }
    }

    /**
     * Helper method to create a GUI item with a specific material, name, and lore.
     *
     * @param material The material of the item.
     * @param name The display name of the item.
     * @param lore An array of strings for the item's lore.
     * @return The created ItemStack.
     */
    private ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));

        item.setItemMeta(meta);
        return item;
    }
}