package org.aerial_dad.alexplugin.Sumo.commands;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Arena {

    private final World world;
    private final Location lobbySpawn;
    private final Location[] spawnPoints; // Array for multiple spawn points

    public Arena(World world, Location lobbySpawn, Location... spawnPoints) {
        this.world = world;
        this.lobbySpawn = lobbySpawn;
        this.spawnPoints = spawnPoints;
    }

    public void teleportToLobby(Player player) {
        // Use player.teleport(Location location) instead
        player.teleport(lobbySpawn);
    }

    // Methods for arena manipulation (forcefields, boundaries) and potentially spawn selection
    // ...
}