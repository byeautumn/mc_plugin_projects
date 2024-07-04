package org.aerial_dad.bedwars_plugin.bedwars.game.Teams;

import org.aerial_dad.bedwars_plugin.bedwars.commands.Bw_general;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JoinQueue implements CommandExecutor {

    public static void  doAddPlayerQueue(Player player ) {
         Bw_general.PLAYER_WAITING_QUEUE.add(player);
    }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        Player player = (Player) sender;
        if (sender instanceof Player && cmd.getName().equalsIgnoreCase("joinbedwarsqueue")){
            doAddPlayerQueue(player);
        }

        return true;
    }
}
