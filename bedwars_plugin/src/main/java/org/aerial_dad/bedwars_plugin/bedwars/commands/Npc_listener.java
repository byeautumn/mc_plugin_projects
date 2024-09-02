package org.aerial_dad.bedwars_plugin.bedwars.commands;


import org.aerial_dad.bedwars_plugin.Bedwars_plugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class Npc_listener implements Listener {

    public static Map<Material, ItemStack>itemToCostMap = new HashMap<>();

    private final Bedwars_plugin plugin;

    public Npc_listener(Bedwars_plugin plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        if (entity.hasMetadata("playercreated")) {
            Inventory inventory = Bukkit.createInventory(player, 45, "Shop");
            ItemStack Wool = new ItemStack(Material.WHITE_WOOL, 16);
            ItemStack Stone_Sword = new ItemStack(Material.STONE_SWORD, 1);
            inventory.setItem(28, Wool);
            inventory.setItem(29, Stone_Sword);
            player.openInventory(inventory);
        }


    }

    @EventHandler
    private void onPlayerClickEvent(InventoryClickEvent event){
        String view = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        if (view.equalsIgnoreCase("Shop")){
            event.setCancelled(true);
            if (item != null){
                if (itemToCostMap.containsKey(item.getType())){
                    ItemStack cost = itemToCostMap.get(item.getType());
                    System.out.println(cost + " Cost!!!!!!!!!!!!!!!!!!!!");
                    if (player.getInventory().contains(cost)){
                        player.getInventory().remove(cost);
                        player.getInventory().addItem(item);
                        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                        player.sendMessage("You just bought " + item + " successfully for " + cost + ".");
                    }else{
                        player.playSound(player, Sound.ENTITY_WITHER_HURT, 1.0f, 1.0f);
                        player.sendMessage("You do not have the right amount of materials to buy " + item.getType() + ".");
                    }
                }else{
//                    player.sendMessage(player.getDisplayName() + " has just clicked on something not in the shop.");
                }
            }

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