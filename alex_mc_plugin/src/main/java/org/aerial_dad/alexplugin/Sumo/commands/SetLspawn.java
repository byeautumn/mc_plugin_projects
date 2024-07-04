package org.aerial_dad.alexplugin.Sumo.commands;

import org.aerial_dad.alexplugin.AlexFirstPlugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetLspawn implements CommandExecutor{
    private final AlexFirstPlugin plugin;

    public SetLspawn(AlexFirstPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player && cmd.getName().equalsIgnoreCase("setlspawn")) {
            Player player = (Player) sender;

            Location location = player.getLocation();
            double x = location.getX();
            double z = location.getZ();
            double y = location.getY();


            player.sendMessage("Leave location set to (x, y, z): (" + x + ", " + z + ", " + y + ")");

            System.out.println("Someone has set the /L location");

            plugin.getConfig().set("Leave location", location);
            plugin.saveConfig();




        }
        return true;
    }
}
