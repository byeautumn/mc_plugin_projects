package org.byeautumn.chuachua.accessor;

import java.io.File;

public interface Accessor {

    /**
     * Creates the base directory and its subdirectories if they do not exist.
     * @param baseDir The base directory for this accessor's data.
     */
    void createDirectories(File baseDir);
}