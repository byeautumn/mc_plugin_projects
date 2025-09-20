package org.byeautumn.chuachua.player;

import org.byeautumn.chuachua.accessor.Accessor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class PlayerDataAccessor implements Accessor {

    private final File baseDir;
    private static final String HUB_FILE_NAME = "hub.json";

    public PlayerDataAccessor(File baseDir) {
        this.baseDir = new File(baseDir, "player-data");
        createDirectories(this.baseDir);
    }

    @Override
    public void createDirectories(File baseDir) {
        try {
            if (!baseDir.exists()) {
                Files.createDirectories(baseDir.toPath());
                System.out.println("Created player data base directory: " + baseDir.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to create player data directories: " + e.getMessage());
            throw new RuntimeException("Could not create player data directories", e);
        }
    }

    public void savePlayerData(PlayerData playerData) {
        File playerDir = new File(this.baseDir, playerData.getPlayerUUID().toString());
        String fileName = playerData.getWorldInternalName().equals("world") ? HUB_FILE_NAME : playerData.getWorldUUID().toString() + ".json";
        File playersDataFile = new File(playerDir, fileName);

        try {
            Files.createDirectories(playerDir.toPath());
            try (FileWriter writer = new FileWriter(playersDataFile)) {
                writer.write(playerData.toJson());
                System.out.println("Saved player data for player " + playerData.getPlayerUUID() + " in world " + playerData.getWorldUUID());
            }
        } catch (IOException e) {
            System.err.println("Error saving player data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public PlayerData getPlayerData(UUID playerUUID, UUID worldUUID, String worldInternalName) {
        File playerDir = new File(this.baseDir, playerUUID.toString());
        String fileName = worldInternalName.equals("world") ? HUB_FILE_NAME : worldUUID.toString() + ".json";
        File playerDataFile = new File(playerDir, fileName);

        if (!playerDataFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(playerDataFile)) {
            StringBuilder sb = new StringBuilder();
            int character;
            while ((character = reader.read()) != -1) {
                sb.append((char) character);
            }
            JSONObject jsonObject = new JSONObject(sb.toString());
            return PlayerData.fromJsonObject(jsonObject);
        } catch (IOException e) {
            System.err.println("Error reading player data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public boolean deletePlayerData(UUID playerUUID) {
        File playerDir = new File(this.baseDir, playerUUID.toString());

        if (!playerDir.exists()) {
            System.out.println("Player data for UUID " + playerUUID + " does not exist. Skipping file deletion.");
            return true;
        }

        try {
            Files.walk(playerDir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(f -> {
                        if (!f.delete()) {
                            System.err.println("Failed to delete file/directory: " + f.getAbsolutePath());
                        }
                    });
            System.out.println("Successfully deleted player data for UUID: " + playerUUID);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to delete player data for UUID " + playerUUID + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<UUID> getPlayerWorlds(UUID playerUUID) {
        File playerDir = new File(this.baseDir, playerUUID.toString());
        List<UUID> worldUUIDs = new ArrayList<>();

        File[] playerFiles = playerDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (playerFiles != null) {
            for (File file : playerFiles) {
                if (file.getName().equals(HUB_FILE_NAME)) {
                    // Skip the hub file as it doesn't have a world UUID
                    continue;
                }
                try {
                    String fileName = file.getName();
                    String uuidString = fileName.substring(0, fileName.lastIndexOf('.'));
                    worldUUIDs.add(UUID.fromString(uuidString));
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping invalid file in player directory: " + file.getName());
                }
            }
        }
        return worldUUIDs;
    }
}