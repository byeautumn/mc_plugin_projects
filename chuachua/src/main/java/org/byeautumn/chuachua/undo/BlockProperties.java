package org.byeautumn.chuachua.undo;

import org.bukkit.Material;

public class BlockProperties {
    private final Material meterial;
    public BlockProperties(Material material) {
        this.meterial = material;
    }

    public Material getMeterial() {
        return meterial;
    }
}
