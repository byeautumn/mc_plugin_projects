package org.aerial_dad.alexplugin.Bedwars.Mapsetup.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Bw_general implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player && cmd.getName().equalsIgnoreCase("bw") || cmd.getName().equalsIgnoreCase("bedwars")){
            Player player = (Player) sender;



        }
        return false;
    }
}
