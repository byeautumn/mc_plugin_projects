package org.aerial_dad.alexplugin.Sumo.commands;

//public class Sumo_lobby implements CommandExecutor {
//    @Override
//    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
//        if (sender instanceof Player && command.getName().equalsIgnoreCase("play_duels_sumo")) {
//            Player player = (Player) sender;
////            Block b = null;
////            BlockData bd = b.getBlockData();
////            bd.
//
//            // Get the world to check
//            World sumoWorld1 = Bukkit.getWorld("sumo_game1");
//            World sumoWorld2 = Bukkit.getWorld("sumo_game2");
//            World sumoWorld3 = Bukkit.getWorld("sumo_game2");
//            if (sumoWorld1 != null &&) {
//
//                // Check if the world is loaded and has less than 2 players
//                if (sumoWorld1 != null && sumoWorld1.getPlayers().size() < 2) {
//                    // World is available, check and handle queue
//                    if (removeFromQueue(player)) {
//                        // Player was in the queue and successfully removed
//                        // Player wasn't in the queue or world loaded for someone else
//                        if (player.performCommand("mv load sumo_game1")) {
//                            player.performCommand("mv tp sumo_game1");
//                        } else {
//                            // Handle loading error
//                            player.sendMessage("An error has occurred, this is probably on our end.");
//                            System.out.println("ERROR sumo_game1 cannot be loaded");
//                        }
//                    } else {
//
//                    }
//                } else if (sumoWorld2 != null && sumoWorld2.getPlayers().size() < 2) {
//
//
//                } else if (player.performCommand("mv tp sumo_game3")) {
//                } else {
//                    // World is full or not loaded, add player to queue (assuming queue is not full)
//                    if (addToQueue(player)) {
//                        player.sendMessage("All available sumo world is currently full. You've been added to the queue.");
//                        ItemStack barrier = new ItemStack(Material.BARRIER);
//
//                    } else {
//                        player.sendMessage("The queue for the sumo world is also full.");
//
//                    }
//
//                }
//            }
//            return false;
//        }
//    }
//
//
//
//    // Method to add players to a queue (implementation needed)
//    private boolean addToQueue(Player player) {
//        // Implement queue logic to add player (check for queue capacity)
//        return true; // Replace with actual logic to return true if added successfully
//    }
//
//    // Method to remove players from the queue when a spot opens (implementation needed)
//    private boolean removeFromQueue(Player player) {
//        // Implement queue logic to remove player (check if player exists in queue)
//        return true; // Replace with actual logic to return true if removed from queue
//    }
//}

/**
 * 1. When player type /play_duels_sumo or click a sign that will run that command.
 * 2. if the first game is not full they get teleported in to it
 * 3. if it is full try to teleport them to another world
 * 4. if that is full, repeat how many worlds you have
 * 5. if all worlds are full, add them into a queue that will teleport them in to a game as soon as one is open
 * 6. give them an item in the queue that will allow them to leave the queue
 */