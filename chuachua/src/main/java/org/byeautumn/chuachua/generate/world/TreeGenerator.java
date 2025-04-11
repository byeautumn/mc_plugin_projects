package org.byeautumn.chuachua.generate.world;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Random;

public class TreeGenerator extends BlockPopulator {

    @Override
    public void populate(@NonNull World world, @NonNull Random random, @NonNull Chunk chunk) {
        int cX = chunk.getX() * 16;
        int cZ = chunk.getZ() * 16;
        int cXOff = random.nextInt(10);
        int cZOff = random.nextInt(10);

    }


}
