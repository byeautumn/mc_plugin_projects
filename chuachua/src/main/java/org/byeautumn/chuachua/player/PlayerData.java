package org.byeautumn.chuachua.player;

import org.bukkit.GameMode;
import org.byeautumn.chuachua.common.PlayMode;
import org.json.JSONObject;

import java.util.UUID;

public final class PlayerData {

    private final UUID playerUUID;
    private final PlayMode playMode;
    private final GameMode gameMode;
    private final UUID worldUUID;
    private final String worldInternalName;
    private final UUID lastKnownLogoffWorldUUID;
    private final double lastKnownLogoffX;
    private final double lastKnownLogoffY;
    private final double lastKnownLogoffZ;
    private final float lastKnownLogoffPitch;
    private final float lastKnownLogoffYaw;
    private final double health;
    private final int hunger;
    private final double hydration;
    private final double temperature;
    private final String potionEffects;

    private PlayerData(Builder builder) {
        this.playerUUID = builder.playerUUID;
        this.playMode = builder.playMode;
        this.gameMode = builder.gameMode;
        this.worldUUID = builder.worldUUID;
        this.worldInternalName = builder.worldInternalName;
        this.lastKnownLogoffWorldUUID = builder.lastKnownLogoffWorldUUID;
        this.lastKnownLogoffX = builder.lastKnownLogoffX;
        this.lastKnownLogoffY = builder.lastKnownLogoffY;
        this.lastKnownLogoffZ = builder.lastKnownLogoffZ;
        this.lastKnownLogoffPitch = builder.lastKnownLogoffPitch;
        this.lastKnownLogoffYaw = builder.lastKnownLogoffYaw;
        this.health = builder.health;
        this.hunger = builder.hunger;
        this.hydration = builder.hydration;
        this.temperature = builder.temperature;
        this.potionEffects = builder.potionEffects;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public PlayMode getPlayMode() {
        return playMode;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public UUID getWorldUUID() {
        return worldUUID;
    }

    public String getWorldInternalName() {
        return worldInternalName;
    }

    public UUID getLastKnownLogoffWorldUUID() {
        return lastKnownLogoffWorldUUID;
    }

    public double getLastKnownLogoffX() {
        return lastKnownLogoffX;
    }

    public double getLastKnownLogoffY() {
        return lastKnownLogoffY;
    }

    public double getLastKnownLogoffZ() {
        return lastKnownLogoffZ;
    }

    public float getLastKnownLogoffPitch() {
        return lastKnownLogoffPitch;
    }

    public float getLastKnownLogoffYaw() {
        return lastKnownLogoffYaw;
    }

    public double getHealth() {
        return health;
    }

    public int getHunger() {
        return hunger;
    }

    public double getHydration() {
        return hydration;
    }

    public double getTemperature() {
        return temperature;
    }

    public String getPotionEffects() {
        return potionEffects;
    }

    public String toJson() {
        return toJsonObject().toString(4);
    }

    private JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("playerUUID", this.playerUUID.toString());
        jsonObject.put("playMode", this.playMode.toString());
        jsonObject.put("gameMode", this.gameMode.toString());
        jsonObject.put("worldUUID", this.worldUUID.toString());
        jsonObject.put("worldInternalName", this.worldInternalName);
        if (this.lastKnownLogoffWorldUUID != null) {
            jsonObject.put("lastKnownLogoffWorldUUID", this.lastKnownLogoffWorldUUID.toString());
            jsonObject.put("lastKnownLogoffX", this.lastKnownLogoffX);
            jsonObject.put("lastKnownLogoffY", this.lastKnownLogoffY);
            jsonObject.put("lastKnownLogoffZ", this.lastKnownLogoffZ);
            jsonObject.put("lastKnownLogoffPitch", this.lastKnownLogoffPitch);
            jsonObject.put("lastKnownLogoffYaw", this.lastKnownLogoffYaw);
        }
        jsonObject.put("health", this.health);
        jsonObject.put("hunger", this.hunger);
        jsonObject.put("hydration", this.hydration);
        jsonObject.put("temperature", this.temperature);
        jsonObject.put("potionEffects", this.potionEffects);
        return jsonObject;
    }

    public static PlayerData fromJsonObject(JSONObject jsonObject) {
        try {
            UUID lastKnownLogoffWorldUUID = jsonObject.has("lastKnownLogoffWorldUUID") ? UUID.fromString(jsonObject.getString("lastKnownLogoffWorldUUID")) : null;
            double lastKnownLogoffX = jsonObject.optDouble("lastKnownLogoffX", 0.0);
            double lastKnownLogoffY = jsonObject.optDouble("lastKnownLogoffY", 0.0);
            double lastKnownLogoffZ = jsonObject.optDouble("lastKnownLogoffZ", 0.0);
            float lastKnownLogoffPitch = (float) jsonObject.optDouble("lastKnownLogoffPitch", 0.0f);
            float lastKnownLogoffYaw = (float) jsonObject.optDouble("lastKnownLogoffYaw", 0.0f);

            return PlayerData.builder()
                    .playerUUID(UUID.fromString(jsonObject.getString("playerUUID")))
                    .playMode(PlayMode.valueOf(jsonObject.optString("playMode", PlayMode.UNKNOWN.toString())))
                    .gameMode(GameMode.valueOf(jsonObject.optString("gameMode", GameMode.ADVENTURE.toString())))
                    .worldUUID(UUID.fromString(jsonObject.getString("worldUUID")))
                    .worldInternalName(jsonObject.getString("worldInternalName"))
                    .lastKnownLogoffWorldUUID(lastKnownLogoffWorldUUID)
                    .lastKnownLogoffX(lastKnownLogoffX)
                    .lastKnownLogoffY(lastKnownLogoffY)
                    .lastKnownLogoffZ(lastKnownLogoffZ)
                    .lastKnownLogoffPitch(lastKnownLogoffPitch)
                    .lastKnownLogoffYaw(lastKnownLogoffYaw)
                    .health(jsonObject.optDouble("health", 20.0))
                    .hunger(jsonObject.optInt("hunger", 20))
                    .hydration(jsonObject.optDouble("hydration", 100.0))
                    .temperature(jsonObject.optDouble("temperature", 20.0))
                    .potionEffects(jsonObject.optString("potionEffects", "[]"))
                    .build();
        } catch (Exception e) {
            System.err.println("Error parsing JSON to PlayerData object: " + e.getMessage());
            return null;
        }
    }

    public static class Builder {
        private UUID playerUUID;
        private PlayMode playMode;
        private GameMode gameMode;
        private UUID worldUUID;
        private String worldInternalName;
        private UUID lastKnownLogoffWorldUUID;
        private double lastKnownLogoffX;
        private double lastKnownLogoffY;
        private double lastKnownLogoffZ;
        private float lastKnownLogoffPitch;
        private float lastKnownLogoffYaw;
        private double health;
        private int hunger;
        private double hydration;
        private double temperature;
        private String potionEffects;

        public Builder playerUUID(UUID playerUUID) {
            this.playerUUID = playerUUID;
            return this;
        }

        public Builder playMode(PlayMode playMode) {
            this.playMode = playMode;
            return this;
        }

        public Builder gameMode(GameMode gameMode) {
            this.gameMode = gameMode;
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

        public Builder lastKnownLogoffWorldUUID(UUID lastKnownLogoffWorldUUID) {
            this.lastKnownLogoffWorldUUID = lastKnownLogoffWorldUUID;
            return this;
        }

        public Builder lastKnownLogoffX(double lastKnownLogoffX) {
            this.lastKnownLogoffX = lastKnownLogoffX;
            return this;
        }

        public Builder lastKnownLogoffY(double lastKnownLogoffY) {
            this.lastKnownLogoffY = lastKnownLogoffY;
            return this;
        }

        public Builder lastKnownLogoffZ(double lastKnownLogoffZ) {
            this.lastKnownLogoffZ = lastKnownLogoffZ;
            return this;
        }

        public Builder lastKnownLogoffPitch(float lastKnownLogoffPitch) {
            this.lastKnownLogoffPitch = lastKnownLogoffPitch;
            return this;
        }

        public Builder lastKnownLogoffYaw(float lastKnownLogoffYaw) {
            this.lastKnownLogoffYaw = lastKnownLogoffYaw;
            return this;
        }

        public Builder health(double health) {
            this.health = health;
            return this;
        }

        public Builder hunger(int hunger) {
            this.hunger = hunger;
            return this;
        }

        public Builder hydration(double hydration) {
            this.hydration = hydration;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder potionEffects(String potionEffects) {
            this.potionEffects = potionEffects;
            return this;
        }

        public PlayerData build() {
            if (playerUUID == null || playMode == null || gameMode == null || worldUUID == null || worldInternalName == null) {
                throw new IllegalStateException("Required fields (playerUUID, playMode, gameMode, worldUUID, worldInternalName) must be set.");
            }
            return new PlayerData(this);
        }
    }
}