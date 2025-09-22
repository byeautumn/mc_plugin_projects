package org.byeautumn.chuachua.player.matrix;

import org.json.JSONObject;
import org.json.JSONException;

/**
 * Represents a player's abilities as a matrix,
 * with abilities set using the Builder pattern for clean construction.
 */
public class PlayerActivityMatrix {

    private final double walkingAbility;
    private final double miningAbility;
    private final double jumpingAbility;
    private final double swimmingAbility;
    private final double fightingAbility;

    // Private constructor to enforce object creation via the Builder
    private PlayerActivityMatrix(Builder builder) {
        this.walkingAbility = builder.walkingAbility;
        this.miningAbility = builder.miningAbility;
        this.jumpingAbility = builder.jumpingAbility;
        this.swimmingAbility = builder.swimmingAbility;
        this.fightingAbility = builder.fightingAbility;
    }

    /**
     * A static method to get a new Builder instance.
     * This is a common practice to make object creation more readable.
     * @return A new Builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Converts this object to a JSONObject.
     * @return A JSONObject representing the object.
     */
    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("walkingAbility", this.walkingAbility);
            jsonObject.put("miningAbility", this.miningAbility);
            jsonObject.put("jumpingAbility", this.jumpingAbility);
            jsonObject.put("swimmingAbility", this.swimmingAbility);
            jsonObject.put("fightingAbility", this.fightingAbility);
        } catch (JSONException e) {
            System.err.println("Error creating JSON object: " + e.getMessage());
        }
        return jsonObject;
    }

    /**
     * Converts this object to a formatted JSON string.
     * @return A JSON string with a 4-space indent.
     */
    public String toJson() {
        return toJsonObject().toString(4);
    }

    /**
     * Creates a PlayerActivityMatrix object from a JSONObject.
     * @param jsonObject A JSONObject representing the object.
     * @return A new PlayerActivityMatrix object.
     */
    public static PlayerActivityMatrix fromJsonObject(JSONObject jsonObject) {
        try {
            return PlayerActivityMatrix.builder()
                    .walkingAbility(jsonObject.optDouble("walkingAbility", 0.0))
                    .miningAbility(jsonObject.optDouble("miningAbility", 0.0))
                    .jumpingAbility(jsonObject.optDouble("jumpingAbility", 0.0))
                    .swimmingAbility(jsonObject.optDouble("swimmingAbility", 0.0))
                    .fightingAbility(jsonObject.optDouble("fightingAbility", 0.0))
                    .build();
        } catch (Exception e) {
            System.err.println("Error parsing JSONObject to PlayerActivityMatrix: " + e.getMessage());
            return null;
        }
    }

    // Getters for all fields
    public double getWalkingAbility() { return walkingAbility; }
    public double getMiningAbility() { return miningAbility; }
    public double getJumpingAbility() { return jumpingAbility; }
    public double getSwimmingAbility() { return swimmingAbility; }
    public double getFightingAbility() { return fightingAbility; }

    @Override
    public String toString() {
        return "PlayerActivityMatrix {" +
                "walkingAbility=" + walkingAbility +
                ", miningAbility=" + miningAbility +
                ", jumpingAbility=" + jumpingAbility +
                ", swimmingAbility=" + swimmingAbility +
                ", fightingAbility=" + fightingAbility +
                '}';
    }

    /**
     * The Builder class for constructing PlayerActivityMatrix objects.
     */
    public static class Builder {
        private double walkingAbility;
        private double miningAbility;
        private double jumpingAbility;
        private double swimmingAbility;
        private double fightingAbility;

        public Builder() {
            // Default values can be set here if needed
            this.walkingAbility = 0.0;
            this.miningAbility = 0.0;
            this.jumpingAbility = 0.0;
            this.swimmingAbility = 0.0;
            this.fightingAbility = 0.0;
        }

        // Methods to set each field, returning the builder for method chaining
        public Builder walkingAbility(double walkingAbility) {
            this.walkingAbility = walkingAbility;
            return this;
        }

        public Builder miningAbility(double miningAbility) {
            this.miningAbility = miningAbility;
            return this;
        }

        public Builder jumpingAbility(double jumpingAbility) {
            this.jumpingAbility = jumpingAbility;
            return this;
        }

        public Builder swimmingAbility(double swimmingAbility) {
            this.swimmingAbility = swimmingAbility;
            return this;
        }

        public Builder fightingAbility(double fightingAbility) {
            this.fightingAbility = fightingAbility;
            return this;
        }

        // The build() method returns the final object
        public PlayerActivityMatrix build() {
            return new PlayerActivityMatrix(this);
        }
    }
}

