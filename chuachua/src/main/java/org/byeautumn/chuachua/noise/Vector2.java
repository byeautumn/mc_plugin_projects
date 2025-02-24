package org.byeautumn.chuachua.noise;

public class Vector2 {
    public float x;
    public float y;

    public Vector2(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2 normalize() {
        float length = (float) Math.sqrt(x * x + y * y);
        if (length != 0) {
            return new Vector2(x / length, y / length);
        }
        return new Vector2(0, 0);
    }
}
