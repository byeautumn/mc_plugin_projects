package org.aerial_dad.bedwars_plugin.bedwars.listeners;



import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import java.util.HashSet;
import java.util.Set;

public class Op_listener implements Listener {
    public static Set<String> ops = new HashSet<String>();

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event){
        // example: /op Alex Qiang
        // command: op Alex Qiang
        // String[]: [op] [Alex] [Qiang]
        System.out.println("event.getMessage(): " + event.getMessage());
        String command = event.getMessage().substring(1).trim();
        System.out.println("Command: " + command);

        String[] comStrArr = command.split(" ");

        if(comStrArr.length < 2) {
            return;
        }

        if (comStrArr[0].equalsIgnoreCase("op")){
            for(int idx = 1; idx < comStrArr.length; ++idx) {
                ops.add(comStrArr[idx]);
                System.out.println("Player " + comStrArr[idx] + " has been added to ops list. ");

            }
        }
    }
}
