package org.byeautumn.chuachua.game.firstland;

import java.util.UUID;

public class WorldCreationRequest {
    final long seed;
    final String worldName;
    final UUID worldUUID;
    final String worldFriendlyName; // Add this line

    WorldCreationRequest(long seed, String worldName, UUID worldUUID, String worldFriendlyName) {
        this.seed = seed;
        this.worldName = worldName;
        this.worldUUID = worldUUID;
        this.worldFriendlyName = worldFriendlyName; // Add this line
    }
}
