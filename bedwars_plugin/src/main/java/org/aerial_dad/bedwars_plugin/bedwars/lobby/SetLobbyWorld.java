package org.aerial_dad.bedwars_plugin.bedwars.lobby;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetLobbyWorld {


    public static void doSetLobby(CommandSender sender, String[] args){
            Player player = (Player) sender;
            World world = player.getWorld();
            Location location = world.getSpawnLocation();
            player.sendMessage("The lobby has been set to " + world + "at" + location);






    }
}
