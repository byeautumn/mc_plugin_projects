package org.byeautumn.chuachua.game.firstland;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.IntStream;

public class FirstLandViewMenu implements Listener {
    private final Inventory inventory;
    private final JavaPlugin plugin;
    private final FirstLandWorldConfigAccessor configAccessor;
    private final Player menuOpener;
    private final FirstLandJoinMenu parentJoinMenu;

    // GUI Item Constants
    private static ItemStack BLUE_FILLER;
    private static ItemStack LIGHT_BLUE_FILLER;
    private static ItemStack PINK_FILLER;
    private static ItemStack BLACK_FILLER;
    private static ItemStack BACK_ITEM;
    private static ItemStack AVAILABLE_SLOT_ITEM;
    private static ItemStack SLOT_SUMMARY_HEAD;

    public FirstLandViewMenu(JavaPlugin plugin, FirstLandWorldConfigAccessor configAccessor, Player player, FirstLandJoinMenu parentJoinMenu) {
        this.plugin = plugin;
        this.configAccessor = configAccessor;
        this.menuOpener = player;
        this.parentJoinMenu = parentJoinMenu;

        // Change inventory size to 27
        this.inventory = Bukkit.createInventory(null, 27, ChatColor.DARK_BLUE + "First Land Showcase");

        initializeGuiItems();
        populateWorlds();
    }

    private void initializeGuiItems() {
        if (BLUE_FILLER == null) {
            BLUE_FILLER = createGuiItem(Material.BLUE_STAINED_GLASS_PANE, " ");
            LIGHT_BLUE_FILLER = createGuiItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ");
            PINK_FILLER = createGuiItem(Material.PINK_STAINED_GLASS_PANE, " ");
            BLACK_FILLER = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
            BACK_ITEM = createGuiItem(Material.ARROW, ChatColor.YELLOW + "Back", ChatColor.GRAY + "Return to the main menu");
            AVAILABLE_SLOT_ITEM = createGuiItem(Material.PAPER, ChatColor.WHITE + "Available World Slot", ChatColor.GRAY + "Create a new world in this slot.");
            SLOT_SUMMARY_HEAD = createPlayerHeadItem(ChatColor.AQUA + "World Slots", menuOpener);
        }
    }

    public void openInventory() {
        menuOpener.openInventory(inventory);
    }

    private void populateWorlds() {
        inventory.clear();
        setupVisualLayout();

        List<UUID> ownedWorldUUIDs = configAccessor.getPlayerOwnedWorldUUIDs(menuOpener.getUniqueId());
        int ownedWorldsCount = ownedWorldUUIDs.size();
        int maxWorlds = configAccessor.getMaxWorldsPerPlayer();
        int availableSlotsCount = maxWorlds - ownedWorldsCount;

        // Create a new, mutable copy of the SLOT_SUMMARY_HEAD item
        ItemStack mutableSummaryHead = SLOT_SUMMARY_HEAD.clone();
        ItemMeta slotsMeta = mutableSummaryHead.getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "You own: " + ChatColor.YELLOW + ownedWorldsCount + ChatColor.GRAY + " world(s)");
        lore.add(ChatColor.GRAY + "Max worlds: " + ChatColor.YELLOW + maxWorlds);
        lore.add(ChatColor.GRAY + "Available: " + ChatColor.GREEN + availableSlotsCount + ChatColor.GRAY + " slot(s)");
        slotsMeta.setLore(lore);
        mutableSummaryHead.setItemMeta(slotsMeta);

        // Place the updated player head at the new central top slot (4)
        inventory.setItem(4, mutableSummaryHead);

        // Define the content slots for a 27-slot GUI (middle row)
        List<Integer> contentSlots = Arrays.asList(
                11, 12, 13, 14, 15
        );
        int currentContentSlot = 0;

        // Place owned worlds
        for (UUID worldUUID : ownedWorldUUIDs) {
            if (currentContentSlot >= contentSlots.size()) break;

            String friendlyName = configAccessor.getWorldFriendlyName(worldUUID);
            String internalName = configAccessor.getWorldName(worldUUID);
            long createdAt = configAccessor.getConfig().getLong("worlds." + worldUUID.toString() + ".created-at", 0);
            String creationDate = (createdAt > 0) ? new Date(createdAt).toString() : "Unknown";

            ItemStack worldItem = createGuiItem(Material.GRASS_BLOCK,
                    ChatColor.YELLOW + friendlyName,
                    ChatColor.GRAY + "ID: " + worldUUID.toString(),
                    ChatColor.GRAY + "Internal Name: " + internalName,
                    ChatColor.GRAY + "Created: " + creationDate,
                    "",
                    ChatColor.GREEN + "Click to join!");

            inventory.setItem(contentSlots.get(currentContentSlot++), worldItem);
        }

        // Place available slots
        for (int i = ownedWorldsCount; i < maxWorlds; i++) {
            if (currentContentSlot >= contentSlots.size()) break;
            inventory.setItem(contentSlots.get(currentContentSlot++), AVAILABLE_SLOT_ITEM);
        }
    }

    private void setupVisualLayout() {
        // Place black glass corners for 27 slots
        inventory.setItem(0, BLACK_FILLER);
        inventory.setItem(8, BLACK_FILLER);
        inventory.setItem(18, BLACK_FILLER);
        inventory.setItem(26, BLACK_FILLER);

        // Place pink glass at slots 2 and 6
        inventory.setItem(2, PINK_FILLER);
        inventory.setItem(6, PINK_FILLER);

        // Place blue glass for the rest of the top row and side borders
        inventory.setItem(1, BLUE_FILLER);
        inventory.setItem(3, BLUE_FILLER);
        inventory.setItem(5, BLUE_FILLER);
        inventory.setItem(7, BLUE_FILLER);
        inventory.setItem(9, BLUE_FILLER); // Left side, middle row
        inventory.setItem(17, BLUE_FILLER); // Right side, middle row

        // Fill the bottom row with blue glass
        IntStream.range(19, 26).forEach(i -> inventory.setItem(i, BLUE_FILLER));

        // Place the back item at the center of the bottom row (22)
        inventory.setItem(22, BACK_ITEM);

        // Remove the call to set the summary head here, as it's handled in populateWorlds()
        // inventory.setItem(4, SLOT_SUMMARY_HEAD);

        // Place a light blue inner frame
        inventory.setItem(10, LIGHT_BLUE_FILLER);
        inventory.setItem(16, LIGHT_BLUE_FILLER);
        // Additional slots for light blue filler if needed for the new layout
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        int clickedSlot = event.getSlot();

        if (clickedItem == null || clickedItem.getType().isAir()) return;

        // Handle button clicks
        if (clickedItem.equals(BACK_ITEM)) {
            player.closeInventory();
            HandlerList.unregisterAll(this);
            parentJoinMenu.openInventory(player);
            return;
        }

        // Handle clicks on filler items, summary head
        // Note: Checking the display name is more robust than using .equals() on static items
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta != null) {
            String displayName = meta.getDisplayName();
            if (displayName.equals(" ") || displayName.equals(ChatColor.AQUA + "World Slots")) {
                return;
            }
        }

        // Handle available slot clicks
        if (clickedItem.equals(AVAILABLE_SLOT_ITEM)) {
            player.closeInventory();
            HandlerList.unregisterAll(this);
            FirstLandWorldNameListener.startNamingProcess(player, configAccessor);
            return;
        }

        // Handle world item clicks
        if (clickedItem.getType() == Material.GRASS_BLOCK) {
            if (meta == null || !meta.hasLore()) return;

            UUID targetWorldUUID = null;
            for (String line : meta.getLore()) {
                if (ChatColor.stripColor(line).startsWith("ID: ")) {
                    try {
                        String uuidString = ChatColor.stripColor(line).substring(4);
                        targetWorldUUID = UUID.fromString(uuidString);
                        break;
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            if (targetWorldUUID != null) {
                player.closeInventory();
                HandlerList.unregisterAll(this);
                Universe.connectPlayerToSpecificWorld(player, plugin, configAccessor, Objects.requireNonNull(Universe.getChuaWorldById(targetWorldUUID)).getWorld().getName(), targetWorldUUID);
            }
        }
    }

    private ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlayerHeadItem(final String name, final Player player) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        final SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta.setDisplayName(name);
        meta.setOwningPlayer(player);
        head.setItemMeta(meta);
        return head;
    }
}