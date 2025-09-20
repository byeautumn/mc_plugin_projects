package org.byeautumn.chuachua.game.firstland;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.byeautumn.chuachua.Chuachua;
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
import java.util.stream.Collectors;

public class WorldDataAccessor implements Accessor {

    private final File baseDir;
    // This is the correct placement for the constant
    private static final String UNCONNECTED_DIR_NAME = "unconnected";
    private static final String CONNECTED_DIR_NAME = "connected";
    private static final String WORLD_DATA_FILE_NAME = "world-data.json";

    public WorldDataAccessor(File baseDir) {
        this.baseDir = new File(baseDir, "world-data"); // No more appending "world-data" here
        createDirectories(this.baseDir);
    }

    @Override
    public void createDirectories(File baseDir) {
        try {
            if (!baseDir.exists()) {
                Files.createDirectories(baseDir.toPath());
                System.out.println("Created base directory: " + baseDir.getAbsolutePath());
            }

            File unconnectedDir = new File(baseDir, UNCONNECTED_DIR_NAME);
            if (!unconnectedDir.exists()) {
                Files.createDirectories(unconnectedDir.toPath());
                System.out.println("Created unconnected directory: " + unconnectedDir.getAbsolutePath());
            }

            File connectedDir = new File(baseDir, CONNECTED_DIR_NAME);
            if (!connectedDir.exists()) {
                Files.createDirectories(connectedDir.toPath());
                System.out.println("Created connected directory: " + connectedDir.getAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Failed to create directories: " + e.getMessage());
            throw new RuntimeException("Could not create world data directories", e);
        }
    }


    public void updateWorldData(WorldData worldData, WorldDataPlayers worldDataPlayers) {
        saveWorldData(worldData);

        saveWorldDataPlayers(worldDataPlayers);
    }

    public void connectUnownedWorldToPlayer(UUID worldUUID, UUID ownerUUID, String friendlyName, List<UUID> players, JavaPlugin plugin) {
        File oldUnconnectedDir = new File(new File(baseDir, UNCONNECTED_DIR_NAME), worldUUID.toString());
        File oldDataFile = new File(oldUnconnectedDir, WORLD_DATA_FILE_NAME);
        File oldPlayersFile = new File(oldUnconnectedDir, "players.json");

        if (!oldDataFile.exists()) {
            plugin.getLogger().severe("Unowned world file not found: " + oldDataFile.getAbsolutePath());
            return;
        }

        JSONObject jsonObject = loadJsonObjectFromFile(oldDataFile, plugin);
        if (jsonObject == null) {
            plugin.getLogger().severe("Failed to load JSON from file: " + oldDataFile.getAbsolutePath());
            return;
        }

        try {
            // Get creationDate from the existing JSON and use it
            long creationDate = jsonObject.getLong("creationDate");

            WorldData ownedWorldData = WorldData.builder()
                    .worldUUID(UUID.fromString(jsonObject.getString("worldUUID")))
                    .worldInternalName(jsonObject.getString("worldInternalName"))
                    .worldFriendlyName(friendlyName)
                    .seed(jsonObject.getLong("seed"))
                    .creationDate(creationDate) // This line has been added
                    .ownerUUID(ownerUUID)
                    .build();

            // Save the updated WorldData to the new 'connected' location
            saveWorldData(ownedWorldData);

            // Save the players.json file to the new 'connected' location
            WorldDataPlayers worldDataPlayers = WorldDataPlayers.builder()
                    .worldUUID(worldUUID)
                    .players(players)
                    .build();
            saveWorldDataPlayers(worldDataPlayers);

            // FIX: Delete the old files and directory
            // Delete the players file first, if it exists
            if (oldPlayersFile.exists()) {
                if (!oldPlayersFile.delete()) {
                    plugin.getLogger().severe("Failed to delete old players.json file: " + oldPlayersFile.getAbsolutePath());
                }
            }

            // Delete the world-data.json file
            if (oldDataFile.exists()) {
                if (!oldDataFile.delete()) {
                    plugin.getLogger().severe("Failed to delete old world-data.json file: " + oldDataFile.getAbsolutePath());
                }
            }

            // Now, delete the parent directory which should be empty
            if (oldUnconnectedDir.exists()) {
                if (!oldUnconnectedDir.delete()) {
                    plugin.getLogger().severe("Failed to delete old unconnected directory: " + oldUnconnectedDir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error processing or saving world data: " + e.getMessage());
            e.printStackTrace();
        }
    }public UUID getFirstUnownedWorldUUID() {
        File unconnectedDir = new File(baseDir, UNCONNECTED_DIR_NAME);

        // Check if the directory exists and is a directory
        System.out.println("Checking directory: " + unconnectedDir.getAbsolutePath());
        System.out.println("Directory exists: " + unconnectedDir.exists());
        System.out.println("Is a directory: " + unconnectedDir.isDirectory());

        File[] subdirectories = unconnectedDir.listFiles(File::isDirectory);

        // Check the result of listFiles
        if (subdirectories == null) {
            System.out.println("subdirectories is null. The directory might not exist or there's a permissions issue.");
        } else {
            System.out.println("Number of subdirectories found: " + subdirectories.length);
        }

        if (subdirectories != null) {
            System.out.println(subdirectories.length + "getFirstUnownedWorldUUID: subdirectories.length ");
            for (File subDir : subdirectories) {
                System.out.println(subDir + "getFirstUnownedWorldUUID: subDir ");
                try {
                    // The subdirectory name should be the UUID
                    String uuidString = subDir.getName();
                    System.out.println(uuidString + "getFirstUnownedWorldUUID: uuidString ");
                    UUID worldUUID = UUID.fromString(uuidString);
                    System.out.println(worldUUID + "getFirstUnownedWorldUUID: worldUUID ");

                    // Corrected line: Check for the file with the .json extension
                    File worldDataFile = new File(subDir, WORLD_DATA_FILE_NAME);
                    System.out.println(worldDataFile.toPath().toString() + "getFirstUnownedWorldUUID: worldDataFile ");


                    if (worldDataFile.exists()) {
                        return worldUUID;
                    }
                } catch (IllegalArgumentException e) {
                    // Handle cases where the subdirectory name is not a valid UUID
                    System.err.println("Skipping invalid directory in unconnected directory: " + subDir.getName());
                }
            }
        }
        return null;
    }

    private void saveWorldDataPlayers(WorldDataPlayers worldDataPlayers) {
        if (worldDataPlayers != null) {
            File parentDir = new File(baseDir, CONNECTED_DIR_NAME);
            File worldDir = new File(parentDir, worldDataPlayers.getWorldUUID().toString());

            try {
                Files.createDirectories(worldDir.toPath());
                File playersDataFile = new File(worldDir, "players.json");
                try (FileWriter writer = new FileWriter(playersDataFile)) {
                    writer.write(worldDataPlayers.toJson());
                }
            } catch (IOException e) {
                System.err.println("Error saving players.json for world " + worldDataPlayers.getWorldUUID() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieves the WorldData object for a given world UUID.
     * It checks both player-owned and unconnected world directories.
     *
     * @param worldUUID The UUID of the world to retrieve.
     * @return The WorldData object, or null if not found.
     */
    public WorldData getWorldData(UUID worldUUID) {
        // 1. Check for the world in the 'connected' directory
        File connectedWorldDir = new File(new File(baseDir, CONNECTED_DIR_NAME), worldUUID.toString());
        File connectedWorldDataFile = new File(connectedWorldDir, WORLD_DATA_FILE_NAME);
        if (connectedWorldDataFile.exists()) {
            return loadWorldDataFromFile(connectedWorldDataFile);
        }

        // 2. Check the 'unconnected' directory
        File unconnectedDir = new File(new File(baseDir, UNCONNECTED_DIR_NAME), worldUUID.toString());
        File unconnectedWorldDataFile = new File(unconnectedDir, WORLD_DATA_FILE_NAME);
        if (unconnectedWorldDataFile.exists()) {
            System.out.println("Found unconnected world data file: " + unconnectedWorldDataFile.getAbsolutePath());
            return loadWorldDataFromFile(unconnectedWorldDataFile);
        }
        System.out.println("World data not found for UUID: " + worldUUID);

        return null;
    }


    /**
     * Retrieves the JSON object for the players.json file of a specific world.
     *
     * @param worldUUID The UUID of the world.
     * @return A JSONObject representing the world's players data, or null if the file does not exist.
     */
    public WorldDataPlayers getWorldDataPlayers(UUID worldUUID) {
        File worldDir = new File(baseDir, worldUUID.toString());
        File playersJsonFile = new File(worldDir, "players.json");
        if (playersJsonFile.exists()) {
            try (FileReader reader = new FileReader(playersJsonFile)) {
                StringBuilder sb = new StringBuilder();
                int character;
                while ((character = reader.read()) != -1) {
                    sb.append((char) character);
                }
                return WorldDataPlayers.fromJsonObject(new JSONObject(sb.toString()));
            } catch (IOException e) {
                System.err.println("Error reading players.json for world " + worldUUID + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Saves a WorldData object to a file. This method now correctly
     * separates unconnected worlds into their own directory.
     *
     * @param worldData The WorldData object to save.
     */
    public void saveWorldData(WorldData worldData) {
        File parentDir;
        if (worldData.getOwnerUUID() == null) {
            parentDir = new File(baseDir, UNCONNECTED_DIR_NAME);
        } else {
            parentDir = new File(baseDir, CONNECTED_DIR_NAME);
        }

        File worldDir = new File(parentDir, worldData.getWorldUUID().toString());

        // Use Files.createDirectories for safety and to create parent folders if needed
        try {
            Files.createDirectories(worldDir.toPath());
            File worldFile = new File(worldDir, WORLD_DATA_FILE_NAME);
            try (FileWriter writer = new FileWriter(worldFile)) {
                writer.write(worldData.toJson());
            }
        } catch (IOException e) {
            System.err.println("Error saving world data for UUID " + worldData.getWorldUUID() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Deletes all data associated with a specific world. It now checks both
     * the player-owned and unconnected directories.
     *
     * @param worldUUID The UUID of the world to delete.
     * @return true if the world data was successfully deleted, false otherwise.
     */
    public boolean deleteWorld(UUID worldUUID) {
        // 1. Check for the world in the 'connected' directory (owned worlds)
        File connectedWorldDir = new File(new File(baseDir, CONNECTED_DIR_NAME), worldUUID.toString());

        // 2. Check for the world in the 'unconnected' directory (unowned worlds)
        File unconnectedWorldDir = new File(new File(baseDir, UNCONNECTED_DIR_NAME), worldUUID.toString());

        File dirToDelete = null;

        if (connectedWorldDir.exists() && connectedWorldDir.isDirectory()) {
            dirToDelete = connectedWorldDir;
        } else if (unconnectedWorldDir.exists() && unconnectedWorldDir.isDirectory()) {
            dirToDelete = unconnectedWorldDir;
        }

        if (dirToDelete == null) {
            System.out.println("World data for UUID " + worldUUID + " does not exist. Skipping file deletion.");
            return true;
        }

        try {
            Files.walk(dirToDelete.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(f -> {
                        if (!f.delete()) {
                            System.err.println("Failed to delete file/directory: " + f.getAbsolutePath());
                        }
                    });

            System.out.println("Successfully deleted world data for UUID: " + worldUUID);
            return true;
        } catch (IOException e) {
            System.err.println("Failed to delete world data for UUID " + worldUUID + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void createWorldData(UUID worldUUID, UUID ownerUUID, String friendlyName, String worldInternalName, long seed, List<UUID> players) {
        // 1. Create the world's directory structure in the server's main world container
        File worldFolder = new File(Bukkit.getWorldContainer(), worldInternalName);
        Path worldPath = worldFolder.toPath();
        try {
            // Create the main directory and the 'data' subdirectory for Minecraft saves
            Files.createDirectories(worldPath.resolve("data"));
        } catch (IOException e) {
            // Handle failure to create directories, as the server can't save without them
            System.err.println("Failed to create world directories for: " + worldInternalName);
            e.printStackTrace();
            return;
        }

        // 2. Now that the directories exist, save the world data to the plugin's data folder
        WorldData worldData = WorldData.builder()
                .worldUUID(worldUUID)
                .ownerUUID(ownerUUID)
                .worldInternalName(worldInternalName)
                .worldFriendlyName(friendlyName)
                .seed(seed)
                .creationDate(System.currentTimeMillis())
                .build();
        saveWorldData(worldData);

        // 3. Save player data if the world is owned
        if (ownerUUID != null) {
            WorldDataPlayers worldDataPlayers = WorldDataPlayers.builder()
                    .worldUUID(worldUUID)
                    .players(players)
                    .build();
            saveWorldDataPlayers(worldDataPlayers);
        }
    }

    /**
     * Finds the UUID of the first available unconnected world by looking
     * specifically in the 'unconnected' directory.
     *
     * @return The UUID of the first unconnected world found, or null if none exist.
     */
    public UUID findFirstUnconnectedWorldUUID() {
        File unconnectedDir = new File(baseDir, UNCONNECTED_DIR_NAME);
        File[] jsonFiles = unconnectedDir.listFiles((dir, name) -> name.endsWith(".json"));

        if (jsonFiles != null) {
            for (File jsonFile : jsonFiles) {
                try {
                    String fileName = jsonFile.getName();
                    String uuidString = fileName.substring(0, fileName.lastIndexOf('.'));
                    return UUID.fromString(uuidString);
                } catch (IllegalArgumentException e) {
                    System.err.println("Skipping invalid file in unconnected directory: " + jsonFile.getName());
                }
            }
        }
        return null;
    }

    public void addPlayerToWorld(UUID playerUUID, UUID worldUUID) {
        File worldDir = new File(baseDir, worldUUID.toString());
        File playersFile = new File(worldDir, "players.json");

        if (!playersFile.exists()) {
            System.err.println("Players file not found for world: " + worldUUID);
            return;
        }

        try (FileReader reader = new FileReader(playersFile)) {
            StringBuilder sb = new StringBuilder();
            int character;
            while ((character = reader.read()) != -1) {
                sb.append((char) character);
            }
            JSONObject jsonObject = new JSONObject(sb.toString());
            JSONArray playersArray = jsonObject.getJSONArray("players");

            if (!isPlayerInArray(playersArray, playerUUID)) {
                playersArray.put(playerUUID.toString());

                try (FileWriter writer = new FileWriter(playersFile)) {
                    writer.write(jsonObject.toString(4));
                }
                System.out.println("Player " + playerUUID + " added to world " + worldUUID);
            } else {
                System.out.println("Player " + playerUUID + " is already in world " + worldUUID);
            }
        } catch (IOException e) {
            System.err.println("Error accessing players file for world " + worldUUID + ": " + e.getMessage());
            e.printStackTrace();
        }
    }


    private boolean isPlayerInArray(JSONArray playersArray, UUID playerUUID) {
        for (int i = 0; i < playersArray.length(); i++) {
            if (playersArray.getString(i).equals(playerUUID.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves a list of UUIDs for all worlds owned by a specific player.
     * It now only checks the player-owned world directories for efficiency.
     *
     * @param playerUUID The UUID of the player.
     * @return A list of UUIDs for worlds owned by the player.
     */
    public List<UUID> getPlayerOwnedWorldUUIDs(UUID playerUUID) {
        List<UUID> ownedWorlds = new ArrayList<>();

        // Get the connected worlds directory
        File connectedWorldsDir = new File(baseDir, CONNECTED_DIR_NAME);
        File[] worldDirectories = connectedWorldsDir.listFiles(File::isDirectory);

        if (worldDirectories != null) {
            for (File worldDir : worldDirectories) {
                // Check for the world data file inside each subdirectory
                File worldDataFile = new File(worldDir, WORLD_DATA_FILE_NAME);

                if (worldDataFile.exists()) {
                    WorldData worldData = loadWorldDataFromFile(worldDataFile);
                    if (worldData != null && worldData.getOwnerUUID() != null && worldData.getOwnerUUID().equals(playerUUID)) {
                        ownedWorlds.add(worldData.getWorldUUID());
                    }
                }
            }
        }
        return ownedWorlds;
    }

    /**
     * Helper method to load WorldData from a given file path.
     *
     * @param worldDataFile The file to read.
     * @return The WorldData object, or null if an error occurs.
     */
    private WorldData loadWorldDataFromFile(File worldDataFile) {
        try (FileReader reader = new FileReader(worldDataFile)) {
            StringBuilder sb = new StringBuilder();
            int character;
            while ((character = reader.read()) != -1) {
                sb.append((char) character);
            }
            System.out.println(sb.toString());
            JSONObject jsonObject = new JSONObject(sb.toString());
            return WorldData.fromJsonObject(jsonObject);
        } catch (IOException e) {
            System.err.println("Error reading world data from file " + worldDataFile.getAbsolutePath() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject loadJsonObjectFromFile(File file, JavaPlugin plugin) {
        try (FileReader reader = new FileReader(file)) {
            StringBuilder sb = new StringBuilder();
            int character;
            while ((character = reader.read()) != -1) {
                sb.append((char) character);
            }
            return new JSONObject(sb.toString());
        } catch (IOException e) {
            plugin.getLogger().severe("Error reading JSON from file " + file.getAbsolutePath() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves the maximum number of worlds a player is allowed to own from the main plugin's configuration.
     * This setting is now read from the main config.yml.
     * @return The maximum number of worlds, or the default from Chuachua.java if not specified.
     */
    public int getMaxWorldsPerPlayer(JavaPlugin plugin) {
        if (plugin instanceof Chuachua) {
            return plugin.getConfig().getInt("max-worlds-per-player", 3);
        }
        return 3;
    }

    public boolean worldExistsInConfig(UUID worldUUID) {
        // Check if a player-owned world directory with this UUID exists
        File worldDir = new File(baseDir, worldUUID.toString());
        if (worldDir.exists() && worldDir.isDirectory()) {
            return true;
        }
        // Check if an unconnected world file with this UUID exists
        File unconnectedFile = new File(new File(baseDir, UNCONNECTED_DIR_NAME), WORLD_DATA_FILE_NAME);
        return unconnectedFile.exists() && unconnectedFile.isFile();
    }

    /**
     * Retrieves a list of all World UUIDs stored in the configuration by scanning the file system.
     * This includes both player-owned worlds and unconnected worlds.
     * @return A list of World UUIDs.
     */
    public List<UUID> getKnownWorldUUIDs() {
        List<UUID> worldUUIDs = new ArrayList<>();

        // 1. Scan player-owned world directories
        File[] playerWorldDirs = baseDir.listFiles(File::isDirectory);
        if (playerWorldDirs != null) {
            for (File dir : playerWorldDirs) {
                // Skip the unconnected directory
                if (dir.getName().equals(UNCONNECTED_DIR_NAME)) {
                    continue;
                }
                try {
                    UUID worldUUID = UUID.fromString(dir.getName());
                    worldUUIDs.add(worldUUID);
                } catch (IllegalArgumentException e) {
                    // Ignore directories that are not valid UUIDs
                }
            }
        }

        // 2. Scan unconnected world files
        File unconnectedDir = new File(baseDir, UNCONNECTED_DIR_NAME);
        File[] unconnectedFiles = unconnectedDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (unconnectedFiles != null) {
            for (File file : unconnectedFiles) {
                String fileName = file.getName();
                String uuidString = fileName.substring(0, fileName.lastIndexOf('.'));
                try {
                    UUID worldUUID = UUID.fromString(uuidString);
                    worldUUIDs.add(worldUUID);
                } catch (IllegalArgumentException e) {
                    // Ignore files that are not valid UUIDs
                }
            }
        }
        return worldUUIDs;
    }
}