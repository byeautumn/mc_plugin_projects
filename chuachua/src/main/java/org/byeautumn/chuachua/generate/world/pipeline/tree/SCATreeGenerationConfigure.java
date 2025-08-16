package org.byeautumn.chuachua.generate.world.pipeline.tree;

import org.bukkit.Material;

public class SCATreeGenerationConfigure implements TreeGenerationConfigure {
    // SCA Parameters
    private double influenceRadius; // How far a branch 'sees' attraction points
    private double killRadius;      // How close a branch needs to be to 'consume' an attraction point
    private double branchLength;    // Length of each new branch segment
    private int maxIterations;      // Max growth steps for the tree
    private int attractionPointCount; // Number of attraction points in the cloud

    // Tree Structure Parameters
    private int trunkHeight;       // Height of the initial, straight trunk
    private double canopyHeight;    // Approximate height of the canopy above the trunk
    private double canopyRadius;    // Approximate radius of the canopy
    private double branchRandomness; // How much randomness to add to branch direction (0.0 for none)

    // Leaf Parameters
    private double leafPlacementRadius; // How far leaves extend from branches
    private double leafDensity;         // Density of leaves (0.0 to 1.0)

    // Material Parameters (now also fulfilling TreeGenerationConfigure interface)
    private Material logMaterial;
    private Material leafMaterial;

    // Optional: for advanced SCA, you might add these
    private double branchThicknessStart; // Starting thickness of trunk/branches
    private double branchThicknessEnd;   // Ending thickness for outer branches
    private double thicknessDecreaseRate; // How fast thickness decreases along branches

    // Constructor
    public SCATreeGenerationConfigure(double influenceRadius, double killRadius, double branchLength,
                                      int maxIterations, int attractionPointCount,
                                      int trunkHeight, double canopyHeight, double canopyRadius,
                                      double branchRandomness,
                                      double leafPlacementRadius, double leafDensity,
                                      Material logMaterial, Material leafMaterial) {
        this.influenceRadius = influenceRadius;
        this.killRadius = killRadius;
        this.branchLength = branchLength;
        this.maxIterations = maxIterations;
        this.attractionPointCount = attractionPointCount;

        this.trunkHeight = trunkHeight;
        this.canopyHeight = canopyHeight;
        this.canopyRadius = canopyRadius;
        this.branchRandomness = branchRandomness;

        this.leafPlacementRadius = leafPlacementRadius;
        this.leafDensity = leafDensity;

        this.logMaterial = logMaterial;
        this.leafMaterial = leafMaterial;

        // Default thickness values (can be overridden later if needed)
        this.branchThicknessStart = 1.0;
        this.branchThicknessEnd = 1.0;
        this.thicknessDecreaseRate = 0.0;
    }

    // --- Getters for all parameters (including those from TreeGenerationConfigure) ---
    public double getInfluenceRadius() { return influenceRadius; }
    public double getKillRadius() { return killRadius; }
    public double getBranchLength() { return branchLength; }
    public int getMaxIterations() { return maxIterations; }
    public int getAttractionPointCount() { return attractionPointCount; }

    public int getTrunkHeight() { return trunkHeight; }
    public double getCanopyHeight() { return canopyHeight; }
    public double getCanopyRadius() { return canopyRadius; }
    public double getBranchRandomness() { return branchRandomness; }


    public double getLeafPlacementRadius() { return leafPlacementRadius; }
    public double getLeafDensity() { return leafDensity; }

    @Override
    public Material getLogMaterial() { return logMaterial; }

    @Override
    public Material getLeafMaterial() { return leafMaterial; }

    public double getBranchThicknessStart() { return branchThicknessStart; }
    public double getBranchThicknessEnd() { return branchThicknessEnd; }
    public double getThicknessDecreaseRate() { return thicknessDecreaseRate; }

    // --- Setters for optional parameters (if you want to modify after construction) ---
    public void setBranchThicknessStart(double branchThicknessStart) { this.branchThicknessStart = branchThicknessStart; }
    public void setBranchThicknessEnd(double branchThicknessEnd) { this.branchThicknessEnd = branchThicknessEnd; }
    public void setThicknessDecreaseRate(double thicknessDecreaseRate) { this.thicknessDecreaseRate = thicknessDecreaseRate; }

    // --- Static helper for default config (useful for quick testing) ---
    public static SCATreeGenerationConfigure getDefaultOakConfig() {
        return new SCATreeGenerationConfigure(
                10.0, // Influence Radius - Increased to allow branches to 'see' further
                1.0,  // Kill Radius - **Crucially reduced** to be much smaller than branchLength
                2.0,  // Branch Length - Each segment is 2 blocks
                150,  // Max Iterations - Increased to allow more growth
                500,  // Attraction Point Count - Increased to provide more targets
                5,    // Trunk Height
                8.0,  // Canopy Height - Adjusted relative to influence radius
                7.0,  // Canopy Radius - Adjusted relative to influence radius
                0.5,  // Branch Randomness
                3.0,  // Leaf Placement Radius
                0.6,  // Leaf Density
                Material.OAK_LOG,
                Material.OAK_LEAVES
        );
    }

    public static SCATreeGenerationConfigure getDefaultBirchConfig() {
        return new SCATreeGenerationConfigure(
                8.0,  // Influence Radius
                0.8,  // Kill Radius
                1.5,  // Branch Length
                120,  // Max Iterations
                400,  // Attraction Point Count
                7,    // Trunk Height
                6.0,  // Canopy Height
                5.0,  // Canopy Radius
                0.3,  // Branch Randomness
                2.5,  // Leaf Placement Radius
                0.7,  // Leaf Density
                Material.BIRCH_LOG,
                Material.BIRCH_LEAVES
        );
    }
}