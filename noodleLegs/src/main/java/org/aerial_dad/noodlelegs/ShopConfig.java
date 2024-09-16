package org.aerial_dad.noodlelegs;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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


    private ShopConfig(){

    }

    public void load(){
        file = new File(NoodleLegs.getInstance.getDataFolder(), "shop.yml");

        if (!file.exists()) {
            NoodleLegs.getInstance.saveResource("shop.yml", false);
        }
        config = new YamlConfiguration();

        try{
            config.load(file);

        } catch (Exception exception){
            exception.printStackTrace();
        }

//        shopType = EntityType.valueOf(config.getString("shop.entity-type"));
        System.out.println("itemSectionPage");
        itemSectionPage();
        System.out.println("itemSectionMaterial");
        itemSectionMaterial();
    }

    private List<String> getPageKeys(){
        ConfigurationSection pageSection = config.getConfigurationSection("shop." + "pages");
        if(pageSection == null){
            System.err.println("no sections before Shop");
            return Collections.emptyList();
        }
        return new ArrayList<>(pageSection.getKeys(false));
    }



    private void itemSectionPage(){
        List<String> itemSectionPage = getPageKeys();
        for (String page : itemSectionPage){

            String pageNameKey = "shop." + "pages" + ".page-name";
            pageName = String.valueOf(config.getString(pageNameKey));

            String pageSizeKey = "shop." + "pages" + ".page-size";
            pageSize = config.getInt(pageSizeKey);


            PageConfig pageConfig = new PageConfig(pageName, pageSize);
            Npc_listener.pageToConfigMap.put(pageName, pageConfig);
        }

    }

    private List<String> getItemKeys() {
        List<String> itemSectionPage = getPageKeys();
        for (String page : itemSectionPage) {
            ConfigurationSection itemsSection = config.getConfigurationSection("shop." + "pages." + "items");
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

                String itemKey = "shop." + "pages." + "items." + material.trim() + ".material";
                itemMaterial = Material.valueOf(config.getString(itemKey));

                String currencyKey = "shop." + "pages." + "items." + material.trim() + ".currency";
                currency = Material.valueOf(config.getString(currencyKey));

                String costKey = "shop." + "pages." + "items." + material.trim() + ".cost";
                cost = config.getInt(costKey);

                String itemPerBuyKey = "shop." + "pages." + "items." + material.trim() + ".item-per-buy";
                itemPerBuy = config.getInt(itemPerBuyKey);

                String itemSlotKey = "shop." + "pages." + "items." + material.trim() + ".slot";
                itemSlot = config.getInt(itemSlotKey);

                ItemStack costItem = new ItemStack(ShopConfig.getInstance().getCurrency(), ShopConfig.getInstance().getCost());
                if (itemMaterial == null){
                    System.err.println("itemMaterial is null!!!");
                }
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
