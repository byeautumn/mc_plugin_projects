package org.aerial_dad.noodlelegs.game;

import org.aerial_dad.noodlelegs.NoodleLegs;
import org.aerial_dad.noodlelegs.SpawnNpc;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class ShopNpc extends SpawnNpc {
    private final String name;
    private static final String META_DATA_KEY = "shop";
    private Zombie npc;

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
        System.out.println("Creating shop NPC ...");
        this.npc = (Zombie) shopSpawnBaseLocation.getWorld().spawnEntity(shopSpawnBaseLocation.add(SPAWN_OFFSET), EntityType.ZOMBIE);
        this.npc.setCustomName(getName());
        this.npc.setMetadata(META_DATA_KEY, metadataValue);
        this.npc.setBaby(false);
        this.npc.setHealth(20.0);
        this.npc.getLocation().getDirection().multiply(0);
        System.out.println(" Npc has meta data of '" + META_DATA_KEY + "'.");
    }
}
