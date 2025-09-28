package org.byeautumn.chuachua.player.matrix;

import org.json.JSONObject;
import org.json.JSONException;

/**
 * Represents a player's nutrition stats.
 */
public final class PlayerNutritionMatrix {

    private final double fat;
    private final double protein;
    private final double carbohydrates;

    private PlayerNutritionMatrix(Builder builder) {
        this.fat = builder.fat;
        this.protein = builder.protein;
        this.carbohydrates = builder.carbohydrates;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder(){
        return new Builder(this);
    }

    public double getFat() {
        return fat;
    }

    public double getProtein() {
        return protein;
    }

    public double getCarbohydrates() {
        return carbohydrates;
    }

    public String toJson() {
        return toJsonObject().toString(4);
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fat", this.fat);
            jsonObject.put("protein", this.protein);
            jsonObject.put("carbohydrates", this.carbohydrates);
        } catch (JSONException e) {
            System.err.println("Error creating JSON object: " + e.getMessage());
        }
        return jsonObject;
    }

    public static PlayerNutritionMatrix fromJsonObject(JSONObject jsonObject) {
        try {
            return PlayerNutritionMatrix.builder()
                    .fat(jsonObject.optDouble("fat", 0.0))
                    .protein(jsonObject.optDouble("protein", 0.0))
                    .carbohydrates(jsonObject.optDouble("carbohydrates", 0.0))
                    .build();
        } catch (Exception e) {
            System.err.println("Error parsing JSONObject to PlayerNutritionMatrix object: " + e.getMessage());
            return null;
        }
    }

    public static class Builder {
        private double fat;
        private double protein;
        private double carbohydrates;

        public Builder() {
            this.fat = 0.0;
            this.protein = 0.0;
            this.carbohydrates = 0.0;
        }

        public Builder(PlayerNutritionMatrix matrix) {
            this.fat = matrix.getFat();
            this.protein = matrix.getProtein();
            this.carbohydrates = matrix.getCarbohydrates();
        }

        public Builder fat(double fat) {
            this.fat = fat;
            return this;
        }

        public Builder protein(double protein) {
            this.protein = protein;
            return this;
        }

        public Builder carbohydrates(double carbohydrates) {
            this.carbohydrates = carbohydrates;
            return this;
        }

        public PlayerNutritionMatrix build() {
            return new PlayerNutritionMatrix(this);
        }
    }
}
