package org.byeautumn.chuachua.game.firstland;

import java.util.UUID;

public class WorldCreationRequest {
    final long seed;
    final String worldName;
    final UUID worldUUID;

    WorldCreationRequest(long seed, String worldName, UUID worldUUID) {
        this.seed = seed;
        this.worldName = worldName;
        this.worldUUID = worldUUID;
    }
}
