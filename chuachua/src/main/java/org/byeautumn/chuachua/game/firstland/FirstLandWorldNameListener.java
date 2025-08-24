package org.byeautumn.chuachua.game.firstland;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.byeautumn.chuachua.Chuachua;
import org.byeautumn.chuachua.Universe;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FirstLandWorldNameListener implements Listener {
    private static Map<UUID, FirstLandWorldConfigAccessor> waitingForName = new HashMap<>();

    private final Chuachua plugin;

    public FirstLandWorldNameListener(Chuachua plugin) {
        this.plugin = plugin;
    }

    public static void startNamingProcess(Player player, FirstLandWorldConfigAccessor configAccessor) {
        if (FirstLandJoinMenu.checkIfPlayerReachedMaxWorlds(player, configAccessor)) {
            player.sendMessage(ChatColor.RED + "You have reached the max amount of worlds created");
            return;
        }
        waitingForName.put(player.getUniqueId(), configAccessor);
        player.sendMessage(ChatColor.AQUA + "Please type the name for your new world in chat.");
        player.sendMessage(ChatColor.GRAY + "Type 'cancel' to stop.");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (waitingForName.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String message = event.getMessage();
            FirstLandWorldConfigAccessor configAccessor = waitingForName.remove(player.getUniqueId());

            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(ChatColor.RED + "World creation cancelled.");
                return;
            }

            if (!message.matches("[a-zA-Z0-9_]{3,16}")) {
                player.sendMessage(ChatColor.RED + "Invalid world name. Must be 3-16 alphanumeric characters.");
                startNamingProcess(player, configAccessor);
                return;
            }

            Universe.createOrConnectExistingWorldWithPlayer(player, plugin, configAccessor, message);
        }
    }
}