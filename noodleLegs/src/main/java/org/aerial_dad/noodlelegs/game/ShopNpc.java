package org.aerial_dad.noodlelegs.game;

import org.aerial_dad.noodlelegs.NoodleLegs;
import org.aerial_dad.noodlelegs.SpawnNpc;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class ShopNpc extends SpawnNpc {
    private final String name;
    private static final String META_DATA_KEY = "shop";
    private Entity npc;

    private static final Vector SPAWN_OFFSET = new Vector(8.5, 0, 0.5);

    public ShopNpc(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void release() {
        this.npc.remove();
    }

    public void spawn(Location shopSpawnBaseLocation) {
        FixedMetadataValue metadataValue = new FixedMetadataValue(getPlugin(), true);
        this.npc = shopSpawnBaseLocation.getWorld().spawnEntity(shopSpawnBaseLocation.add(SPAWN_OFFSET), EntityType.ZOMBIE);
        this.npc.setCustomName(getName());
        this.npc.setMetadata(META_DATA_KEY, metadataValue);
        System.out.println(" Npc has meta data of '" + META_DATA_KEY + "'.");
        LivingEntity entity = (LivingEntity) this.npc;
        entity.setHealth(20);
    }
}
