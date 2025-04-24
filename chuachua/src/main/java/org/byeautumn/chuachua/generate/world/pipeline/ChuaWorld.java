package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.World;

import java.util.UUID;

public class ChuaWorld {
    private final World world;
    private final long seed;
    public ChuaWorld(long seed, World world) {
        this.seed = seed;
        this.world = world;
    }
    public World getWorld() {
        return world;
    }

    public long getSeed() {
        return seed;
    }
    public UUID getID(){
        return world.getUID();
    }


}
