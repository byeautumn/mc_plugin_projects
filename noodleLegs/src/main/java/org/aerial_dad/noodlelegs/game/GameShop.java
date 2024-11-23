package org.aerial_dad.noodlelegs.game;

import de.tr7zw.nbtapi.NBT;
import org.aerial_dad.noodlelegs.ItemConfig;
import org.aerial_dad.noodlelegs.ShopConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GameShop {
    public static final String GAME_SHOP_INVENTORY_NAME = "shop";
    public static final String NBT_ITEMSTACK_TYPE_KEY = "ItemType";
    public static final String SHOP_ITEMSTACK_TYPE_NAME = "ShopItem";

    private final ShopConfig shopConfig;

    private Map<UUID, Inventory> playerToShopInvenotryMap = new HashMap<>();

    public GameShop(ShopConfig shopConfig) {
        this.shopConfig = shopConfig;
    }

    private Inventory createShopInventory(Player player) {
        System.out.println("Creating a new inventory ...");
        Inventory shopInventory = Bukkit.createInventory(player, 27, GAME_SHOP_INVENTORY_NAME);
        Map<Material, ItemConfig> itemToConfigMap = this.shopConfig.getItemToConfigMap();
        System.out.println("itemToConfigMap size is: '" + itemToConfigMap.size() + "'. ");
        for (Material material : itemToConfigMap.keySet()) {
            ItemConfig itemConfig = itemToConfigMap.get(material);
            player.sendMessage("Spawning ");
            ItemStack itemStack = new ItemStack(material, itemConfig.getItemPerBuy());
            NBT.modify(itemStack, nbt -> {
                nbt.setString(NBT_ITEMSTACK_TYPE_KEY, SHOP_ITEMSTACK_TYPE_NAME);
            });
            player.sendMessage(material + " ");
            shopInventory.setItem(itemConfig.getItemSlot(), itemStack);
        }

        return shopInventory;
    }

    public Inventory getShopInventory(Player player) {
        if(!playerToShopInvenotryMap.containsKey(player.getUniqueId())) {
            playerToShopInvenotryMap.put(player.getUniqueId(), createShopInventory(player));
        }

        return playerToShopInvenotryMap.get(player.getUniqueId());
    }

    public boolean isShopItem(ItemStack item) {
        if(null != item && item.getType() != Material.AIR) {
//            System.out.println("DEBUGGING: the given item is " + item.getType() + " and the amount is " + item.getAmount());
            if (GameShop.SHOP_ITEMSTACK_TYPE_NAME.equals(NBT.get(item, nbt ->
                    (String) nbt.getString(GameShop.NBT_ITEMSTACK_TYPE_KEY)
            ))) {
                return true;
            }
        }

        return false;
    }
    public void sell(Player player, ItemStack item) {
        Map<Material, ItemStack> itemToCostMap = this.shopConfig.getItemToCostMap();
        if (itemToCostMap.containsKey(item.getType())) {
            ItemStack cost = itemToCostMap.get(item.getType());
            System.out.println("Found item '" + item.getType() + "' costs " + cost.getAmount() + " '" + cost.getType() + "'.");

            Inventory inventory = player.getInventory();
            ItemStack[] allItems = inventory.getContents();
            System.out.println("Player '" + player.getDisplayName() + "' has total " + allItems.length + " items in inventory.");
            List<ItemStack> moneyStacks = new ArrayList<>();
            for (ItemStack curr : allItems) {
                if (null != curr) {
                    if (isShopItem(curr)) {
                        continue;
                    }
                    if (cost.getType().equals(curr.getType())) {
                        moneyStacks.add(curr);
                    }
                }
            }
            System.out.println("Player '" + player.getDisplayName() + "' has " + moneyStacks.size() + " money items in inventory.");
            int totalAmount = 0;
            for (ItemStack curr : moneyStacks) {
                totalAmount += curr.getAmount();
            }
            System.out.println("Player '" + player.getDisplayName() + "' has buying power of " + totalAmount + " in pockets.");
            if(cost.getAmount() <= totalAmount) {
                System.out.println("Player has the buying power for item '" + item.getType() + "'.");
                for (ItemStack curr : moneyStacks) {
                    inventory.remove(curr);
                }

                if (cost.getAmount() < totalAmount) {
                    int remainingAmount = totalAmount - cost.getAmount();
                    System.out.println("Player has " + remainingAmount + " '" + cost.getType() + "' remaining as money.");
                    ItemStack remainder = new ItemStack(cost.getType(), remainingAmount);
                    inventory.addItem(remainder);
                }
                inventory.addItem(item);

                player.playSound(player.getLocation(), Sound.GLASS, 1.0f, 1.0f);
                player.sendMessage(ChatColor.GREEN + "You just bought " + item + " successfully for " + cost + ".");
            }
            else {
                System.out.println("Player doesn't have the buying power for item '" + item.getType() + "'.");
                player.playSound(player.getLocation(), Sound.CAT_HISS, 1.0f, 1.0f);
                player.sendMessage(ChatColor.RED + "You do not have the right amount of materials to buy " + item.getType() + ".");
            }
        }
    }
}
