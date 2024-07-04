package org.aerial_dad.bedwars_plugin.bedwars.game;

import org.bukkit.entity.Player;

public class BwPlayer {
    private Player player;

    private String bwName;

    private String bwDisplayName;

    public BwPlayer(String bwName, Player player) {
        this.bwName = bwName;
        this.player = player;
    }

    public String getBwName() {
        return bwName;
    }

    public Player getPlayer() {
        return player;
    }
}
