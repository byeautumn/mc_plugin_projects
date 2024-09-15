package org.aerial_dad.noodlelegs;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NpcInteract implements Listener {


    @EventHandler
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        if (entity.hasMetadata("lobbynpc")) {
            player.sendMessage("you clicked this NPC!!!!!");
        }


    }
}
