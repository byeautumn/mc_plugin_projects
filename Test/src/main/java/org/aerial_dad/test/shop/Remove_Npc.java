package org.aerial_dad.test.shop;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Remove_Npc implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;
            Location playerLocation = player.getLocation();




        }else {
            sender.sendMessage("Are you a player?");
            sender.sendMessage("no shady stuff OR ELSE");
        }
        return true;
    }
}
