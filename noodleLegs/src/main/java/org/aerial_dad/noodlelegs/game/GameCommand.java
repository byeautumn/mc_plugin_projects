package org.aerial_dad.noodlelegs.game;

import org.aerial_dad.noodlelegs.Block_listener;
import org.aerial_dad.noodlelegs.Universe;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GameCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] strings) {
        if (sender instanceof Player && cmd.getName().equalsIgnoreCase("exit")){
            Player player = (Player) sender;
            World fromWorld = player.getWorld();


            player.performCommand("mv tp world");

            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(false);
            player.setFlying(false);
            player.getInventory().clear();
            player.setHealth(20.0);

            if (fromWorld.getName().startsWith(GameLauncher.GAME_WORLD_KEYWORD) && fromWorld.getPlayers().isEmpty()) {
                System.out.println("The world '" + fromWorld.getName() + "' has no player now. It will be unloaded soon.");
                Bukkit.unloadWorld(fromWorld.getName(), false);
                player.performCommand("nl rmworld " + fromWorld.getName());
            }

        }
        return true;
    }
}
