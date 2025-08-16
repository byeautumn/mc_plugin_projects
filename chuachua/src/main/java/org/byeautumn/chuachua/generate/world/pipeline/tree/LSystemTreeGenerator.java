package org.byeautumn.chuachua.generate.world.pipeline.tree;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LSystemTreeGenerator {

    public void generate(Location startLocation, TreeConfigure config) {
        World world = startLocation.getWorld();
        Random random = new Random(world.getSeed() ^ startLocation.hashCode()); // Seeded random for consistency

        String current = config.getAxiom();
        for (int i = 0; i < config.getIterations(); i++) {
            StringBuilder next = new StringBuilder();
            for (char c : current.toCharArray()) {
                if (config.getRules().containsKey(c)) {
                    next.append(config.getRules().get(c));
                } else {
                    next.append(c);
                }
            }
            current = next.toString();
        }

        List<TurtleState> stack = new ArrayList<>();
        TurtleState currentState = new TurtleState(startLocation.toVector(), new Vector(0, 1, 0)); // Initial state: position and direction (up)

        for (char command : current.toCharArray()) {
            switch (command) {
                case 'F': // Move forward and draw a segment
                    Vector start = currentState.position.clone();
                    currentState.position.add(currentState.direction.clone().normalize().multiply(config.getSegmentLength() / config.getIterations())); // Adjust segment length based on iterations
                    drawLine(world, start.toLocation(world), currentState.position.toLocation(world), config.getLogMaterial());
                    break;
                case '+': // Turn left by angle
                    currentState.direction.rotateAroundAxis(new Vector(0, 1, 0), Math.toRadians(config.getAngle() + (random.nextDouble() - 0.5) * config.getAngleVariation()));
                    break;
                case '-': // Turn right by angle
                    currentState.direction.rotateAroundAxis(new Vector(0, 1, 0), Math.toRadians(-(config.getAngle() + (random.nextDouble() - 0.5) * config.getAngleVariation())));
                    break;
                case '[': // Push current state onto the stack
                    stack.add(currentState.clone());
                    break;
                case ']': // Pop the last state from the stack
                    if (!stack.isEmpty()) {
                        currentState = stack.remove(stack.size() - 1);
                    }
                    break;
                // You can add more symbols for branching up/down, rolling, etc.
            }
        }

        // Basic leaf generation (needs improvement)
        int leafRadius = 2;
        Location trunkTop = currentState.position.toLocation(world);
        for (int x = -leafRadius; x <= leafRadius; x++) {
            for (int y = -leafRadius; y <= leafRadius; y++) {
                for (int z = -leafRadius; z <= leafRadius; z++) {
                    if (x * x + y * y + z * z <= leafRadius * leafRadius &&
                            world.getBlockAt(trunkTop.getBlockX() + x, trunkTop.getBlockY() + y, trunkTop.getBlockZ() + z).getType() == Material.AIR) {
                        world.getBlockAt(trunkTop.getBlockX() + x, trunkTop.getBlockY() + y, trunkTop.getBlockZ() + z).setType(config.getLeafMaterial());
                    }
                }
            }
        }
    }

    private void drawLine(World world, Location start, Location end, Material material) {
        List<Location> line = createLine(start, end);
        for (Location point : line) {
            if (world.getBlockAt(point).getType() == Material.AIR) {
                world.getBlockAt(point).setType(material);
            }
        }
    }

    private List<Location> createLine(Location start, Location end) {
        List<Location> line = new ArrayList<>();
        double distance = start.distance(end);
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        for (double i = 0; i <= distance; i += 0.5) {
            line.add(start.clone().add(direction.clone().multiply(i)));
        }
        return line;
    }

    private static class TurtleState implements Cloneable {
        Vector position;
        Vector direction;

        public TurtleState(Vector position, Vector direction) {
            this.position = position;
            this.direction = direction.normalize();
        }

        @Override
        public TurtleState clone() {
            try {
                TurtleState clone = (TurtleState) super.clone();
                clone.position = this.position.clone();
                clone.direction = this.direction.clone();
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }
}
