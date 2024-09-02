package org.aerial_dad.bedwars_plugin.bedwars.commands;

import org.aerial_dad.bedwars_plugin.Bedwars_plugin;
import org.aerial_dad.bedwars_plugin.bedwars.game.BwPlayer;
import org.aerial_dad.bedwars_plugin.bedwars.lobby.SetLobbyWorld;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Bw_general implements CommandExecutor {
    public enum BW_Mode {
        EDIT,
        SAVE
    }

    public static BW_Mode bwMode = BW_Mode.SAVE;

    private final Bedwars_plugin plugin;


    public static Map<String, World> BW_CREATED_MAPS = new HashMap<>();

    public static Queue<BwPlayer> PLAYER_WAITING_QUEUE = new ConcurrentLinkedQueue<>();

    private Map<String, Player> mapCreators = new HashMap<>();
    private Map<String, Location> mapPos1 = new HashMap<>();
    private Map<String, Location> mapPos2 = new HashMap<>();

    public static void loadPrebuiltDuelGameWorlds(List<World> worlds) {
        prebuiltDuelGameWorlds = worlds;
    }

    public static List<World> prebuiltDuelGameWorlds;



    private static final List<String> BW_VALID_ARGS = Arrays.asList("create", "list", "edit", "save", "pos1", "pos2", "mapbottom", "setqueuespawn", "setlobbyworld","delete", "teleport");


    public Bw_general(Bedwars_plugin plugin) {
        this.plugin = plugin;
    }

    public void doConfigMapSave() {
        plugin.getConfig().set("The maps created are: " + BW_CREATED_MAPS, 1);
        plugin.saveConfig();


    }

    private void doCreate(@Nonnull String[] args, Player player) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Please provide a name for your map. ");
        } else {
            String firstArg = args[0];
            String secondArg = args[1];
            String worldName = secondArg;
            if (BW_CREATED_MAPS.containsKey(worldName)) {
                player.sendMessage("You have already created a map called: " + worldName);
            } else {
                World world = player.getServer().createWorld(new WorldCreator(secondArg));
                player.sendMessage("Loading... ");
                BW_CREATED_MAPS.put(secondArg, world);
                doConfigMapSave();
                player.sendMessage("Loading... ");
                Location newWorldLocation  = new Location(Bukkit.getWorld(secondArg), 0, 60, 0 );
                player.teleport(newWorldLocation);
                player.sendMessage(ChatColor.GREEN + "The map and world " + secondArg + " has been created.");
//                player.sendMessage("Please select a range for your map " + ChatColor.YELLOW + "/bw pos1" + ChatColor.GREEN + " and " + ChatColor.YELLOW + "/bw pos2");
            }

        }



    }

    private boolean doDelete(@Nonnull String[] args, Player player){
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Please provide a name for the map you want to delete.  ");
        } else {
            String firstArg = args[0];
            String secondArg = args[1];
            String worldName = secondArg;
            World world = player.getWorld();
            if (world.equals(worldName)){
                player.sendMessage("You cannot delete a world that you are currently in ");


            }else{
                if (BW_CREATED_MAPS.containsKey(worldName)) {

                    BW_CREATED_MAPS.remove(worldName);

                    Server server = Bukkit.getServer();
                    World deleteWorld = server.getWorld(worldName);

                    if (deleteWorld != null) {
                        server.unloadWorld(deleteWorld, false); // Unload without saving players
                        File worldFolder = deleteWorld.getWorldFolder();
                        for (File file : worldFolder.listFiles()) {
                            file.delete();
                        }

                        worldFolder.delete();
                        System.out.println("World " + worldName + " deleted successfully!");
                        player.sendMessage("World " + worldName + " deleted successfully!");
                    } else {
                        System.out.println("World " + worldName + " not found!");
                        player.sendMessage("World " + worldName + " not found!");
                    }
                }


                }
            }


        return true;
    }

    private void doTeleport(Player player, String worldName) {
        final World world = BW_CREATED_MAPS.get(worldName);
        Location teleLoc = world.getSpawnLocation();
        player.teleport(teleLoc);
////        Location queueLocation = plugin.getConfig().getLocation("Queue spawn: ");
//        if (queueLocation == null) {
//            Location newWorldLocation = new Location(Bukkit.getWorld(worldName), 0, 60, 0);
//            player.teleport(newWorldLocation);
//        } else {
//            player.teleport(queueLocation);
//        }

    }




    private void doSetMapBottom(Player player){
        Location location = player.getLocation();
        double X = location.getX();
        double Y = location.getY();
        double Z = location.getZ();
        player.sendMessage("Bottom of map has been set to " + X + " " + Y + " " + Z + "." );

        plugin.getConfig().set("Bottom of map: ", location );
        plugin.saveConfig();

    }

    private void doSetQueueSpawn(Player player, String[] args){
        Location location = player.getLocation();
        double X = location.getX();
        double Y = location.getY();
        double Z = location.getZ();
        player.sendMessage("Queue spawn is: " + X + " " + Y + " " + Z + "." );


        plugin.getConfig().set("Queue spawn: ", location );
        plugin.saveConfig();
        String worldName = args[1];
        if (worldName.isEmpty());
        player.sendMessage("please provived the map this ");

    }






    private void doList(@Nonnull String[] args, Player player) {
        if (args.length > 1) {
            player.sendMessage(ChatColor.RED + "You do not need a second argument ");
        } else {
            player.sendMessage(ChatColor.DARK_GREEN + "All created maps: " + BW_CREATED_MAPS);
        }

    }

    private void doEdit(Player player) {
//        EDIT = true;
        bwMode = BW_Mode.EDIT;
        player.sendMessage(ChatColor.GREEN + "The game has been set to editing mode");
    }

    private void doSave(Player player) {
//        EDIT = false;
        bwMode = BW_Mode.SAVE;
        player.sendMessage(ChatColor.GREEN + "The game has been set to save mode");
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender,
                             @Nonnull Command cmd,
                             @Nonnull String label,
                             @Nonnull String[] args) {
        if (sender instanceof Player && cmd.getName().equalsIgnoreCase("bw") ||
                cmd.getName().equalsIgnoreCase("bedwars")) {
            Player player = (Player) sender;

            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + "could not find the argument. Try these: ");
                player.sendMessage(ChatColor.YELLOW + " " + BW_VALID_ARGS);
            } else {
                String firstArg = args[0];

                if (BW_VALID_ARGS.contains(firstArg)) {
                    if (firstArg.equalsIgnoreCase("create")) {
                        player.sendMessage("Loading... ");
                        doCreate(args, player);
                    }

                    if (firstArg.equalsIgnoreCase("list")) {
                        doList(args, player);

                    }

//                  Modes

                    else if (firstArg.equalsIgnoreCase("edit")) {
                        doEdit(player);

                    } else if (firstArg.equalsIgnoreCase("save")) {
                        doSave(player);


                    } else {
                        System.err.println(ChatColor.RED + "THE MODE IS NOT EDIT OR SAVE ");

                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Could not find the argument you wrote " + ChatColor.YELLOW + firstArg);
                }


//                setting points on map
                if (bwMode == BW_Mode.EDIT ){
                    if (firstArg.equalsIgnoreCase("mapbottom")){
                        doSetMapBottom(player);
                    }

                    if (firstArg.equalsIgnoreCase("SetQueueSpawn")){
                        doSetQueueSpawn(player,  args);
                    }
                }else{
                    player.sendMessage(ChatColor.YELLOW + "You are now no more in edit mode, so you can not change anything. ");
                }

                if (firstArg.equalsIgnoreCase("setlobbyWorld")){
                    SetLobbyWorld.doSetLobby(sender, args);
                }

                if (firstArg.equalsIgnoreCase("delete")){
                    doDelete(args, player);
                }
                if (firstArg.equalsIgnoreCase("teleport")){
                    if(args.length < 2) {
                        System.out.println("The teleport destination is missing.");
                    }
                    else {
                        String worldName = args[1];
                        if(!BW_CREATED_MAPS.containsKey(worldName)) {
                            System.out.println("The world " + worldName + " doesn't exist or is NOT a bedwar world.");
                        }
                        else {
                            doTeleport(player, worldName);
                        }
                    }


                }




            }
            return false;
        }


        return false;
    }
}