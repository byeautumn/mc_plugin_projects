package org.byeautumn.chuachua.command;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.byeautumn.chuachua.Chuachua;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.common.LocationVector;
import org.byeautumn.chuachua.common.PlayMode;
import org.byeautumn.chuachua.generate.PolyWall;
import org.byeautumn.chuachua.generate.SimpleWall;
import org.byeautumn.chuachua.io.ChunkExporter;
import org.byeautumn.chuachua.io.ChunkImporter;
import org.byeautumn.chuachua.player.PlayerTracker;
import org.byeautumn.chuachua.player.PlayerUtil;
import org.byeautumn.chuachua.undo.ActionRecord;
import org.byeautumn.chuachua.undo.ActionRecorder;
import org.byeautumn.chuachua.undo.ActionRunner;
import org.byeautumn.chuachua.undo.ActionType;

import java.util.Arrays;
import java.util.List;

public class OperationCommand  implements CommandExecutor {

    private static final List<String> BW_VALID_ARGS = Arrays.asList("exit", "tp", "listWorlds", "createWall", "undo", "undoGen"
            , "redo", "redoGen", "setPlayMode", "polySelect", "cancelSelect", "export", "diaSelect", "import");
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
                else if (firstArg.equalsIgnoreCase("listWorlds")) {
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                    for (World world : Bukkit.getWorlds()) {
                        player.sendMessage(world.getName() + " -- " + world.getWorldType().getName());
                    }
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                }
                else if (firstArg.equalsIgnoreCase("createWall")) {
                    World world = player.getWorld();
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Invalid Arguments for command " + firstArg);
                        return false;
                    }
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    if (args[1].equalsIgnoreCase("polySelect")) {
                        int height = Integer.parseInt(args[2]);
                        List<Block> polySelectedBlocks = recorder.getPolySelectedBlocks();
                        PolyWall wall = new PolyWall(polySelectedBlocks, height);
                        wall.setWorld(world);
                        recorder.record(ActionType.GENERATION,wall.generate());
                        recorder.resetPolySelection();
                    }
                    else {
                        if (args.length < 8) {
                            player.sendMessage(ChatColor.RED + "Invalid Arguments for command " + firstArg);
                            return false;
                        }
                        else {
                            try {
                                LocationVector[] posArr = new LocationVector[2];
                                for (int idx = 0; idx < 2; ++idx) {
                                    double x = Double.parseDouble(args[idx * 3 + 1]);
                                    double y = Double.parseDouble(args[idx * 3 + 2]);
                                    double z = Double.parseDouble(args[idx * 3 + 3]);
                                    posArr[idx] = new LocationVector(x, y, z);
                                }

                                int height = Integer.parseInt(args[7]);
                                SimpleWall wall = new SimpleWall(posArr[0], posArr[1], height);
                                wall.setWorld(world);


                                recorder.record(ActionType.GENERATION,wall.generate());

                            } catch (Exception e) {
                                player.sendMessage(e.getMessage());
                            }
                        }
                    }
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                }
                else if (firstArg.equalsIgnoreCase("undo")) {
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    ActionRecord action = recorder.getPreviousAction();
                    ActionRunner.undo(action);
                    player.sendMessage(ChatColor.GREEN + "Previous edit has been rolled back.");
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                }
                else if (firstArg.equalsIgnoreCase("undoGen")) {
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    ActionRecord action = recorder.getPreviousAction(ActionType.GENERATION);
                    ActionRunner.undo(action);
                    player.sendMessage(ChatColor.GREEN + "Previous generation has been rolled back.");
                    player.sendMessage(ChatColor.BLUE + "===================================================");

                }
                else if (firstArg.equalsIgnoreCase("redo")) {
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    ActionRecord action = recorder.getNextAction();
                    ActionRunner.redo(action);
                    player.sendMessage(ChatColor.GREEN + "Rolled back edit has been redone.");
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                }
                else if (firstArg.equalsIgnoreCase("redoGen")) {
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    ActionRecord action = recorder.getNextAction(ActionType.GENERATION);
                    ActionRunner.redo(action);
                    player.sendMessage(ChatColor.GREEN + "Rolled back generation has been redone.");
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                }
                else if (firstArg.equalsIgnoreCase("setPlayMode")) {
                    if (args.length < 2) {
                        player.sendMessage("Invalid Arguments for command " + firstArg);
                        return false;
                    }
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                    String playMode = args[1];
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT.name().equalsIgnoreCase(playMode)) {

                        playerTracker.setPlayMode(PlayMode.EDIT);
                        player.sendMessage(ChatColor.YELLOW + "The play mode is set to " + ChatColor.AQUA + playerTracker.getPlayMode().name());
                    }
                    else if (PlayMode.READONLY.name().equalsIgnoreCase(playMode)) {
                        playerTracker.setPlayMode(PlayMode.READONLY);
                        player.sendMessage(ChatColor.YELLOW + "The play mode is set to " + ChatColor.AQUA + playerTracker.getPlayMode().name());
                    }
                    else if (PlayMode.UNKNOWN.name().equalsIgnoreCase(playMode)) {
                        playerTracker.setPlayMode(PlayMode.UNKNOWN);
                        player.sendMessage(ChatColor.YELLOW + "The play mode is set to " + ChatColor.AQUA + playerTracker.getPlayMode().name());
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "The play mode is not recognized: " + playMode);
                    }
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                }
                else if (firstArg.equalsIgnoreCase("polySelect")) {
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                    player.sendMessage(ChatColor.YELLOW + "PolySelect tool Given!");
                    player.sendMessage(ChatColor.GRAY + "(Place the candles where you want each position to be for your polygon.)");
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT != playerTracker.getPlayMode()) {
                        player.sendMessage(ChatColor.RED + "The play mode needs to be Edit to do polySelect.");
                        return false;
                    }
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    recorder.resetPolySelection();
                    recorder.setPolySelection(true);
                    PlayerUtil.sendObjectToMainHand(player, ActionRecorder.POLY_SELECT_TYPE);
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                }
                else if (firstArg.equalsIgnoreCase("diaSelect")) {
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                    player.sendMessage(ChatColor.YELLOW + "DiaSelect tool Given!");
                    player.sendMessage(ChatColor.GRAY + "(Place 2 candles where the rectangle diagonally defined by these 2 positions will be the range of the block selection.)");
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT != playerTracker.getPlayMode()) {
                        player.sendMessage(ChatColor.RED + "The play mode needs to be Edit to do diaSelect.");
                        return false;
                    }
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    recorder.resetDiaSelection();
                    recorder.setDiaSelection(true);
                    PlayerUtil.sendObjectToMainHand(player, ActionRecorder.DIA_SELECT_TYPE);
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                }
                else if (firstArg.equalsIgnoreCase("cancelSelect")) {
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT != playerTracker.getPlayMode()) {
                        player.sendMessage(ChatColor.RED + "The play mode needs to be Edit to do cancelSelect.");
                        return false;
                    }
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    recorder.resetPolySelection();
                    recorder.resetDiaSelection();
                    PlayerInventory inventory = player.getInventory();
                    ItemStack item = inventory.getItem(0);
                    player.getInventory().setItemInMainHand(item);
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                }
                else if (firstArg.equalsIgnoreCase("export")) {
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT != playerTracker.getPlayMode()) {
                        player.sendMessage(ChatColor.RED + "The play mode needs to be Edit to do export.");
                        return false;
                    }
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Invalid Arguments for command " + firstArg);
                        return false;
                    }
                    String chunkName = args[1];
                    Block b1 = null, b2 = null;
                    if (args[2].equalsIgnoreCase("diaSelect")) {
                        ActionRecorder recorder = Universe.getActionRecorder(player);
                        if (recorder.getDiaSelectedBlocks().size() < 2) {
                            player.sendMessage(ChatColor.RED + "The dia selection is not complete. Please select 2 blocks before calling export.");
                            return false;
                        }
                        List<Block> diaSelectedBlocks = recorder.getDiaSelectedBlocks();
                        b1 = diaSelectedBlocks.get(0);
                        b2 = diaSelectedBlocks.get(1);

                        recorder.resetDiaSelection();
                    }
                    else {
                        if (args.length < 8) {
                            player.sendMessage(ChatColor.RED + "Invalid Arguments for command " + firstArg);
                            return false;
                        }

                        int x1 = Integer.parseInt(args[2]), y1 = Integer.parseInt(args[3]), z1 = Integer.parseInt(args[4]);
                        int x2 = Integer.parseInt(args[5]), y2 = Integer.parseInt(args[6]), z2 = Integer.parseInt(args[7]);
                        World world = player.getWorld();
                        b1 = world.getBlockAt(x1, y1, z1);
                        b2 = world.getBlockAt(x2, y2, z2);
                    }

                    if (null == b1 || null == b2) {
                        player.sendMessage(ChatColor.RED + "There is at least one block has not been correctly selected. Export is terminated.");
                        return false;
                    }

                    ChunkExporter exporter = new ChunkExporter();
                    if (exporter.exportChunk(chunkName, b1, b2)) {
                        player.sendMessage(ChatColor.GREEN + "Export " + chunkName + " succeeded.");
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Export " + chunkName + " failed.");
                    }
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                }
                else if (firstArg.equalsIgnoreCase("import")) {
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT != playerTracker.getPlayMode()) {
                        player.sendMessage(ChatColor.RED + "The play mode needs to be Edit to do import.");
                        return false;
                    }
                    if (args.length != 2 && args.length != 5) {
                        player.sendMessage(ChatColor.RED + "Invalid Arguments for command " + firstArg);
                        return false;
                    }
                    String chunkName = args[1];
                    Block selectedBlock = null;
                    if (args.length == 2) {
                        selectedBlock = player.getLocation().getBlock();
                    }
                    else {
                        int x1 = Integer.parseInt(args[2]), y1 = Integer.parseInt(args[3]), z1 = Integer.parseInt(args[4]);
                        World world = player.getWorld();
                        selectedBlock = world.getBlockAt(x1, y1, z1);
                    }
                    ChunkImporter importer = new ChunkImporter();
                    if (!importer.exists(chunkName)) {
                        player.sendMessage(ChatColor.RED + "'" + chunkName + "' doesn't exists.");
                        return false;
                    }
                    if (importer.importChunk(chunkName, selectedBlock)) {
                        player.sendMessage(ChatColor.GREEN + "Import " + chunkName + " succeeded.");
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "Import " + chunkName + " failed.");
                    }
                    player.sendMessage(ChatColor.BLUE + "===================================================");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Could not find the argument you wrote " + ChatColor.YELLOW + firstArg);
            }
        }


        return true;
    }
}
