package org.aerial_dad.noodlelegs;



import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;


public class Npc_listener implements Listener {

    public static Map<Material, ItemStack>itemToCostMap = new HashMap<>();

    public static Map<Material, ItemConfig>itemToConfigMap = new HashMap<>();

    public static Map<String, PageConfig> pageToConfigMap = new HashMap<>();

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
            System.out.println("pageToConfigMap size is: '" + pageToConfigMap.size() + "'. ");
                    for (String string : pageToConfigMap.keySet()) {
                        System.out.println("Creating inventory");
                        PageConfig pageConfig = pageToConfigMap.get(string);
                        Inventory inventory = Bukkit.createInventory(player, pageConfig.getPageSize(), pageConfig.getPageName());
                        System.out.println("itemToConfigMap size is: '" + itemToConfigMap.size() + "'. ");
                        for (Material material : itemToConfigMap.keySet()) {
                            ItemConfig itemConfig = itemToConfigMap.get(material);
                            player.sendMessage("Spawning ");
                            ItemStack itemStack = new ItemStack(material, itemConfig.getItemPerBuy());
                            player.sendMessage(material + " ");
                            inventory.setItem(itemConfig.getItemSlot(), itemStack);

                        }
                        player.openInventory(inventory);
                    }
            }


            }





    @EventHandler
    private void onPlayerClickEvent(InventoryClickEvent event) {
        String view = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        ItemStack itemStack = event.getCurrentItem();
        if (view.equalsIgnoreCase("Shop")) {
            event.setCancelled(true);

            if (itemStack != null) {
//                System.out.println("CODE RUN HERE!!!!!!!!!!");
                if (itemToCostMap.containsKey(itemStack.getType())) {
//                    System.out.println(itemStack);
                    ItemStack cost = itemToCostMap.get(itemStack.getType());
                    System.out.println(cost + " Cost!!!!!!!!!!!!!!!!!!!!");
                    if (player.getInventory().contains(cost)) {
                        player.getInventory().remove(cost);
                        player.getInventory().addItem(itemStack);
                        player.playSound(player.getLocation(), Sound.GLASS, 1.0f, 1.0f);
                        player.sendMessage(ChatColor.GREEN + "You just bought " + itemStack + " successfully for " + cost + ".");
                    } else {
                        player.playSound(player.getLocation(), Sound.CAT_HISS, 1.0f, 1.0f);
                        player.sendMessage(ChatColor.RED + "You do not have the right amount of materials to buy " + itemStack.getType() + ".");
                    }

                }

            }
        }
    }

    @EventHandler
    private void onPlayerClickPageEvent(InventoryClickEvent event){
        Player player = (Player) event.getWhoClicked();
        ItemStack clickItem = event.getCurrentItem();
        if (clickItem.getType() == Material.ARROW){
            Inventory inventory = Bukkit.createInventory(player, 54, "Shop2");
            ItemStack arrowBack = new ItemStack(Material.ARROW, 1);
            inventory.setItem( 51, arrowBack);
            player.openInventory(inventory);
        }


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