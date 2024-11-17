package org.aerial_dad.noodlelegs.game;

import org.aerial_dad.noodlelegs.Block_listener;
import org.aerial_dad.noodlelegs.Universe;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GameCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] strings) {
        if (sender instanceof Player && cmd.getName().equalsIgnoreCase("exit")){
            Player player = (Player) sender;
            player.performCommand("mv tp world");
//            PlayerTracker playerTracker = Universe.getPlayerTracker(player);
//            playerTracker.reset();
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(false);
            player.setFlying(false);
        }
        return true;
    }
}
