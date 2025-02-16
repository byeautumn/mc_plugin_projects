package org.byeautumn.chuachua.io;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.byeautumn.chuachua.undo.ActionRecord;
import org.byeautumn.chuachua.undo.BlockProperties;
import org.byeautumn.chuachua.undo.BlockPropertiesRecord;
import org.byeautumn.chuachua.undo.GenerationRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ChunkImporter {
    private final Player player;

    public ChunkImporter(Player player) {
        this.player = player;
    }

    public ActionRecord importChunk(String chunkName, Block baseBlock) {
        if (!exists(chunkName)) {
            return null;
        }
        final World world = baseBlock.getWorld();
        int baseX = baseBlock.getX(), baseY = baseBlock.getY(), baseZ = baseBlock.getZ();
        final String filePath = IOUntil.getAbsoluteFilePath(IOUntil.getIODir(), chunkName + "." + IOUntil.IO_EXTENSION);
        GenerationRecord action;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            action = new GenerationRecord();
            String line;
            while ((line = br.readLine()) != null) {
                String[] strArr = line.split(IOUntil.CC_SPLITTER);
                if (strArr.length < 4) {
                    System.err.println("The imported line is invalid ... skip.");
                    continue;
                }

                int x = Integer.parseInt(strArr[0]);
                int y = Integer.parseInt(strArr[1]);
                int z = Integer.parseInt(strArr[2]);
                Material material = Material.valueOf(strArr[3]);
                Block original = world.getBlockAt(baseX + x, baseY + y, baseZ + z);
                if (strArr.length < 5) {
                    original.setType(material);
                } else {
                    String originalBlockData = original.getBlockData().getAsString();
                    String blockData = strArr[4];
                    if (IOUntil.updateBlockData(player, original, blockData)) {
                        BlockPropertiesRecord record = new BlockPropertiesRecord(original, new BlockProperties(originalBlockData),
                                new BlockProperties(blockData));
                        action.addBlockPropertiesRecord(record);
                    }
                }

            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return null;
        }
        return action;
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
