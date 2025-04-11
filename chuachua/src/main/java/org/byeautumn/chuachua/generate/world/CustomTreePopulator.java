package org.byeautumn.chuachua.generate.world;

import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;
import java.util.Random;

public interface CustomTreePopulator {

    void populateCustomTree(WorldInfo worldInfo, Random random, int x, int y, int z, LimitedRegion limitedRegion);

}