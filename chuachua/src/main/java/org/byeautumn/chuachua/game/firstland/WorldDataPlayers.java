package org.byeautumn.chuachua.game.firstland;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * A data class representing the players associated with a world, using the Builder pattern.
 * This class is immutable once an instance is created.
 */
public final class WorldDataPlayers {

    private final List<UUID> players;
    private final UUID worldUUID;

    /**
     * Private constructor to enforce object creation via the Builder.
     * @param builder The builder instance containing the field values.
     */
    private WorldDataPlayers(Builder builder) {
        this.players = builder.players;
        this.worldUUID = builder.worldUUID;
    }

    /**
     * Static factory method to get a new Builder instance.
     * @return A new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    // --- Getter Methods ---
    public List<UUID> getPlayers() {
        return players;
    }

    public UUID getWorldUUID() {
        return worldUUID;
    }

    /**
     * Converts this WorldDataPlayers object to a JSON string.
     * @return The JSON string representation of the object.
     */
    public String toJson() {
        return toJsonObject().toString(4);
    }

    private JSONObject toJsonObject(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("worldUUID", this.worldUUID.toString());

        JSONArray playersArray = new JSONArray();
        for (UUID playerUUID : this.players) {
            playersArray.put(playerUUID.toString());
        }
        jsonObject.put("players", playersArray);

        return jsonObject;
    }

    /**
     * Creates a WorldDataPlayers object from a JSONObject.
     *
     * @param jsonObject The JSON object containing the player's data.
     * @return A new WorldDataPlayers instance, or null if the JSON is invalid.
     */
    public static WorldDataPlayers fromJsonObject(JSONObject jsonObject) {
        try {
            UUID worldUUID = UUID.fromString(jsonObject.getString("worldUUID"));
            JSONArray playersArray = jsonObject.getJSONArray("players");

            List<UUID> playersList = new ArrayList<>();
            for (int i = 0; i < playersArray.length(); i++) {
                playersList.add(UUID.fromString(playersArray.getString(i)));
            }

            return WorldDataPlayers.builder()
                    .worldUUID(worldUUID)
                    .players(playersList)
                    .build();
        } catch (Exception e) {
            // Catches missing keys and invalid UUID formats
            System.err.println("Error parsing JSON to WorldDataPlayers object: " + e.getMessage());
            return null;
        }
    }

    /**
     * A static nested class that implements the Builder pattern
     * for the WorldDataPlayers class.
     */
    public static class Builder {
        private List<UUID> players;
        private UUID worldUUID;

        public Builder players(List<UUID> players) {
            this.players = players;
            return this;
        }

        public Builder worldUUID(UUID worldUUID) {
            this.worldUUID = worldUUID;
            return this;
        }

        /**
         * Builds and returns a new WorldDataPlayers instance.
         * Throws an IllegalStateException if any required field is not set.
         * @return A new WorldDataPlayers instance.
         */
        public WorldDataPlayers build() {
            if (players == null || worldUUID == null) {
                throw new IllegalStateException("Required fields (players, worldUUID) must be set.");
            }
            return new WorldDataPlayers(this);
        }
    }

    public static void main(String[] args) {
        // Example of how to use the Builder pattern to create a WorldDataPlayers object
        UUID exampleWorldUUID = UUID.randomUUID();
        List<UUID> examplePlayersList = Arrays.asList(UUID.randomUUID(), UUID.randomUUID());


        WorldDataPlayers worldPlayers = WorldDataPlayers.builder()
                .worldUUID(exampleWorldUUID)
                .players(examplePlayersList)
                .build();

        // Print the data to show it's correctly built
        System.out.println("Created WorldDataPlayers object:");
        System.out.println("World UUID: " + worldPlayers.getWorldUUID());
        System.out.println("Players: " + worldPlayers.getPlayers());

        System.out.println("\nJSON representation:");
        System.out.println(worldPlayers.toJson());
    }
}