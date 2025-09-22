package org.byeautumn.chuachua.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.byeautumn.chuachua.Universe;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataAccessor implements Accessor {

    private final File baseDir;
    private static final String HUB_FILE_NAME = "hub.json";

    // In-memory cache using a nested map to handle player data for multiple worlds
    private final Map<UUID, Map<UUID, PlayerData>> playerDataCache;

    public PlayerDataAccessor(File baseDir) {
        this.baseDir = new File(baseDir, "player-data");
        createDirectories(this.baseDir);
        this.playerDataCache = new ConcurrentHashMap<>();
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
        // Update the cache first using the player UUID and world UUID as keys
        playerDataCache.computeIfAbsent(playerData.getPlayerUUID(), k -> new ConcurrentHashMap<>())
                .put(playerData.getWorldUUID(), playerData);

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

    public void updatePlayerData(Player player){
        Location playerLocation = player.getLocation();
        PlayerData currentPlayerData = getPlayerData(player.getUniqueId(), player.getWorld().getUID(), player.getWorld().getName());

        // If data is not found, create a new PlayerData object
        if (currentPlayerData == null) {
            currentPlayerData = PlayerData.builder()
                    .playerUUID(player.getUniqueId())
                    .playMode(Universe.getPlayerTracker(player).getPlayMode())
                    .gameMode(player.getGameMode())
                    .worldUUID(playerLocation.getWorld().getUID())
                    .worldInternalName(playerLocation.getWorld().getName())
                    .lastKnownLogoffWorldUUID(playerLocation.getWorld().getUID())
                    .lastKnownLogoffX(playerLocation.getX())
                    .lastKnownLogoffY(playerLocation.getY())
                    .lastKnownLogoffZ(playerLocation.getZ())
                    .lastKnownLogoffPitch(playerLocation.getPitch())
                    .lastKnownLogoffYaw(playerLocation.getYaw())
                    .health(player.getHealth())
                    .hunger(player.getFoodLevel())
                    .build();
        } else {
            // Otherwise, update the existing object
            currentPlayerData = currentPlayerData.toBuilder()
                    .playMode(Universe.getPlayerTracker(player).getPlayMode())
                    .gameMode(player.getGameMode())
                    .worldUUID(playerLocation.getWorld().getUID())
                    .worldInternalName(playerLocation.getWorld().getName())
                    .health(player.getHealth())
                    .hunger(player.getFoodLevel())
                    .lastKnownLogoffWorldUUID(playerLocation.getWorld().getUID())
                    .lastKnownLogoffX(playerLocation.getX())
                    .lastKnownLogoffY(playerLocation.getY())
                    .lastKnownLogoffZ(playerLocation.getZ())
                    .lastKnownLogoffPitch(playerLocation.getPitch())
                    .lastKnownLogoffYaw(playerLocation.getYaw())
                    .build();
        }

        savePlayerData(currentPlayerData);
    }

    public PlayerData getPlayerData(UUID playerUUID, UUID worldUUID, String worldInternalName) {
        // First, check the cache for the player's data in the specific world
        Map<UUID, PlayerData> playerWorlds = playerDataCache.get(playerUUID);
        if (playerWorlds != null) {
            PlayerData cachedData = playerWorlds.get(worldUUID);
            if (cachedData != null) {
                return cachedData;
            }
        }

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
            PlayerData loadedData = PlayerData.fromJsonObject(jsonObject);

            // Add the newly loaded data to the cache
            if (loadedData != null) {
                playerDataCache.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                        .put(worldUUID, loadedData);
            }

            return loadedData;
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

            // Remove from cache even if files don't exist
            playerDataCache.remove(playerUUID);
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

            // Remove from cache after successful file deletion
            playerDataCache.remove(playerUUID);

            System.out.println("Successfully deleted player data for UUID: " + playerUUID);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to delete player data for UUID " + playerUUID + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Method to manually clear the cache (useful for events like server shutdown)
    public void clearCache() {
        this.playerDataCache.clear();
        System.out.println("Player data cache has been cleared.");
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
