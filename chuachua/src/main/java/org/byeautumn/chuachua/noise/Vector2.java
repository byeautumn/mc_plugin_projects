package org.byeautumn.chuachua.noise;

public class Vector2 {
    public double x;
    public double y;

    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2 normalize() {
        double length = (double) Math.sqrt(x * x + y * y);
        if (length != 0) {
            return new Vector2(x / length, y / length);
        }
        return new Vector2(0, 0);
    }
}
