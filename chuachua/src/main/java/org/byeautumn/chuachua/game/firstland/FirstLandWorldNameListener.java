package org.byeautumn.chuachua.game.firstland;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.byeautumn.chuachua.Chuachua;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.player.PlayerDataAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FirstLandWorldNameListener implements Listener {
    // 1. Change the map value to use the new accessor
    private static Map<UUID, WorldDataAccessor> waitingForName = new HashMap<>();

    private final Chuachua plugin;
    private final PlayerDataAccessor playerDataAccessor;


    public FirstLandWorldNameListener( PlayerDataAccessor playerDataAccessor ,Chuachua plugin){
        this.plugin = plugin;
        this.playerDataAccessor = playerDataAccessor;
    }

    /**
     * Initiates the process of creating a new world by prompting the player for a name.
     * This method ensures the player has not reached their world creation limit.
     *
     * @param player The player initiating the process.
     * @param worldDataAccessor The accessor for world data.
     */
    public static void startNamingProcess(Player player, WorldDataAccessor worldDataAccessor, JavaPlugin plugin) {
        // 2. The check now uses only the WorldDataAccessor
        if(FirstLandJoinMenu.checkIfPlayerReachedMaxWorlds(player, worldDataAccessor, plugin)){
            player.sendMessage(ChatColor.RED + "You have reached the max amount of worlds created");
            return;
        }
        waitingForName.put(player.getUniqueId(), worldDataAccessor);
        player.sendMessage(ChatColor.AQUA + "Please type the name for your new world in chat.");
        player.sendMessage(ChatColor.GRAY + "Type 'cancel' to stop.");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (waitingForName.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage();
            // Retrieve the correct accessor from the map
            WorldDataAccessor worldDataAccessor = waitingForName.remove(player.getUniqueId());

            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "World creation cancelled.");
                return;
            }

            if (!message.matches("[a-zA-Z0-9_]{3,16}")) {
                player.sendMessage(ChatColor.RED + "Invalid world name. Must be 3-16 alphanumeric characters.");
                // Recursively call the corrected method with the new accessor
                startNamingProcess(player, worldDataAccessor, plugin);
                return;
            }

            // Corrected: Pass all necessary data to the Universe method
            UUID ownerUUID = player.getUniqueId();
            // We'll use the same name for both internal and friendly name for simplicity
            String friendlyName = message;

            Universe.createOrConnectExistingWorldWithPlayer(player, plugin, worldDataAccessor, playerDataAccessor ,friendlyName, ownerUUID);
        }
    }
}