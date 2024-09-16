package org.aerial_dad.noodlelegs;

import org.bukkit.Material;

public class ItemConfig {
    private Material currency;

    private int cost;

    private int itemPerBuy;

    private Integer itemSlot;

    private Material itemMaterial;

    public ItemConfig(Material material, int cost, int itemPerBuy, int itemSlot, Material currency){
        this.itemMaterial = material;
        this.cost = cost;
        this.itemPerBuy = itemPerBuy;
        this.itemSlot = itemSlot;
        this.currency = currency;
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

    public Integer getItemSlot() {
        return itemSlot;
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }
}
