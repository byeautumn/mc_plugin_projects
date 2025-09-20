package org.byeautumn.chuachua.game.firstland;
import org.json.JSONObject;
import java.util.UUID;

/**
 * A data class representing a First Land world, using the Builder pattern.
 * This class is immutable once an instance is created.
 */
public final class WorldData {

    private final UUID ownerUUID;
    private final UUID worldUUID;
    private final String worldInternalName;
    private final String worldFriendlyName;
    private final long seed;
    private final long creationDate;

    /**
     * Private constructor to enforce object creation via the Builder.
     * @param builder The builder instance containing the field values.
     */
    private WorldData(Builder builder) {
        this.ownerUUID = builder.ownerUUID;
        this.worldUUID = builder.worldUUID;
        this.worldInternalName = builder.worldInternalName;
        this.worldFriendlyName = builder.worldFriendlyName;
        this.seed = builder.seed;
        this.creationDate = builder.creationDate;
    }

    /**
     * Static factory method to get a new Builder instance.
     * @return A new Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    // --- Getter Methods ---
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public UUID getWorldUUID() {
        return worldUUID;
    }

    public String getWorldInternalName() {
        return worldInternalName;
    }

    public String getWorldFriendlyName() {
        return worldFriendlyName;
    }

    public long getSeed(){
        return seed;
    }

    public long getCreationDate() {
        return creationDate;
    }

    /**
     * Converts the WorldData object into a JSON string.
     *
     * @return A JSON string representing the world data.
     */
    public String toJson() {
        return toJsonObject().toString(4); // `4` for pretty printing
    }

    private JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("worldUUID", this.worldUUID.toString());
        jsonObject.put("worldInternalName", this.worldInternalName);
        jsonObject.put("worldFriendlyName", this.worldFriendlyName);
        jsonObject.put("seed", this.seed);
        jsonObject.put("creationDate", this.creationDate);

        // Add the ownerUUID only if it is not null
        if (this.ownerUUID != null) {
            jsonObject.put("ownerUUID", this.ownerUUID.toString());
        }

        return jsonObject;
    }

    /**
     * Creates a WorldData object from a JSONObject.
     *
     * @param jsonObject The JSON object containing the world data.
     * @return A new WorldData instance, or null if the JSON is invalid.
     */
    public static WorldData fromJsonObject(JSONObject jsonObject) {
        try {
            UUID ownerUUID = jsonObject.has("ownerUUID") ? UUID.fromString(jsonObject.getString("ownerUUID")) : null;
            UUID worldUUID = UUID.fromString(jsonObject.getString("worldUUID"));
            String worldInternalName = jsonObject.getString("worldInternalName");
            String worldFriendlyName = jsonObject.getString("worldFriendlyName");
            long seed = jsonObject.getLong("seed");

            // Handle the creationDate field, converting string to long if necessary
            long creationDate;
            if (jsonObject.has("creationDate")) {
                Object creationDateValue = jsonObject.get("creationDate");
                if (creationDateValue instanceof String) {
                    // This is a temporary fix for old data that saved the date as a string
                    System.err.println("Warning: Found creationDate as a string. Please migrate data.");
                    // For now, let's just use the current time
                    creationDate = System.currentTimeMillis();
                } else if (creationDateValue instanceof Number) {
                    creationDate = ((Number) creationDateValue).longValue();
                } else {
                    creationDate = System.currentTimeMillis(); // Default to current time if format is unknown
                }
            } else {
                creationDate = System.currentTimeMillis(); // Default to current time if field is missing
            }


            return WorldData.builder()
                    .ownerUUID(ownerUUID)
                    .worldUUID(worldUUID)
                    .worldInternalName(worldInternalName)
                    .worldFriendlyName(worldFriendlyName)
                    .seed(seed)
                    .creationDate(creationDate)
                    .build();
        } catch (Exception e) {
            // Catches missing keys and invalid UUID formats
            System.err.println("Error parsing JSON to WorldData object: " + e.getMessage());
            return null;
        }
    }

    /**
     * A static nested class that implements the Builder pattern
     * for the WorldData class.
     */
    public static class Builder {
        private UUID ownerUUID;
        private UUID worldUUID;
        private String worldInternalName;
        private String worldFriendlyName;
        private long seed;
        private long creationDate;

        public Builder ownerUUID(UUID ownerUUID) {
            this.ownerUUID = ownerUUID;
            return this;
        }

        public Builder worldUUID(UUID worldUUID) {
            this.worldUUID = worldUUID;
            return this;
        }

        public Builder worldInternalName(String worldInternalName) {
            this.worldInternalName = worldInternalName;
            return this;
        }

        public Builder worldFriendlyName(String worldFriendlyName) {
            this.worldFriendlyName = worldFriendlyName;
            return this;
        }
        public Builder seed(long seed) {
            this.seed = seed;
            return this;
        }

        public Builder creationDate(long creationDate) {
            this.creationDate = creationDate;
            return this;
        }


        /**
         * Builds and returns a new WorldData instance.
         * Throws an IllegalStateException if any required field is not set.
         * @return A new WorldData instance.
         */
        public WorldData build() {
            if (worldUUID == null || worldInternalName == null || creationDate == 0) {
                System.out.println("worldUUID: " + worldUUID);
                System.out.println("worldInternalName: " + worldInternalName);
                System.out.println("worldFriendlyName: " + worldFriendlyName);
                System.out.println("seed: " + seed);
                System.out.println("creationDate: " + creationDate);
                throw new IllegalStateException("Required fields (worldUUID, worldInternalName, creationDate) must be set.");
            }
            return new WorldData(this);
        }
    }
}