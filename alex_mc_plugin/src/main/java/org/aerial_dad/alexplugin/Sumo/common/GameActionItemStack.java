package org.aerial_dad.alexplugin.Sumo.common;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class GameActionItemStack extends ItemStack {
    public enum Action {
        REPLAY,
        QUIT
    }
    private final Action action;
    public GameActionItemStack (Material material, Action action) {
        super(material, 1);
        this.action = action;
    }

    public Action getAction() {
        return action;
    }
}
