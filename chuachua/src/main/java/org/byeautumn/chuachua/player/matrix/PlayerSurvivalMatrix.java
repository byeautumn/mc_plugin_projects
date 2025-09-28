package org.byeautumn.chuachua.player.matrix;

import org.json.JSONObject;
import org.json.JSONException;

/**
 * Represents a player's survival stats, including hydration, body temperature,
 * and a nested PlayerNutritionMatrix.
 */

/**
 * Max fat (100) - 30 days decay rate 10/3. 0 = DEATH
 *
 * Max hydration (100) - 5 days decay rate -20 per day. 0 = DEATH
 *
 * Max Protein (100) - 70 days decay rate of 7/10. 0 = DEATH
 *
 * Max Carbs (100) - No Energy, 5 days.
 */
public final class PlayerSurvivalMatrix {


    private final double hydration;
    private final double bodyTemp;
    private final PlayerNutritionMatrix playerNutrition;

    private PlayerSurvivalMatrix(Builder builder) {
        this.hydration = builder.hydration;
        this.bodyTemp = builder.bodyTemp;
        this.playerNutrition = builder.playerNutrition;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder(){
        return new Builder(this);
    }

    public double getHydration() {
        return hydration;
    }

    public double getBodyTemp() {
        return bodyTemp;
    }

    public PlayerNutritionMatrix getPlayerNutrition() {
        return playerNutrition;
    }

    public String toJson() {
        return toJsonObject().toString(4);
    }

    public JSONObject toJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("hydration", this.hydration);
            jsonObject.put("bodyTemp", this.bodyTemp);
            if (this.playerNutrition != null) {
                jsonObject.put("playerNutrition", this.playerNutrition.toJsonObject());
            }
        } catch (JSONException e) {
            System.err.println("Error creating JSON object: " + e.getMessage());
        }
        return jsonObject;
    }

    public static PlayerSurvivalMatrix fromJsonObject(JSONObject jsonObject) {
        try {
            PlayerNutritionMatrix nutritionMatrix = null;
            if (jsonObject.has("playerNutrition") && !jsonObject.isNull("playerNutrition")) {
                nutritionMatrix = PlayerNutritionMatrix.fromJsonObject(jsonObject.getJSONObject("playerNutrition"));
            }

            return PlayerSurvivalMatrix.builder()
                    .hydration(jsonObject.optDouble("hydration", 100.0))
                    .bodyTemp(jsonObject.optDouble("bodyTemp", 50.0))
                    .playerNutrition(nutritionMatrix)
                    .build();
        } catch (Exception e) {
            System.err.println("Error parsing JSONObject to PlayerSurvivalMatrix object: " + e.getMessage());
            return null;
        }
    }

    public static class Builder {
        private double hydration;
        private double bodyTemp;
        private PlayerNutritionMatrix playerNutrition;

        public Builder() {
            this.hydration = 100.0;
            this.bodyTemp = 50.0;
            this.playerNutrition = null;
        }

        public Builder(PlayerSurvivalMatrix matrix) {
            this.hydration = matrix.hydration;
            this.bodyTemp = matrix.bodyTemp;
            this.playerNutrition = matrix.playerNutrition;
        }

        public Builder hydration(double hydration) {
            this.hydration = hydration;
            return this;
        }

        public Builder bodyTemp(double bodyTemp) {
            this.bodyTemp = bodyTemp;
            return this;
        }

        public Builder playerNutrition(PlayerNutritionMatrix playerNutrition) {
            this.playerNutrition = playerNutrition;
            return this;
        }

        public PlayerSurvivalMatrix build() {
            return new PlayerSurvivalMatrix(this);
        }
    }
}
