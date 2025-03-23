package org.byeautumn.chuachua.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.byeautumn.chuachua.common.LocationVector;

import java.util.Arrays;
import java.util.List;

public class Game {
    private List<Chapter> chapters;

    public Game() {
        loadChapters();
    }

    // Temporary function; will need to move it to a config file
    private void loadChapters() {
        this.chapters = Arrays.asList(
                new Chapter(
                        new ChapterConfig(0, "chapter 1", "chapter1_world",
                                new LocationVector(8.0, 230, 23),
                                Arrays.asList(new LocationVector(-304.0, 86.0, 16.0))))
                ,
                new Chapter(
                        new ChapterConfig(0, "chapter 2", "chapter2_world",
                                new LocationVector(0.0, 0.0, 0.0),
                                Arrays.asList(new LocationVector(0.0, 0.0, 0.0))))
//                                ,
//                new Chapter(
//                        new ChapterConfig(0, "chapter 3", "chapter3_world",
//                                new LocationVector(0.0, 0.0, 0.0),
//                                Arrays.asList(new LocationVector(0.0, 0.0, 0.0)))),
//                new Chapter(
//                        new ChapterConfig(0, "chapter 4", "chapter4_world",
//                                new LocationVector(0.0, 0.0, 0.0),
//                                Arrays.asList(new LocationVector(0.0, 0.0, 0.0))))
        );
    }

    public Chapter getChapter(int index) {
        if (null == this.chapters) {
            throw new RuntimeException("The chapters are not loaded yet.");
        }

        if (index >= this.chapters.size()) {
            System.err.println("The input index " + index + " is out of the range of [0," + (this.chapters.size() - 1) + "].");
            return null;
        }

        return chapters.get(index);
    }

}
