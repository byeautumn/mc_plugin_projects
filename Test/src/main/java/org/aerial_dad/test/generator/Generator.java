package org.aerial_dad.test.generator;

import org.aerial_dad.test.Test;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;



public class Generator implements CommandExecutor {

    public enum TEAMCOLORS{

        RED,

        BLUE,
        ;

        public static boolean containsAny(String[] values) {
            for (TEAMCOLORS enumValue : TEAMCOLORS.values()) {
                for (String value : values) {
                    if (enumValue.name().equals(value)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private final Test plugin;

    public Generator(Test plugin){
        this.plugin = plugin;
    }

    private void setGenerator(String color, Location location){
        double locationX = location.getX();
        double locationY = location.getY();
        double locationZ = location.getZ();
        double XYZ = locationX + locationY + locationZ;
        plugin.getConfig().set("Generator Team name: "+ color, color);
        plugin.getConfig().set("Generator location" + XYZ, XYZ);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (sender instanceof Player ){
            Player player = (Player) sender;

            if (args.length < 1){
                player.sendMessage(ChatColor.RED + "please provide a team for this generator. ");
            }else{
                if (TEAMCOLORS.containsAny(args)){
                    String teamColor = args[0];
                    setGenerator(teamColor, player.getLocation());
                }else{
                    player.sendMessage("Please provide a valid Team. ");
                }
            }

        }
        return true;
    }
}
