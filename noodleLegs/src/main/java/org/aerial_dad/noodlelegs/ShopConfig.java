package org.aerial_dad.noodlelegs;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class ShopConfig {
    private final static ShopConfig instance = new ShopConfig();

    private YamlConfiguration config;

    private Map<Material, ItemStack> itemToCostMap = new HashMap<>();

    private Map<Material, ItemConfig>itemToConfigMap = new HashMap<>();

    private ShopConfig() {}

    public void load(File shopConfigFile){
        if (!shopConfigFile.exists()) {
            System.err.println("The shop config file shop.yml cannot be found!");
            return;
        }
        config = new YamlConfiguration();

        try{
            config.load(shopConfigFile);

        } catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("Loading shop items ...");
        loadShopItems();
        System.out.println("Shop config has been loaded.");
    }

    private List<String> getItemKeys() {
        ConfigurationSection itemsSection = config.getConfigurationSection("shop." + "pages." + "items");
        if (itemsSection == null) {
            System.err.println("no section called items");
            return Collections.emptyList();
        }

        return new ArrayList<>(itemsSection.getKeys(false));
    }

    private void loadShopItems(){
        itemToCostMap.clear();
        itemToConfigMap.clear();
        List<String> itemSectionMaterial = getItemKeys();
        String shopItemPath = "shop.pages.items.";
        for (String material : itemSectionMaterial){
            String itemKey = shopItemPath + material.trim() + ".material";
            System.err.println();
            final Material itemMaterial = Material.valueOf(config.getString(itemKey));

            String currencyKey = shopItemPath + material.trim() + ".currency";
            final Material currency = Material.valueOf(config.getString(currencyKey));

            String costKey = shopItemPath + material.trim() + ".cost";
            final int cost = config.getInt(costKey);

            String itemPerBuyKey = shopItemPath + material.trim() + ".item-per-buy";
            final int itemPerBuy = config.getInt(itemPerBuyKey);

            String itemSlotKey = shopItemPath + material.trim() + ".slot";
            final int itemSlot = config.getInt(itemSlotKey);

            ItemStack costItem = new ItemStack(currency, cost);
            if (itemMaterial == null){
                System.err.println("itemMaterial is null!!!");
            }
            itemToCostMap.put(itemMaterial, costItem);

            ItemConfig itemConfig = new ItemConfig(itemMaterial, cost, itemPerBuy, itemSlot, currency);
            itemToConfigMap.put(itemMaterial, itemConfig);
        }
    }

    public Map<Material, ItemStack> getItemToCostMap() {
        return itemToCostMap;
    }

    public Map<Material, ItemConfig> getItemToConfigMap() {
        return itemToConfigMap;
    }

    public static ShopConfig getInstance(){
        return instance;
    }
}
