package org.byeautumn.chuachua.common;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.byeautumn.chuachua.Universe;

import java.util.ArrayList;
import java.util.List;

public class LocationUtil {

    public static Location getLocationFromVector(World world, LocationVector vector) {
        return new Location(world, vector.getX(), vector.getY(), vector.getZ());
    }

    public static List<Block> getBlocksBetweenLocations(Location loc1, Location loc2) {
        List<Block> blocks = new ArrayList<>();

        World world = loc1.getWorld();
        if (world == null || !world.equals(loc2.getWorld())) {
            // Locations are in different worlds, cannot process.  Handle the error as you see fit.
            return blocks; // Or throw an exception, log an error, etc.
        }

        Vector start = loc1.toVector();
        System.out.println("Start Vector : " + printVector(start));
        Vector end = loc2.toVector();
        System.out.println("End Vector : " + printVector(end));

        double distance = end.distance(start); // Total distance
        System.out.println("The distance is: " + distance);
        int numSteps = (int) Math.ceil(distance); // Number of steps needed
        System.out.println("The numSteps is: " + numSteps);

        Vector direction = end.clone().subtract(start.clone()).normalize(); // Direction vector
        for (int i = 0; i <= numSteps; i++) {
            double t = (double) i / numSteps; // Interpolation parameter (0.0 to 1.0)
            Vector currentPos = start.clone().add(direction.clone().multiply(t * distance));

            // Get the block at the current position.  Important to use getBlockX/Y/Z for integers!
            int x = currentPos.getBlockX();
            int y = currentPos.getBlockY();
            int z = currentPos.getBlockZ();
            Block block = world.getBlockAt(x, y, z);

            blocks.add(block);
        }
        System.out.println(LocationUtil.printBlocks(blocks));
        return blocks;
    }

    public static String printBlocks(List<Block> blocks) {
        final StringBuffer sb = new StringBuffer();
        for (Block block : blocks) {
            sb.append(printBlock(block)).append("\n");
        }

        return sb.toString();
    }

    public static String printBlock(Block block) {
        return printLocation(block.getLocation()) + " -- " + block.getType().name();
    }

    public static String printLocation(Location loc) {
        final StringBuffer sb = new StringBuffer();
        sb.append(null == loc.getWorld() ? null : loc.getWorld().getName()).append(" -- ");
        sb.append(printVector(loc.toVector()));
        return sb.toString();
    }

    public static String printVector(Vector vector) {
        StringBuffer sb = new StringBuffer();
        sb.append(vector.getX()).append(" , ");
        sb.append(vector.getY()).append(" , ");
        sb.append(vector.getZ());
        return sb.toString();
    }

    public static void main(String[] args) {
        Location loc1 = new Location(Universe.getLobby(), 0.0, 0.0, 0.0);
        Location loc2 = new Location(Universe.getLobby(), 0.0, 0.0, 10.0);

        List<Block> blocks = getBlocksBetweenLocations(loc1, loc2);
        System.out.println(printBlocks(blocks));
    }
}
