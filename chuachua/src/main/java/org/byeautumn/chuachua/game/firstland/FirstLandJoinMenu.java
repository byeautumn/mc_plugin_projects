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

import java.util.*;

public class FirstLandJoinMenu implements Listener {
    private final Inventory inventory;
    private final JavaPlugin plugin;
    private final FirstLandWorldConfigAccessor configAccessor;

    private final ItemStack YOUR_WORLDS_ITEM;
    private final ItemStack CREATE_NAMED_WORLD_ITEM;
    private final ItemStack DELETE_WORLD_ITEM; // New item for deleting worlds

    public FirstLandJoinMenu(JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor) {
        this.plugin = plugin;
        this.configAccessor = configAccessor;
        // Make the inventory size 27 to accommodate more buttons gracefully
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "First Land Menu");

        YOUR_WORLDS_ITEM = createGuiItem(Material.GRASS_BLOCK, ChatColor.AQUA + "Your Worlds",
                ChatColor.GRAY + "View and join your existing", ChatColor.GRAY + "First Land worlds.");

        CREATE_NAMED_WORLD_ITEM = createGuiItem(Material.WRITABLE_BOOK, ChatColor.GREEN + "Create New World",
                ChatColor.GRAY + "Create a new world with a", ChatColor.GRAY + "custom name.");

        DELETE_WORLD_ITEM = createGuiItem(Material.BARRIER, ChatColor.RED + "Delete World",
                ChatColor.GRAY + "Permanently delete one of", ChatColor.GRAY + "your First Land worlds.");

        // Place the items in the inventory. Centered in the middle row.
        inventory.setItem(11, YOUR_WORLDS_ITEM); // Slot 11 (second row, second item)
        inventory.setItem(13, CREATE_NAMED_WORLD_ITEM); // Slot 13 (second row, fourth item)
        inventory.setItem(15, DELETE_WORLD_ITEM); // Slot 15 (second row, sixth item)
    }

    public void openInventory(Player player) {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        // Validate the clicked item
        if (clickedItem == null || clickedItem.getType().isAir() || !clickedItem.hasItemMeta() || !Objects.requireNonNull(clickedItem.getItemMeta()).hasDisplayName()) {
            return;
        }

        if (clickedItem.equals(YOUR_WORLDS_ITEM)) {
            player.closeInventory();
            FirstLandViewMenu firstLandViewMenu = new FirstLandViewMenu(plugin, configAccessor, player, this);
            plugin.getServer().getPluginManager().registerEvents(firstLandViewMenu, plugin);
            firstLandViewMenu.openInventory();
        } else if (clickedItem.equals(CREATE_NAMED_WORLD_ITEM)) {
            player.closeInventory();
            FirstLandWorldNameListener.startNamingProcess(player, configAccessor);
        } else if (clickedItem.equals(DELETE_WORLD_ITEM)) { // Handle the new delete button
            player.closeInventory();
            FirstLandDeleteMenu firstLandDeleteMenu = new FirstLandDeleteMenu(plugin, configAccessor, player, this);
            plugin.getServer().getPluginManager().registerEvents(firstLandDeleteMenu, plugin);
            firstLandDeleteMenu.openInventory();
        }
    }

    public static boolean checkIfPlayerReachedMaxWorlds(Player player, FirstLandWorldConfigAccessor configAccessor) {
        UUID id = player.getUniqueId();
        if(configAccessor.getPlayerOwnedWorldUUIDs(id).size() >= configAccessor.getMaxWorldsPerPlayer()){
            return true;
        }
        return false;
    }
    /**
     * Helper method to create a GUI item with a specific material, name, and lore.
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

    public static void transferGui(Player player, Inventory to){
        player.closeInventory();
        player.openInventory(to);
    }
}
