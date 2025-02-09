package org.byeautumn.chuachua.io;

import org.bukkit.Bukkit;
import org.byeautumn.chuachua.Universe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class IOUntil {
    public static final String IO_DIR_NAME = "io";
    public static final String IO_EXTENSION = "cc";

    public static boolean checkDirExistAndCreateItIfNot(File dir) {
        if (!dir.exists()) {
            System.out.println("IO folder doesn't exist. Trying to create it ...");
            try {
                dir.createNewFile();
            } catch (IOException ioe) {
                System.err.println("IO folder creation failed: " + ioe.getMessage());
                return false;
            }
            System.out.println("IO folder is created successfully.");
        }

        return true;
    }

    public static boolean saveExportIntoAIOFile(String chunkName, String exportContent) {
        File serverDir = Bukkit.getWorldContainer();
        File ioDir = new File(serverDir, IO_DIR_NAME);
        if (!IOUntil.checkDirExistAndCreateItIfNot(ioDir)) {
            System.err.println("The IO folder doesn't exist and failed on trying to create it.");
            return false;
        }

        String fileName = getAbsoluteFilePath(ioDir, chunkName + "." + IO_EXTENSION);
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
