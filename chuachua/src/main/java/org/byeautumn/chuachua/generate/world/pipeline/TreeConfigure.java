package org.byeautumn.chuachua.generate.world.pipeline;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class TreeConfigure {
    private int iterations;
    private double segmentLength;
    private double angle;
    private double angleVariation;
    private String axiom;
    private Map<Character, String> rules;
    private char forwardSymbol;
    private Material logMaterial;
    private Material leafMaterial;

    private TreeConfigure(Builder builder) {
        this.iterations = builder.iterations;
        this.segmentLength = builder.segmentLength;
        this.angle = builder.angle;
        this.angleVariation = builder.angleVariation;
        this.axiom = builder.axiom;
        this.rules = builder.rules;
        this.forwardSymbol = builder.forwardSymbol;
        this.logMaterial = builder.logMaterial;
        this.leafMaterial = builder.leafMaterial;
    }

    public int getIterations() {
        return iterations;
    }

    public double getSegmentLength() {
        return segmentLength;
    }

    public double getAngle() {
        return angle;
    }

    public double getAngleVariation() {
        return angleVariation;
    }

    public String getAxiom() {
        return axiom;
    }

    public Map<Character, String> getRules() {
        return rules;
    }

    public char getForwardSymbol() {
        return forwardSymbol;
    }

    public Material getLogMaterial() {
        return logMaterial;
    }

    public Material getLeafMaterial() {
        return leafMaterial;
    }

    public static class Builder {
        private int iterations = 5; // Default value
        private double segmentLength = 20; // Default value
        private double angle = 22.5; // Default value
        private double angleVariation = 0.7; // Default value
        private String axiom = "F"; // Default value
        private Map<Character, String> rules = new HashMap<>(); // Default value
        private char forwardSymbol = 'F'; // Default value
        private Material logMaterial = Material.OAK_LOG; // Default value
        private Material leafMaterial = Material.OAK_LEAVES; // Default value

        public Builder iterations(int iterations) {
            this.iterations = iterations;
            return this;
        }

        public Builder segmentLength(double segmentLength) {
            this.segmentLength = segmentLength;
            return this;
        }

        public Builder angle(double angle) {
            this.angle = angle;
            return this;
        }

        public Builder angleVariation(double angleVariation) {
            this.angleVariation = angleVariation;
            return this;
        }

        public Builder axiom(String axiom) {
            this.axiom = axiom;
            return this;
        }

        public Builder rules(Map<Character, String> rules) {
            this.rules = rules;
            return this;
        }

        public Builder forwardSymbol(char forwardSymbol) {
            this.forwardSymbol = forwardSymbol;
            return this;
        }

        public Builder logMaterial(Material logMaterial) {
            this.logMaterial = logMaterial;
            return this;
        }

        public Builder leafMaterial(Material leafMaterial) {
            this.leafMaterial = leafMaterial;
            return this;
        }

        public TreeConfigure build() {
            return new TreeConfigure(this);
        }
    }
}
