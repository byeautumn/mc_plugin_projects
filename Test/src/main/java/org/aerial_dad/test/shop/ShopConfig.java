package org.aerial_dad.test.shop;

import org.aerial_dad.test.Test;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.units.qual.A;
import sun.jvm.hotspot.debugger.Page;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ShopConfig {

    private final static ShopConfig instance = new ShopConfig();

    private File file;
    private YamlConfiguration config;

    private EntityType shopType;

    private Material currency;

    private int cost;

    private int itemPerBuy;

    private Integer itemSlot;

    private Material itemMaterial;

    private String pageName;

    private int pageSize;

    private int pageIndex;

    private Material pageSwitchItem;


    private ShopConfig(){

    }

    public void load(){
        file = new File(Test.getInstance.getDataFolder(), "Shop.yml");

        if (!file.exists()) {
            Test.getInstance.saveResource("Shop.yml", false);
        }
        config = new YamlConfiguration();
        config.options().parseComments(true);

        try{
            config.load(file);

        } catch (Exception exception){
            exception.printStackTrace();
        }

//        shopType = EntityType.valueOf(config.getString("shop.entity-type"));
        itemSectionPage();
        itemSectionMaterial();
    }

    private List<String> getPageKeys(){
        ConfigurationSection pageSection = config.getConfigurationSection("shop");
        if(pageSection == null){
            System.err.println("no sections before Shop");
            return Collections.emptyList();
        }
        return new ArrayList<>(pageSection.getKeys(false));
    }



    private void itemSectionPage(){
        List<String> itemSectionPage = getPageKeys();
        for (String page : itemSectionPage){

            String pageNameKey = "shop." + page.trim() + ".page-name";
            pageName = String.valueOf(config.getString(pageNameKey));

            String pageSizeKey = "shop." + page.trim() + ".page-size";
            pageSize = config.getInt(pageSizeKey);

            String pageIndexKey = "shop." + page.trim() + ".page-index";
            pageIndex = config.getInt(pageIndexKey);

            String pageSwitchItemKey = "shop." + page.trim() + ".page-switch-item";
            pageSwitchItem = Material.valueOf(pageSwitchItemKey);

            PageConfig pageConfig = new PageConfig(pageName, pageSize, pageIndex, pageSwitchItem);
            Npc_listener.pageToConfigMap.put(pageName, pageConfig);
        }

    }

    private List<String> getItemKeys() {
        List<String> itemSectionPage = getPageKeys();
        for (String page : itemSectionPage) {
            ConfigurationSection itemsSection = config.getConfigurationSection("shop." + page.trim() + "items");
            if (itemsSection == null) {
                System.err.println("no section called items");
                return Collections.emptyList();
            }
            return new ArrayList<>(itemsSection.getKeys(false));
        }

        return itemSectionPage;
    }

    public void itemSectionMaterial(){
        List<String> itemSectionMaterial = getItemKeys();
        for (String material : itemSectionMaterial){
            List<String> itemSectionPage = getPageKeys();
            for (String page : itemSectionPage) {

                String itemKey = "shop." + page.trim() + ".items." + material.trim() + ".material";
                itemMaterial = Material.valueOf(config.getString(itemKey));

                String currencyKey = "shop." + page.trim() + ".items." + material.trim() + ".currency";
                currency = Material.valueOf(config.getString(currencyKey));

                String costKey = "shop." + page.trim() + ".items." + material.trim() + ".cost";
                cost = config.getInt(costKey);

                String itemPerBuyKey = "shop." + page.trim() + ".items." + material.trim() + ".item-per-buy";
                itemPerBuy = config.getInt(itemPerBuyKey);

                String itemSlotKey = "shop." + page.trim() + ".items." + material.trim() + ".slot";
                itemSlot = config.getInt(itemSlotKey);

                ItemStack costItem = new ItemStack(ShopConfig.getInstance().getCurrency(), ShopConfig.getInstance().getCost());
                Npc_listener.itemToCostMap.put(itemMaterial, costItem);

                ItemConfig itemConfig = new ItemConfig(itemMaterial, cost, itemPerBuy, itemSlot, currency);
                Npc_listener.itemToConfigMap.put(itemMaterial, itemConfig);
            }
        }
    }



    public void save(){
        try{
            config.save(file);

        } catch (Exception exception){
            exception.printStackTrace();
        }
    }

    public void set(String path, Object value){
        config.set(path, value);

        save();
    }

    public Material getCurrency() {
        return currency;
    }

    public int getCost() {
        return cost;
    }

    public int getItemPerBuy() {
        return itemPerBuy;
    }

    public int getItemSlot() {
        return itemSlot;
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }


    public void setShopType(EntityType shopType) {
        this.shopType = shopType;
        set(" ", shopType.name());
    }



    public static ShopConfig getInstance(){
        return instance;
    }
}
