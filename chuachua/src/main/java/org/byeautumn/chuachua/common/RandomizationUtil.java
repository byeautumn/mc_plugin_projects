package org.byeautumn.chuachua.common;

import java.util.Random;

public class RandomizationUtil {
    private static final Random random = new Random();
    public static int getRandomIndex(int containerSize) {
        return random.nextInt(containerSize);
    }
}
