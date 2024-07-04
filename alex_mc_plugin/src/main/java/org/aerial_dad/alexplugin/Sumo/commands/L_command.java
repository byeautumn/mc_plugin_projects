package org.aerial_dad.alexplugin.Sumo.commands;

import org.aerial_dad.alexplugin.AlexFirstPlugin;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class L_command implements CommandExecutor {

    private final AlexFirstPlugin plugin;

    public L_command(AlexFirstPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (sender instanceof Player &&
                (command.getName().equalsIgnoreCase("L" ) || command.getName().equalsIgnoreCase("Leave" ))){
            Player player = (Player) sender;
            Location location = player.getLocation();

            Location spawnLocation = plugin.getConfig().getLocation("Leave location");
            if (spawnLocation != null) {
                player.setGameMode(GameMode.ADVENTURE);
                player.setInvulnerable(true);
                player.teleport(spawnLocation);
                player.sendMessage("Teleported to lobby location!");
            } else {
                player.sendMessage("Spawn location not set yet.");
            }
        }
        return false;
    }
}
