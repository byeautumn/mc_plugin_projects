package org.aerial_dad.bedwars_plugin.bedwars.game;

import org.bukkit.World;
import javax.annotation.Nonnull;

public class BwWorld {

    private final World world;

    private String worldName;

//    public BwWorld(@Nonnull World world){
//        this.world = world;
//        this.worldName = world.getName();
//    }
    public BwWorld(@Nonnull World world, String worldName){
        this.world = world;
        this.worldName = worldName;
    }

    public World getWorld() {
        return world;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public static void main(String[] args) {

    }
}
