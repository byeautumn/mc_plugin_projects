package org.byeautumn.chuachua.io;

import org.bukkit.World;
import org.bukkit.block.Block;

public class ChunkExporter {

    public boolean exportChunk(String chunkName, Block b1, Block b2) {
        if (!IOUntil.saveExportIntoAIOFile(chunkName, printNormalizedBlocksFromDiaSelection(b1, b2))) {
            System.err.println("Chunk exportChunk failed.");
            return false;
        }
        System.out.println("Chunk exportChunk succeeded.");
        return true;
    }

    private String printNormalizedBlocksFromDiaSelection(Block b1, Block b2) {
        if (!b1.getWorld().getName().equalsIgnoreCase(b2.getWorld().getName())) {
            System.err.println("The input blocks are not from the same world. The exportChunk process will be terminated.");
            return null;
        }

        StringBuffer sb = new StringBuffer();
        final World world = b1.getWorld();
        int baseX = b1.getX(), baseY = b1.getY(), baseZ = b1.getZ();
        int diffX = b2.getX() - b1.getX(), diffY = b2.getY() - b1.getY(), diffZ = b2.getZ() - b1.getZ();
        int factorX = diffX < 0 ? -1 : 1, factorY = diffY < 0 ? -1 : 1, factorZ = diffZ < 0 ? -1 : 1;
        for (int xx = 0; xx <= Math.abs(diffX); ++xx) {
            for (int yy = 0; yy <= Math.abs(diffY); ++yy) {
                for (int zz = 0; zz <= Math.abs(diffZ); ++zz) {
                    int stepX = factorX * xx, stepY = factorY * yy, stepZ = factorZ * zz;
                    int newX = baseX + stepX;
                    int newY = baseY + stepY;
                    int newZ = baseZ + stepZ;
                    Block block = world.getBlockAt(newX, newY, newZ);
                    sb.append(stepX).append(",").append(stepY).append(",").append(stepZ).append(",").append(block.getType().name()).append("\n");
                }
            }
        }
        return sb.toString();
    }
}
