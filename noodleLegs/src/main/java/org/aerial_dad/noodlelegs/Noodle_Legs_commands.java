package org.aerial_dad.noodlelegs;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Noodle_Legs_commands implements CommandExecutor {

    // /nl tp world, x, y ,z
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String s, String[] args) {
        if (sender instanceof Player && cmd.getName().equalsIgnoreCase("nl") ||
                cmd.getName().equalsIgnoreCase("noodlelegs")){
            Player player = (((Player) sender).getPlayer());
            if (args.length == 0 ){
                player.sendMessage(ChatColor.YELLOW + "You ran " + ChatColor.AQUA + "" + cmd.getName() + ChatColor.YELLOW + "" + " pls provide a argument.");
            }else{
                String functionArg = args[0];
                if (functionArg.equalsIgnoreCase("tp") || functionArg.equalsIgnoreCase("teleport")){
                    String worldName = args[1];
                    String[] coordinates = args[2].split(",");
                    int x = Integer.parseInt(coordinates[0]);
                    int y = Integer.parseInt(coordinates[1]);
                    int z = Integer.parseInt(coordinates[2]);

                    // Create a Location object
                    World toWorld = Bukkit.getWorld(worldName);
                    if (toWorld == null){
                        System.out.println("World '" + worldName + "' doesn't exits. ");
                        System.out.println("Existing worlds are '" + Bukkit.getWorlds() + "'.");
                        return false;

                    }
                    Location tpLocation = new Location(toWorld, x, y, z);
                    doTeleport(player, tpLocation);

                }else if(functionArg.equalsIgnoreCase("createWorld")){
                    doCreate(args, player);

                }else{
                    player.sendMessage(ChatColor.RED + "You have ran a invalid argument: " + functionArg);
                }



            }
        }else{
            sender.sendMessage("Only players can use this command!");
        }
        return true;
    }

    private void doTeleport(Player player, Location toLocation){
        Universe.teleport(player, toLocation);

    }

    private void doCreate( String[] args, Player player) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Please provide a name for your map. ");
        } else {
            String worldName = args[1];
            player.getServer().createWorld(new WorldCreator(worldName));
            player.sendMessage("Loading... ");
            player.sendMessage(ChatColor.GREEN + "The map and world " + worldName + " has been created.");
            Location newWorldLocation  = new Location(Bukkit.getWorld(worldName), 0, 60, 0 );
            player.teleport(newWorldLocation);
//                player.sendMessage("Please select a range for your map " + ChatColor.YELLOW + "/bw pos1" + ChatColor.GREEN + " and " + ChatColor.YELLOW + "/bw pos2");


        }
    }
}