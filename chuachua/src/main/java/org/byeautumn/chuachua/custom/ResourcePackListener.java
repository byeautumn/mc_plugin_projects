package org.byeautumn.chuachua.custom;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.byeautumn.chuachua.Chuachua;
import org.byeautumn.chuachua.Universe;

public class ResourcePackListener implements Listener {

    private final String resourcePackURL;
    private static final String RESOURCE_PACK_SENT_METADATA = "resourcePackSent";

    public ResourcePackListener(String resourcePackURL) {
        this.resourcePackURL = resourcePackURL;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasMetadata(RESOURCE_PACK_SENT_METADATA)) {
            System.out.println("Resource pack metadata not found for " + player.getDisplayName() + ". Sending request.");
            System.out.println("Chua resource pack URL: " + resourcePackURL);
            // Send the resource pack request
            player.setResourcePack(resourcePackURL);
            
            // Optional: Send a message to the player explaining the resource pack
            player.sendMessage("§aWelcome to the server!");
            player.sendMessage("§eThis server uses a custom resource pack for unique content.");
            player.sendMessage("§eYou will be prompted to download it. Please accept for the best experience.");
        } else {
            // Optional: You could send a different welcome message
            player.sendMessage("§aWelcome back to the server!");
        }
    }
}