package org.byeautumn.chuachua.generate.world.pipeline.tree;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class SCATreeGenerator implements TreeGenerator {

    // Internal data structures for SCA nodes and points
    private static class Node {
        Vector position;
        Node parent; // For tracing back the branch structure
        int branchesCount; // Number of attraction points it's attracting
        Vector growthDirection; // Accumulated direction for next growth
        boolean isDead; // For pruning/growth termination

        Node(Vector position, Node parent) {
            this.position = position;
            this.parent = parent;
            this.branchesCount = 0;
            this.growthDirection = new Vector(0, 0, 0);
            this.isDead = false;
        }

        // Resets the node for the next growth iteration
        void reset() {
            branchesCount = 0;
            growthDirection.zero();
        }
    }

    private static class AttractionPoint {
        Vector position;
        boolean isConsumed; // Marks if it has been 'reached' by a branch

        AttractionPoint(Vector position) {
            this.position = position;
            this.isConsumed = false;
        }
    }

    private final Random random = new Random();

    @Override
    public boolean generate(Location location, TreeGenerationConfigure configure) {
        if (!(configure instanceof SCATreeGenerationConfigure)) {
            System.err.println("SCATreeGenerator requires an instance of SCATreeGenerationConfigure!");
            return false;
        }
        SCATreeGenerationConfigure scaConfigure = (SCATreeGenerationConfigure) configure;

        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        if (!isGroundBlock(location.getBlock().getType())) {
            System.err.println("The generating location is not a suitable ground block. The generation aborted.");
            return false;
        }

        List<Node> nodes = new ArrayList<>();
        Set<Location> placedLogs = new HashSet<>();
        Set<Location> placedLeaves = new HashSet<>();

        // --- 1. Generate the Trunk ---
        System.out.println("Generating Trunk ...");
        Node trunkStartNode = new Node(location.toVector(), null); // The very bottom of the trunk (ground level)
        Node lastTrunkNode = trunkStartNode; // This will track the highest node in the trunk
        Location currentTrunkLoc = location.clone(); // Start at the base location

        for (int i = 0; i < scaConfigure.getTrunkHeight(); i++) {
            currentTrunkLoc.add(0, 1, 0); // Move up one block for each trunk segment
            Block block = world.getBlockAt(currentTrunkLoc);

            if (!block.getType().isAir()) {
                System.err.println("Not enough space for trunk at " + currentTrunkLoc + ". Tree generation aborted. Block type: " + block.getType());
                for (Location logLoc : placedLogs) {
                    if (logLoc.getY() > location.getY() && logLoc.getY() <= currentTrunkLoc.getY()) {
                        world.getBlockAt(logLoc).setType(Material.AIR); // Cleanup
                    }
                }
                return false;
            }

            block.setType(scaConfigure.getLogMaterial());
            if (block.getBlockData() instanceof Directional) {
                Directional directional = (Directional) block.getBlockData();
                directional.setFacing(BlockFace.UP);
                block.setBlockData(directional);
            }
            placedLogs.add(block.getLocation());

            Node newNode = new Node(currentTrunkLoc.toVector(), lastTrunkNode);
            nodes.add(newNode); // Add trunk nodes to the list that SCA will work on
            lastTrunkNode = newNode; // Update lastTrunkNode
        }

        // If no trunk was generated (trunkHeight = 0), SCA should start from the initial location.
        // Otherwise, it starts from the top of the generated trunk.
        if (scaConfigure.getTrunkHeight() == 0) {
            nodes.add(trunkStartNode); // Add the initial ground node for SCA to start from
        }

        // Ensure there's at least one node to start SCA growth from
        if (nodes.isEmpty()) {
            System.err.println("No initial nodes for SCA growth. Check trunk height configuration.");
            return false;
        }


        // --- 2. Generate Attraction Points (Canopy Shape) ---
        System.out.println("Generate Attraction Points (Canopy Shape) ...");
        List<AttractionPoint> attractionPoints = new ArrayList<>();
        // Attraction points should be relative to the *highest* point of the initial structure (trunk top or base location)
        Vector canopyBasePosition = lastTrunkNode.position;

        double canopyHeight = scaConfigure.getCanopyHeight();
        double canopyRadius = scaConfigure.getCanopyRadius();

        // Debug: Check initial canopy position
        System.out.println("Canopy base position: " + canopyBasePosition);
        System.out.println("Canopy dimensions: Height=" + canopyHeight + ", Radius=" + canopyRadius);


        for (int i = 0; i < scaConfigure.getAttractionPointCount(); i++) {
            // Generate points within a spherical volume relative to the canopyBasePosition
            double x = (random.nextDouble() * 2 - 1) * canopyRadius;
            double z = (random.nextDouble() * 2 - 1) * canopyRadius;
            // Y needs to be from 0 to canopyHeight *above* canopyBasePosition
            double y = random.nextDouble() * canopyHeight;

            Vector pointPosition = canopyBasePosition.clone().add(new Vector(x, y, z));
            attractionPoints.add(new AttractionPoint(pointPosition));
        }

        // Debug: Check attraction point count
        System.out.println("Generated " + attractionPoints.size() + " attraction points.");
        if (attractionPoints.isEmpty()) {
            System.err.println("No attraction points generated. SCA will not grow.");
            return false; // Exit early if no points
        }


        // --- 3. Iterative Growth (Core SCA Loop) ---
        System.out.println("Iterative Growth (Core SCA Loop) ...");
        for (int iteration = 0; iteration < scaConfigure.getMaxIterations(); iteration++) {
            // Debug: Track iteration progress
            // System.out.println("Iteration " + (iteration + 1) + "/" + scaConfigure.getMaxIterations() +
            //                    ", Active Nodes: " + nodes.size() +
            //                    ", Remaining Attraction Points: " + attractionPoints.stream().filter(p -> !p.isConsumed).count());

            // If no more attraction points, growth stops early
            if (attractionPoints.isEmpty() || attractionPoints.stream().allMatch(p -> p.isConsumed)) {
                System.out.println("All attraction points consumed or none left at iteration " + (iteration + 1));
                break;
            }

            nodes.forEach(Node::reset);

            List<AttractionPoint> activePoints = attractionPoints.stream()
                    .filter(p -> !p.isConsumed)
                    .collect(Collectors.toList());

            if (activePoints.isEmpty()) { // No active points means no more growth
                System.out.println("No active attraction points. Growth halted at iteration " + (iteration + 1));
                break;
            }

            // Phase 1: Associate attraction points with closest nodes
            for (AttractionPoint point : activePoints) {
                Node closestNode = null;
                double minDistanceSq = Double.MAX_VALUE;

                for (Node node : nodes) {
                    // Only consider active, non-dead nodes for attraction
                    if (node.isDead) continue;
                    double distSq = node.position.distanceSquared(point.position);
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq;
                        closestNode = node;
                    }
                }

                if (closestNode != null) {
                    double dist = Math.sqrt(minDistanceSq);

                    // If point is within 'kill radius', consume it
                    if (dist < scaConfigure.getKillRadius()) {
                        point.isConsumed = true;
                    }
                    // If point is within 'influence radius', it influences growth
                    else if (dist < scaConfigure.getInfluenceRadius()) {
                        Vector direction = point.position.clone().subtract(closestNode.position).normalize();
                        closestNode.growthDirection.add(direction);
                        closestNode.branchesCount++;
                    }
                }
            }

            // Phase 2: Create new nodes for growth
            List<Node> newNodes = new ArrayList<>();
            int nodesGrewThisIteration = 0;
            for (Node node : nodes) {
                if (node.isDead) continue;
                if (node.branchesCount > 0) { // Only grow nodes that attracted points
                    Vector avgDirection = node.growthDirection.clone().normalize();
                    Vector newPosition = node.position.clone().add(avgDirection.multiply(scaConfigure.getBranchLength()));

                    if (scaConfigure.getBranchRandomness() > 0) {
                        newPosition.add(new Vector(
                                (random.nextDouble() * 2 - 1) * scaConfigure.getBranchRandomness(),
                                (random.nextDouble() * 2 - 1) * scaConfigure.getBranchRandomness(),
                                (random.nextDouble() * 2 - 1) * scaConfigure.getBranchRandomness()
                        ));
                    }

                    Node newNode = new Node(newPosition, node);
                    newNodes.add(newNode);
                    nodesGrewThisIteration++;
                } else {
                    // If a node didn't attract any points this iteration, it might stop growing.
                    // Only mark as dead if it's not the initial trunk base node AND it didn't attract points.
                    // This prevents the very first node from dying too soon.
                    if (!node.equals(trunkStartNode) || scaConfigure.getTrunkHeight() == 0) {
                        node.isDead = true;
                    }
                }
            }
            nodes.addAll(newNodes);

            // Debug: Check if any nodes grew
            if (nodesGrewThisIteration == 0 && iteration > 0) { // If no nodes grew, something stalled (after initial iteration)
                System.out.println("No nodes grew this iteration. Growth stalled.");
                break;
            }

            attractionPoints.removeIf(p -> p.isConsumed);

            // Pruning: Remove dead nodes that are no longer contributing to growth
            nodes.removeIf(node -> node.isDead && node.branchesCount == 0 && node.parent != null); // Don't remove root/trunk base
        }


        // --- 4. Render Tree (Place Blocks) ---
        System.out.println("Rendering Tree (Placing Logs) ...");
        // The trunk logs were already placed. Now, draw the branches grown by SCA.
        for (Node node : nodes) {
            // Only draw branches for nodes that have a parent (i.e., are part of the branching structure)
            // and are not the very initial trunk base node (which is handled by trunk generation).
            if (node.parent != null && !node.equals(trunkStartNode)) {
                drawBranch(world, node.parent.position, node.position, scaConfigure.getLogMaterial(), placedLogs);
            }
        }

        System.out.println("Rendering Leaves ...");
        // Place leaves around active nodes (terminal nodes and those that still contribute)
        Set<Node> terminalNodes = nodes.stream()
                .filter(node -> !node.isDead || node.branchesCount > 0)
                .collect(Collectors.toSet());

        for (Node node : terminalNodes) {
            placeLeavesAroundNode(world, node.position, scaConfigure.getLeafPlacementRadius(), scaConfigure.getLeafMaterial(), scaConfigure.getLeafDensity(), placedLogs, placedLeaves);
        }

        attractionPoints.clear();

        System.out.println("Tree generation complete.");
        return true;
    }

    private void drawBranch(World world, Vector start, Vector end, Material material, Set<Location> placedBlocks) {
        Vector direction = end.clone().subtract(start);
        double distance = direction.length();
        direction.normalize();

        BlockFace facing = getBlockFaceFromVector(direction);

        for (double d = 0; d <= distance; d += 0.5) {
            Vector currentPos = start.clone().add(direction.clone().multiply(d));
            Location blockLoc = currentPos.toLocation(world);
            blockLoc = blockLoc.getBlock().getLocation();

            if (!placedBlocks.contains(blockLoc) && world.getBlockAt(blockLoc).getType().isAir()) {
                Block block = world.getBlockAt(blockLoc);
                block.setType(material);

                if (block.getBlockData() instanceof Directional) {
                    Directional directional = (Directional) block.getBlockData();
                    directional.setFacing(facing);
                    block.setBlockData(directional);
                }
                placedBlocks.add(block.getLocation());
            }
        }
    }

    private BlockFace getBlockFaceFromVector(Vector direction) {
        double absX = Math.abs(direction.getX());
        double absY = Math.abs(direction.getY());
        double absZ = Math.abs(direction.getZ());

        if (absX > absY && absX > absZ) {
            return direction.getX() > 0 ? BlockFace.EAST : BlockFace.WEST;
        } else if (absY > absX && absY > absZ) {
            return direction.getY() > 0 ? BlockFace.UP : BlockFace.DOWN;
        } else if (absZ > absX && absZ > absY) {
            return direction.getZ() > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }
        return BlockFace.UP;
    }


    private void placeLeavesAroundNode(World world, Vector nodePosition, double radius, Material leafMaterial, double leafDensity, Set<Location> placedLogs, Set<Location> placedLeaves) {
        int r = (int) Math.ceil(radius);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    Vector offset = new Vector(x, y, z);
                    if (offset.lengthSquared() <= radius * radius) {
                        if (random.nextDouble() > leafDensity) {
                            continue;
                        }

                        Location leafLoc = nodePosition.clone().add(offset).toLocation(world);
                        Block block = world.getBlockAt(leafLoc);

                        if (block.getType().isAir() && !placedLogs.contains(leafLoc) && !placedLeaves.contains(leafLoc)) {
                            block.setType(leafMaterial);
                            placedLeaves.add(leafLoc);
                        }
                    }
                }
            }
        }
    }

    private boolean isGroundBlock(Material type) {
        return type == Material.DIRT || type == Material.GRASS_BLOCK || type == Material.COARSE_DIRT || type == Material.PODZOL;
    }
}