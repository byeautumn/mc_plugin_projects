package org.byeautumn.chuachua.generate;

import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;
import org.byeautumn.chuachua.common.LocationVector;

import java.util.List;

public class SimpleRoom {
    private SimpleFloor floor;
    private List<SimpleWall> walls;

    public SimpleRoom(List<LocationVector> polygon){
        for (LocationVector vector : polygon) {
//            vector.
            //y = mx+b
            //y=m(x-x1)+y1
        }

        for(int idx = 0; idx < polygon.size(); idx++) {

            LocationVector pos1 = polygon.get(idx);
            int nextIdx = (idx == polygon.size() - 1) ? 0 : idx + 1;
            LocationVector pos2 = polygon.get(nextIdx);
            double x1 = pos1.getX();
            double x2 = pos2.getX();
            double z1 = pos1.getZ();
            double z2 = pos2.getZ();
            double slope = ((z2 - z1) / (x2 - x1));
            double yIntercept = z1 - (slope * x1);
            double distance = calculateDistance(x1, z1, x2, z2);
            double a = solveForA(slope, distance);
            LocationVector blockVector = new LocationVector(a, pos1.getZ(), slope*a);






        }
    }

    public static double solveForA(double m, double distance) {
        double denominator = Math.pow(m, 2) + 1;
        double sqrtTerm = Math.sqrt(distance * distance / denominator);

        double a1 = sqrtTerm;
        double a2 = -sqrtTerm;
        return a1;
    }

    public static double calculateDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }


}
