package org.byeautumn.chuachua.io;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.byeautumn.chuachua.Universe;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class IOUntil {
    public static final String IO_DIR_NAME = "io";
    public static final String IO_EXTENSION = "cc";
    public static final String CC_SPLITTER = ";";

    public static boolean checkDirExistAndCreateItIfNot(File dir) {
        if (!dir.exists()) {
            System.out.println("IO folder doesn't exist. Trying to create it ...");
            if (dir.mkdirs()) {
                System.err.println("Failed to create " + dir.getName());
            }

            System.out.println("The folder " + dir.getName() + "is created successfully.");
        }

        return true;
    }

    public static File getIODir() {
        File serverDir = Bukkit.getWorldContainer();
        return new File(serverDir, IO_DIR_NAME);
    }

    public static boolean saveExportIntoAIOFile(String chunkName, String exportContent) {
        File ioDir = getIODir();
        if (!IOUntil.checkDirExistAndCreateItIfNot(ioDir)) {
            System.err.println("The IO folder doesn't exist and failed on trying to create it.");
            return false;
        }

        final String fileName = getAbsoluteFilePath(ioDir, chunkName + "." + IO_EXTENSION);
        final File file = new File(fileName);
        if (file.exists()) {
            System.err.println("'" + chunkName + "' exists already. Please give the chunk exportChunk another name.");
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(exportContent);
            System.out.println("File created and saved successfully: " + fileName);
        } catch (IOException e) {
            System.err.println("An error occurred while creating or saving the file: " + e.getMessage());
            return false;
        }

        return true;
    }

    public static String getAbsoluteFilePath(File folder, String fileName) {
        // Create a new File object combining the folder and file name
        File file = new File(folder, fileName);

        // Get the absolute path
        return file.getAbsolutePath();
    }
}
