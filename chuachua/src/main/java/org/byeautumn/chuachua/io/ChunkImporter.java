package org.byeautumn.chuachua.io;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ChunkImporter {

    public boolean importChunk(String chunkName, Block baseBlock) {
        if (!exists(chunkName)) {
            return false;
        }
        final World world = baseBlock.getWorld();
        int baseX = baseBlock.getX(), baseY = baseBlock.getY(), baseZ = baseBlock.getZ();
        final String filePath = IOUntil.getAbsoluteFilePath(IOUntil.getIODir(), chunkName + "." + IOUntil.IO_EXTENSION);
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] strArr = line.split(",");
                if (strArr.length != 4) {
                    System.err.println("The imported line is invalid ... skip.");
                    continue;
                }
                int x = Integer.parseInt(strArr[0]);
                int y = Integer.parseInt(strArr[1]);
                int z = Integer.parseInt(strArr[2]);
                Material material = Material.valueOf(strArr[3]);
                world.getBlockAt(baseX + x, baseY + y, baseZ + z).setType(material);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return false;
        }
        return true;
    }

    public boolean exists(String chunkName) {
        if (null == chunkName || chunkName.isEmpty()) {
            System.err.println("Invalid given chunk name: " + chunkName);
            return false;
        }
        File ioDir = IOUntil.getIODir();
        if (!ioDir.exists()) {
            System.err.println("The IO folder doesn't exist so there is no chunk can be imported.");
            return false;
        }

        final String fileName = IOUntil.getAbsoluteFilePath(ioDir, chunkName + "." + IOUntil.IO_EXTENSION);
        final File file = new File(fileName);
        if (!file.exists()) {
            System.err.println("'" + chunkName + "' doesn't exist.");
            return false;
        }

        return true;
    }
}
