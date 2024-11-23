package org.aerial_dad.noodlelegs;


import org.aerial_dad.noodlelegs.game.Game;
import org.aerial_dad.noodlelegs.game.GameShop;
import org.aerial_dad.noodlelegs.game.PlayerTracker;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;


public class Npc_listener implements Listener {

    public static Map<Material, ItemStack>itemToCostMap = new HashMap<>();

    public static Map<Material, ItemConfig>itemToConfigMap = new HashMap<>();

    private final NoodleLegs plugin;

    public Npc_listener(NoodleLegs plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        System.out.println("PlayerInteractEvent:");
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        System.out.println("player has right clicked '" + entity + "'. ");
        if (entity.hasMetadata("shop")) {
            System.out.println(entity.getName() + " has meta data!");

            PlayerTracker playerTracker = Universe.getPlayerTracker(player);
            Game game = playerTracker.getCurrentGame();
            if (null == game) {
                System.err.println("Player '" + player.getDisplayName() + "' is not in a game now. Skipping ...");
                return;
            }
            GameShop gameShop = game.getGameShop();
            Inventory shopInventory = gameShop.getShopInventory(player);
            if(null == shopInventory) {
                System.err.println("Failed to locate shop inventory for player '" + player.getDisplayName() + "'.");
                return;
            }

            player.openInventory(shopInventory);
        }

    }
    @EventHandler
    private void onShopDamage(EntityDamageEvent event){
        if (event.getEntity().hasMetadata("shop")){
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerClickEvent(InventoryClickEvent event) {
        String view = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        ItemStack itemStack = event.getCurrentItem();
        Inventory clickedInventory = event.getClickedInventory();
        if(null == itemStack || null == clickedInventory) {
            System.out.println("Player '" + player.getDisplayName() + "' didn't click on an inventory item.");
            return;
        }
        if (GameShop.GAME_SHOP_INVENTORY_NAME.equalsIgnoreCase(view) &&
                GameShop.GAME_SHOP_INVENTORY_NAME.equalsIgnoreCase(clickedInventory.getName())) {
            event.setCancelled(true);

            if (itemStack != null) {
                PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                Game game = playerTracker.getCurrentGame();
                if (null == game) {
                    System.err.println("Player '" + player.getDisplayName() + "' is not in a game now. Skipping ...");
                    return;
                }
                GameShop gameShop = game.getGameShop();
                if (!gameShop.isShopItem(itemStack)) {
                    System.out.println("Player '" + player.getDisplayName() + "' clicked on a non-shop item.");
                    return;
                }
                gameShop.sell(player, itemStack);

            }
        }
    }

    @EventHandler
    private void onPlayerClickPageEvent(InventoryClickEvent event){
//        Player player = (Player) event.getWhoClicked();
//        ItemStack clickItem = event.getCurrentItem();
//        if (clickItem.getType() == Material.ARROW){
//            Inventory inventory = Bukkit.createInventory(player, 54, "Shop2");
//            ItemStack arrowBack = new ItemStack(Material.ARROW, 1);
//            inventory.setItem( 51, arrowBack);
//            player.openInventory(inventory);
//        }


    }


}
/**
 * Right-click a Npc to open a shop
 * the shop has different items you can buy for an amount of currency (e.g. iron gold diamond etc.).
 * Each item has a cost.
 * If you want to get that item you will have to have the right amount of resources (currency) in your personal inventory in order to buy that item.
 * When u right-click or left-click an item it will be directed into your inventory but also take out the amount (from your inventory) that is required to buy that item.
 *
 * Cheating preventions:
 * the item in the shop cannot be taken out of the shop. e.g. if I want to buy a Diamond sword I cannot just drag the diamond sword in to my hot bar I need to have the system to do that.
 * It's like blacklisting the item so when u have that item that is blacklisted it will be removed from your inventory.
 */
/*


 */