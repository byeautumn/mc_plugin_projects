package org.aerial_dad.alexplugin.Sumo.commands;

import org.bukkit.Location;
import org.bukkit.event.Listener;


public class Sumo_waiting implements Listener {
    private static Location CETNER_LOCATION = new Location(null, -0.5, -7.0, 0.5);

    private static Location Player1_LOCATION = new Location(null, -4.5, -7.0, 0.5);

    private static Location Player2_LOCATION = new Location(null, 3.5, -7.0, 0.5);

    private int playerCount = 0;

    public boolean full = false ;

    private String worldName = "Sumo_game1"; // Replace with your world name

//    @EventHandler
//    public void onPlayerJoin(PlayerJoinEvent event) {
//        Player player = event.getPlayer();
//        World world = player.getWorld();
//
//        if (world.getName().equalsIgnoreCase(worldName)) {
//            // Player joined your specific world
//            if (playerCount == 0) {
//                player.sendMessage("Player1 Has joined!");
//                player.setDisplayName("Player1");
//                ++playerCount;
//            } else if (playerCount == 1){
//                player.sendMessage("Player2 Has joined!");
//                player.setDisplayName("Player2");
//                ++playerCount;
//            } else {
//                event.setJoinMessage(null); // Hide join message
//                player.performCommand("l");
//                player.sendMessage("The world is full!"); // Kick player
//
//            }
//            // Player joined a different world
//        }else{
//    }








//        }


//        player.sendMessage("Welcome to the world, " + player.getDisplayName() + "!");

//            World sumoWorld1 = Bukkit.getWorld("sumo_game1");
//          World sumoWorld2 = Bukkit.getWorld("sumo_game2");
//          World sumoWorld3 = Bukkit.getWorld("sumo_game3");




//            if (sumoWorld1 != null && sumoWorld1.getPlayers().size() < 2) {
//                player.setInvulnerable(true);
//
//                if (sumoWorld1 != null && sumoWorld1.getPlayers().size() == 2) {
//                    for (int i = 3; i >= 1; i--) {
//                        try {
//                            Thread.sleep(1000); // Wait for 1 second between each number
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                        player.setInvulnerable(false);
//                    }
//
//                }
//            }
//
//
        }
    /**
     /* How can I have someone joins the world they get assigned a spawning point when the game starts
     /* then when the world is full no more people can join that world
     /* then how can I have when the game start there is a countdown and once at 0 it shows Fight! Then they get tped to the starting location they got assigned too
     /* When you start the game have invulnerability set to true
     /* how can I have when some 1 gets below a curtain y level they lose and the other player wins \
     /* loser says game over
     /* winner says victory!
     */


