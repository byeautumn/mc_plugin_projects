package org.byeautumn.chuachua.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.byeautumn.chuachua.Chuachua;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.player.PlayerTracker;

import java.util.Arrays;
import java.util.List;

public class OperationCommand  implements CommandExecutor {

    private static final List<String> BW_VALID_ARGS = Arrays.asList("exit", "create", "list", "edit", "save", "pos1", "pos2", "mapbottom", "setqueuespawn", "setlobbyworld","delete", "tp", "listworlds");
    private final Chuachua plugin;

    public OperationCommand(Chuachua plugin)  {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player && cmd.getName().equalsIgnoreCase("cc")) {
            Player player = (Player) sender;
            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "could not find the argument. Try these: ");
                player.sendMessage(ChatColor.YELLOW + " " + BW_VALID_ARGS);
                return true;
            }
            String firstArg = args[0];
            if (BW_VALID_ARGS.contains(firstArg)) {
                if (firstArg.equalsIgnoreCase("exit")) {
                    Universe.teleportToLobby(player);

                    player.setGameMode(GameMode.ADVENTURE);
                    player.setAllowFlight(false);
                    player.setFlying(false);
                    player.getInventory().clear();
                    player.setHealth(20.0);
                    Universe.resetPlayerTracker(player);
                }
                else if (firstArg.equalsIgnoreCase("listworlds")) {
                    player.sendMessage("===================================================");
                    for (World world : Bukkit.getWorlds()) {
                        player.sendMessage(world.getName() + " -- " + world.getWorldType().getName());
                    }
                    player.sendMessage("===================================================");
                }

            } else {
                player.sendMessage(ChatColor.RED + "Could not find the argument you wrote " + ChatColor.YELLOW + firstArg);
            }
        }


        return true;
    }
}
