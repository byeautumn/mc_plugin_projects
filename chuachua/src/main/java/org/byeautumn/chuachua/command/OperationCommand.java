package org.byeautumn.chuachua.command;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.byeautumn.chuachua.Chuachua;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.common.LocationVector;
import org.byeautumn.chuachua.common.PlayMode;
import org.byeautumn.chuachua.game.firstland.FirstLandWorldConfigAccessor;
import org.byeautumn.chuachua.generate.PolyWall;
import org.byeautumn.chuachua.generate.SimpleWall;
import org.byeautumn.chuachua.generate.world.pipeline.*;
import org.byeautumn.chuachua.generate.world.pipeline.tree.SCATreeGenerationConfigure;
import org.byeautumn.chuachua.generate.world.pipeline.tree.SCATreeGenerator;
import org.byeautumn.chuachua.generate.world.pipeline.tree.TreeGenerator;
import org.byeautumn.chuachua.io.ChunkExporter;
import org.byeautumn.chuachua.io.ChunkImporter;
import org.byeautumn.chuachua.player.PlayerTracker;
import org.byeautumn.chuachua.player.PlayerUtil;
import org.byeautumn.chuachua.undo.ActionRecord;
import org.byeautumn.chuachua.undo.ActionRecorder;
import org.byeautumn.chuachua.undo.ActionRunner;
import org.byeautumn.chuachua.undo.ActionType;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class OperationCommand implements CommandExecutor {

    private final Map<UUID, String> pendingConfirmations = new HashMap<>();

    private static final List<String> BW_VALID_ARGS = Arrays.asList("exit", "tp", "listWorlds", "createWall", "undo", "undoGen"
            , "redo", "redoGen", "setPlayMode", "polySelect", "cancelSelect", "export", "diaSelect", "import", "createWorld"
            , "getBiome", "chuaWorldInfo", "generateTree", "createFirstLandWorlds", "deleteWorld", "confirm", "cancel");
    private final Chuachua plugin;

    public OperationCommand(Chuachua plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player && cmd.getName().equalsIgnoreCase("cc")) {
            Player player = (Player) sender;
            if (args.length == 0) {
                player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Oops! Command argument missing. Try these:");
                player.sendMessage(ChatColor.YELLOW + "   " + BW_VALID_ARGS);
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
                    player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "Exited the world successfully!");
                } else if (firstArg.equalsIgnoreCase("listWorlds")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " World List " + ChatColor.BLUE + "]================");
                    for (World world : Bukkit.getWorlds()) {
                        UUID worldID = world.getUID();
                        if (Universe.getChuaWorldById(worldID) != null) {
                            player.sendMessage(ChatColor.GOLD + world.getName() + ChatColor.WHITE + " -- " + ChatColor.GRAY + world.getWorldType().getName() + ChatColor.GRAY + "(ChuaGeneratedWorld)");
                        } else {
                            player.sendMessage(ChatColor.AQUA + world.getName() + ChatColor.WHITE + " -- " + ChatColor.GRAY + world.getWorldType().getName());
                        }
                    }
                } else if (firstArg.equalsIgnoreCase("chuaWorldInfo")) {
                    long seed;
                    if (args.length < 2) {
                        World world = player.getWorld();
                        UUID worldID = world.getUID();
                        String worldName = world.getName();
                        if (Universe.getChuaWorldById(worldID) == null) {
                            player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + worldName + " Info " + ChatColor.BLUE + "]================");
                            player.sendMessage(ChatColor.RED + ">> '" + ChatColor.YELLOW + worldName + ChatColor.RED + "' is not a ChuaWorld.");

                        } else {
                            ChuaWorld chuaWorld = Universe.getChuaWorldById(worldID);
                            seed = chuaWorld.getSeed();
                            player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + worldName + " Info " + ChatColor.BLUE + "]================");
                            player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.WHITE + "seed: '" + ChatColor.AQUA + seed + ChatColor.WHITE + "'.");
                            player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.WHITE + "UUID: '" + ChatColor.AQUA + worldID + ChatColor.WHITE + "'.");
                        }


                    } else {
                        String worldName = args[1];
                        World world;
                        try {
                            world = Bukkit.getWorld(worldName);
                        } catch (NullPointerException exception) {
                            player.sendMessage(ChatColor.RED + ">> " + ChatColor.YELLOW + "'" + worldName + "'" + ChatColor.RED + " does not exist.");
                        }
                        if (Bukkit.getWorlds().contains(Bukkit.getWorld(worldName))) {
                            world = Bukkit.getWorld(worldName);
                            UUID worldID = world.getUID();
                            ChuaWorld chuaWorld = Universe.getChuaWorldById(worldID);
                            seed = chuaWorld.getSeed();
                            if (Universe.getChuaWorldById(Bukkit.getWorld(worldName).getUID()) != null) {
                                player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + worldName + " Info " + ChatColor.BLUE + "]================");
                                player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.WHITE + "seed: '" + ChatColor.AQUA + seed + ChatColor.WHITE + "'.");
                                player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.WHITE + "UUID: '" + ChatColor.AQUA + Bukkit.getWorld(worldName).getUID() + ChatColor.WHITE + "'.");
                            } else {
                                player.sendMessage(ChatColor.RED + ">> '" + ChatColor.YELLOW + worldName + ChatColor.RED + "' is not a ChuaWorld.");
                            }

                        }
                    }


                    player.sendMessage(ChatColor.BLUE + "================================================");

                } else if (firstArg.equalsIgnoreCase("createWall")) {
                    World world = player.getWorld();
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Wall Creation " + ChatColor.BLUE + "]================");
                    ActionRecorder recorder = Universe.getActionRecorder(player);

                    if (args.length >= 3 && args[1].equalsIgnoreCase("polySelect")) {
                        if (args.length < 3) {
                            player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Usage: /cc createWall polySelect <height>");
                            return true;
                        }
                        try {
                            int height = Integer.parseInt(args[2]);
                            List<Block> polySelectedBlocks = recorder.getPolySelectedBlocks();
                            PolyWall wall = new PolyWall(polySelectedBlocks, height);
                            wall.setWorld(world);
                            ActionRecord record = wall.generate(player);
                            if (null == record) {
                                player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Wall creation failed.");
                                return true;
                            }
                            recorder.record(ActionType.GENERATION, record);
                            player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "Poly wall created with height " + ChatColor.YELLOW + height + ChatColor.AQUA + ".");
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Invalid height argument.");
                        }
                    } else if (args.length >= 8) {
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

                            ActionRecord record = wall.generate(player);
                            if (null == record) {
                                player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Wall creation failed.");
                                return true;
                            }
                            recorder.record(ActionType.GENERATION, record);
                            player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "Simple wall created with height " + ChatColor.YELLOW + height + ChatColor.AQUA + ".");

                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Invalid location or height argument.");
                            player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Usage: /cc createWall <x1> <y1> <z1> <x2> <y2> <z2> <height>");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Usage: /cc createWall <x1> <y1> <z1> <x2> <y2> <z2> <height> or /cc createWall polySelect <height>");
                    }
                    player.sendMessage(ChatColor.BLUE + "================================================");
                } else if (firstArg.equalsIgnoreCase("undo")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Undo Edit " + ChatColor.BLUE + "]================");
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    ActionRecord action = recorder.getPreviousAction();
                    ActionRunner.undo(action, player);
                    player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "Previous edit has been rolled back.");
                    player.sendMessage(ChatColor.BLUE + "================================================");
                } else if (firstArg.equalsIgnoreCase("undoGen")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Undo Gen " + ChatColor.BLUE + "]================");
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    ActionRecord action = recorder.getPreviousAction(ActionType.GENERATION);
                    ActionRunner.undo(action, player);
                    player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "Previous generation has been rolled back.");
                    player.sendMessage(ChatColor.BLUE + "================================================");
                } else if (firstArg.equalsIgnoreCase("redo")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Redo Edit " + ChatColor.BLUE + "]================");
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    ActionRecord action = recorder.getNextAction();
                    ActionRunner.redo(action, player);
                    player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "Rolled back edit has been redone.");
                    player.sendMessage(ChatColor.BLUE + "================================================");
                } else if (firstArg.equalsIgnoreCase("redoGen")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Redo Gen " + ChatColor.BLUE + "]================");
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    ActionRecord action = recorder.getNextAction(ActionType.GENERATION);
                    ActionRunner.redo(action, player);
                    player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "Rolled back generation has been redone.");
                    player.sendMessage(ChatColor.BLUE + "================================================");
                } else if (firstArg.equalsIgnoreCase("setPlayMode")) {
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Usage: /cc setPlayMode <mode>");
                        return true;
                    }
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Play Mode Set " + ChatColor.BLUE + "]================");
                    String playMode = args[1];
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT.name().equalsIgnoreCase(playMode)) {
                        playerTracker.setPlayMode(PlayMode.EDIT);
                        player.sendMessage(ChatColor.YELLOW + ">> The play mode is set to " + ChatColor.AQUA + playerTracker.getPlayMode().name());
                    } else if (PlayMode.READONLY.name().equalsIgnoreCase(playMode)) {
                        playerTracker.setPlayMode(PlayMode.READONLY);
                        player.sendMessage(ChatColor.YELLOW + ">> The play mode is set to " + ChatColor.AQUA + playerTracker.getPlayMode().name());
                    } else if (PlayMode.UNKNOWN.name().equalsIgnoreCase(playMode)) {
                        playerTracker.setPlayMode(PlayMode.UNKNOWN);
                        player.sendMessage(ChatColor.YELLOW + ">> The play mode is set to " + ChatColor.AQUA + playerTracker.getPlayMode().name());
                    } else {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "The play mode is not recognized: " + playMode);
                    }
                    player.sendMessage(ChatColor.BLUE + "================================================");
                } else if (firstArg.equalsIgnoreCase("polySelect")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Poly Select " + ChatColor.BLUE + "]================");
                    player.sendMessage(ChatColor.YELLOW + ">> PolySelect tool Given!");
                    player.sendMessage(ChatColor.GRAY + ">> (Place the candles where you want each position to be for your polygon.)");
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT != playerTracker.getPlayMode()) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "The play mode needs to be Edit to do polySelect.");
                        return true;
                    }
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    recorder.resetPolySelection();
                    recorder.setPolySelection(true);
                    PlayerUtil.sendObjectToMainHand(player, ActionRecorder.POLY_SELECT_TYPE);
                    player.sendMessage(ChatColor.BLUE + "================================================");
                } else if (firstArg.equalsIgnoreCase("diaSelect")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Dia Select " + ChatColor.BLUE + "]================");
                    player.sendMessage(ChatColor.YELLOW + ">> DiaSelect tool Given!");
                    player.sendMessage(ChatColor.GRAY + ">> (Place 2 candles where the rectangle diagonally defined by these 2 positions will be the range of the block selection.)");
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT != playerTracker.getPlayMode()) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "The play mode needs to be Edit to do diaSelect.");
                        return true;
                    }
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    recorder.resetDiaSelection();
                    recorder.setDiaSelection(true);
                    PlayerUtil.sendObjectToMainHand(player, ActionRecorder.DIA_SELECT_TYPE);
                    player.sendMessage(ChatColor.BLUE + "================================================");
                } else if (firstArg.equalsIgnoreCase("cancelSelect")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Cancel Select " + ChatColor.BLUE + "]================");
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT != playerTracker.getPlayMode()) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "The play mode needs to be Edit to do cancelSelect.");
                        return true;
                    }
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    recorder.resetPolySelection();
                    recorder.resetDiaSelection();
                    PlayerInventory inventory = player.getInventory();
                    ItemStack item = inventory.getItem(0);
                    player.getInventory().setItemInMainHand(item);
                    player.sendMessage(ChatColor.BLUE + "================================================");
                } else if (firstArg.equalsIgnoreCase("export")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Export Chunks " + ChatColor.BLUE + "]================");
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT != playerTracker.getPlayMode()) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "The play mode needs to be Edit to do export.");
                        return true;
                    }
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Usage: /cc export <chunkName> <diaSelect|x1 y1 z1 x2 y2 z2>");
                        return true;
                    }
                    String chunkName = args[1];
                    Block b1 = null, b2 = null;
                    if (args[2].equalsIgnoreCase("diaSelect")) {
                        ActionRecorder recorder = Universe.getActionRecorder(player);
                        if (recorder.getDiaSelectedBlocks().size() < 2) {
                            player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "The dia selection is not complete. Please select 2 blocks before calling export.");
                            return true;
                        }
                        List<Block> diaSelectedBlocks = recorder.getDiaSelectedBlocks();
                        b1 = diaSelectedBlocks.get(0);
                        b2 = diaSelectedBlocks.get(1);

                        recorder.resetDiaSelection();
                    } else {
                        if (args.length < 8) {
                            player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Usage: /cc export <chunkName> <x1> <y1> <z1> <x2> <y2> <z2>");
                            return true;
                        }

                        int x1 = Integer.parseInt(args[2]), y1 = Integer.parseInt(args[3]), z1 = Integer.parseInt(args[4]);
                        int x2 = Integer.parseInt(args[5]), y2 = Integer.parseInt(args[6]), z2 = Integer.parseInt(args[7]);
                        World world = player.getWorld();
                        b1 = world.getBlockAt(x1, y1, z1);
                        b2 = world.getBlockAt(x2, y2, z2);
                    }

                    if (null == b1 || null == b2) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "There is at least one block has not been correctly selected. Export is terminated.");
                        return true;
                    }

                    ChunkExporter exporter = new ChunkExporter();
                    if (exporter.exportChunk(chunkName, b1, b2)) {
                        player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "Export " + chunkName + " succeeded.");
                    } else {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Export " + chunkName + " failed.");
                    }
                    player.sendMessage(ChatColor.BLUE + "================================================");
                } else if (firstArg.equalsIgnoreCase("import")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Import Chunks " + ChatColor.BLUE + "]================");
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT != playerTracker.getPlayMode()) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "The play mode needs to be Edit to do import.");
                        return true;
                    }
                    if (args.length != 2 && args.length != 5) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Usage: /cc import <chunkName> [x y z]");
                        return true;
                    }
                    String chunkName = args[1];
                    Block selectedBlock = null;
                    if (args.length == 2) {
                        selectedBlock = player.getLocation().getBlock();
                    } else {
                        int x1 = Integer.parseInt(args[2]), y1 = Integer.parseInt(args[3]), z1 = Integer.parseInt(args[4]);
                        World world = player.getWorld();
                        selectedBlock = world.getBlockAt(x1, y1, z1);
                    }
                    ChunkImporter importer = new ChunkImporter(player);
                    if (!importer.exists(chunkName)) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "'" + chunkName + "' doesn't exists.");
                        return true;
                    }
                    ActionRecorder recorder = Universe.getActionRecorder(player);
                    ActionRecord record = importer.importChunk(chunkName, selectedBlock);

                    if (null != record) {
                        player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "Import " + chunkName + " succeeded.");
                        recorder.record(ActionType.GENERATION, record);
                    } else {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Import " + chunkName + " failed.");
                    }
                    player.sendMessage(ChatColor.BLUE + "================================================");
                } else if (firstArg.equalsIgnoreCase("createWorld")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Create World " + ChatColor.BLUE + "]================");
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Usage: /cc createWorld <worldName> [seed]");
                        return true;
                    }

                    String worldName = args[1];
                    long createSeed;
                    if(Bukkit.getWorlds().contains(Bukkit.getWorld(worldName))){
                        try{
                            player.sendMessage(ChatColor.AQUA + worldName + ChatColor.YELLOW + " has already ben created, so you were teleported there.");
                            Universe.teleport(player, Bukkit.getWorld(worldName).getSpawnLocation());
                        } catch (NullPointerException exception){
                            player.sendMessage( ChatColor.RED + ">> an error occurred!");
                        }
                    }else {
                        if (args.length >= 3 && args[2] != null && !args[2].isEmpty()) {
                            try {

                                createSeed = Long.parseLong(args[2]);
                            } catch (NumberFormatException nfe) {
                                player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Please enter a valid seed.");
                                return true;
                            }
                        }else {
                            Random random = new Random();
                            createSeed = random.nextLong();
                        }
                        ChuaWorld chuaWorld = Universe.createWorld(createSeed, worldName);
                        player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "A New World is Being Generated with the seed: '" + createSeed + "'!");
                        Universe.teleport(player, chuaWorld.getWorld().getSpawnLocation());
                        ChuaWorldConfigAccessor accessor = new ChuaWorldConfigAccessor(plugin);
                        accessor.addNewWorld(worldName, createSeed, chuaWorld.getWorld().getSpawnLocation());
                    }
                    player.sendMessage(ChatColor.BLUE + "================================================");
                } else if (firstArg.equalsIgnoreCase("getBiome")) {
                    long seed;
                    Location playerLocation = player.getLocation();
                    World currentWorld = playerLocation.getWorld();
                    Biome currentBiome = currentWorld.getBiome(playerLocation);
                    try {
                        UUID worldID = currentWorld.getUID();

                        if (Universe.getChuaWorldById(worldID) == null) {
                            player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Create World " + ChatColor.BLUE + "]================");
                            player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "'" + player.getDisplayName() + "' " + ChatColor.YELLOW + "is currently in the biome: '" + ChatColor.AQUA + ChatColor.BOLD + currentBiome + "'.");
                            player.sendMessage(ChatColor.BLUE + "================================================");
                            player.sendMessage(ChatColor.RED + ">> " + "'" + ChatColor.AQUA + "" + currentWorld.getName() + "'" + ChatColor.RED + " is not in the ChuaWorld creation pool, So no ChuaSeed cant be provided");
                            player.sendMessage(ChatColor.YELLOW + "" + currentWorld.getName() + " has a UUID of " + ChatColor.AQUA + currentWorld.getUID());
                            player.sendMessage(ChatColor.YELLOW + ">>" + ChatColor.AQUA + currentWorld.getName() + ChatColor.WHITE + "--" + ChatColor.AQUA + player.getDisplayName() + "is in the Biome " + player.getWorld().getBiome(player.getLocation()));
                            player.sendMessage(ChatColor.BLUE + "================================================");
                        } else {
                            ChuaWorld chuaWorld = Universe.getChuaWorldById(worldID);
                            seed = chuaWorld.getSeed();
                            if (Bukkit.getWorlds().contains(currentWorld)) {
                                BiomeConstants biomeConstants = new BiomeConstants();
                                int playerX = playerLocation.getBlockX();
                                int playerZ = playerLocation.getBlockZ();
                                // Ensure LocationBiomeValues constructor parameters match its actual definition.
                                // You are using seed + N for different noise types, which is common.
                                LocationBiomeValues locationBiomeValues = new LocationBiomeValues(seed + 2, seed + 1, seed, seed + 3, seed + 4, seed + 5);

                                player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Create World " + ChatColor.BLUE + "]================");
                                player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "'" + player.getDisplayName() + "' " + ChatColor.YELLOW + "is currently in the biome: '" + ChatColor.AQUA + ChatColor.BOLD + currentBiome + "'.");
                                player.sendMessage(ChatColor.BLUE + "================================================");
                                player.sendMessage(ChatColor.WHITE + ">> " + ChatColor.AQUA + "TEMP. = " + ChatColor.YELLOW + "'" + locationBiomeValues.getTemp(playerX + biomeConstants.getTempScale(), playerZ + biomeConstants.getTempScale()) + "'.");
                                player.sendMessage(ChatColor.WHITE + ">> " + ChatColor.AQUA + "HYDR. = " + ChatColor.YELLOW + "'" + locationBiomeValues.getHydr(playerX + biomeConstants.getHydrScale(), playerZ + biomeConstants.getHydrScale()) + "'.");
                                player.sendMessage(ChatColor.WHITE + ">> " + ChatColor.AQUA + "ALT. = " + ChatColor.YELLOW + "'" + locationBiomeValues.getAltitude(playerX + biomeConstants.getAltitudeScale(), playerZ + biomeConstants.getAltitudeScale()) + "'.");
                                player.sendMessage(ChatColor.WHITE + ">> " + ChatColor.AQUA + "CONT. = " + ChatColor.YELLOW + "'" + locationBiomeValues.getContinental(playerX + biomeConstants.getContinentalScale(), playerZ + biomeConstants.getContinentalScale()) + "'.");
                                player.sendMessage(ChatColor.WHITE + ">> " + ChatColor.AQUA + "REG. = " + ChatColor.YELLOW + "'" + locationBiomeValues.getRegional(playerX + biomeConstants.getRegionalScale(), playerZ + biomeConstants.getRegionalScale()) + "'.");
                                player.sendMessage(ChatColor.WHITE + ">> " + ChatColor.AQUA + "EROS. = " + ChatColor.YELLOW + "'" + locationBiomeValues.getErosion(playerX + biomeConstants.getErosionScale(), playerZ + biomeConstants.getErosionScale()) + "'.");

                                // --- FIX: Add the Regional value here ---
                                player.sendMessage(ChatColor.WHITE + ">> " + ChatColor.AQUA + "REGION = " + ChatColor.YELLOW + "'" + locationBiomeValues.getRegional(playerX + biomeConstants.getRegionalScale(), playerZ + biomeConstants.getRegionalScale()) + "'.");
                                // --- END FIX ---

                                player.sendMessage(ChatColor.BLUE + "================================================");

//                        } else {
//                            player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Create World " + ChatColor.BLUE + "]================");
//                            player.sendMessage(ChatColor.GREEN + ">> " + ChatColor.AQUA + "'" + player.getDisplayName() + "' " + ChatColor.YELLOW + "is currently in the biome: '" + ChatColor.AQUA + ChatColor.BOLD + currentBiome + "'.");
//                            player.sendMessage(ChatColor.BLUE + "================================================");
//                            player.sendMessage(ChatColor.RED + ">> " + "'" + ChatColor.AQUA + "" + currentWorld + "'" + ChatColor.RED + " is not in the ChuaWorld creation pool, So no ChuaSeed can be provided");
//                            player.sendMessage(ChatColor.YELLOW + "" + currentWorld + " has a UUID of " + ChatColor.AQUA + currentWorld.getUID());
//                            player.sendMessage(ChatColor.BLUE + "================================================");
//                        }
                            }
                        }
                    } catch (NullPointerException exception) {
                        System.out.println(exception);
                        player.sendMessage(ChatColor.BLUE + "================================================");
                        player.sendMessage(ChatColor.RED + ">> Cannot figure out the seed of the world: '" + ChatColor.AQUA + "" + currentWorld.getName() + ChatColor.RED + "'.");
                        player.sendMessage("This may be a temporary problem and will be fixed later.");
                        player.sendMessage(ChatColor.BLUE + "================================================");
                    }


                } else if (firstArg.equalsIgnoreCase("generateTree")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Generating a tree " + ChatColor.BLUE + "]================");
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT != playerTracker.getPlayMode()) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "The play mode needs to be Edit to do generation.");
                        return true;
                    }
                    if (args.length != 2) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Usage: /cc generateTree treeType");
                        return true;
                    }
                    String treeType = args[1];
                    Location location = player.getLocation();
//                    TreeConfigure treeConfigure = TreeGenerationOrganizer.getTreeConfigure(treeType);
//                    if (treeConfigure == null) {
//                        player.sendMessage(ChatColor.RED + ">> " + "'" + ChatColor.AQUA + "" + treeType + "'" + ChatColor.RED + " is not a pre-defined tree type.");
//
//                    } else {
//                        LSystemTreeGenerator treeGenerator = new LSystemTreeGenerator();
//                        treeGenerator.generate(location, treeConfigure);
//                    }

                    // Create an instance of the specific configuration

                    System.out.println("Generating " + treeType + " trees at [" + location.toString() + "].");
                    SCATreeGenerationConfigure scaConfigure = SCATreeGenerationConfigure.getDefaultOakConfig();

                    // Pass it to the generic interface type
                    TreeGenerator treeGenerator = new SCATreeGenerator();
                    boolean success = treeGenerator.generate(location, scaConfigure); // Pass the specific config

                    if (success) {
                        player.sendMessage("Tree generated successfully!");
                    } else {
                        player.sendMessage("Failed to generate tree. Check console for errors or space availability.");
                    }

                    player.sendMessage(ChatColor.BLUE + "================================================");

                } else if (firstArg.equalsIgnoreCase("createFirstLandWorlds")) {
                    player.sendMessage(ChatColor.BLUE + "=================[" + ChatColor.GOLD + " Generating More Worlds " + ChatColor.BLUE + "]================");
                    PlayerTracker playerTracker = Universe.getPlayerTracker(player);
                    if (PlayMode.EDIT != playerTracker.getPlayMode()) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "The play mode needs to be Edit to do generation.");
                        return true;
                    }
                    if (args.length != 2) {
                        player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Usage: /cc createFirstLandWorlds [amount]");
                        return true;
                    }

                    String worldAmountString = args[1];
                    int worldAmount = 0;
                    try {
                        worldAmount = Integer.parseInt(worldAmountString);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "The value '" + ChatColor.YELLOW + worldAmountString + "' " + ChatColor.RED + "is not a number, so the command cannot be performed.");
                        return true;
                    }
                    if (worldAmount <= 0) {
                        player.sendMessage(ChatColor.RED + "The value '" + ChatColor.YELLOW + worldAmount + "' " + ChatColor.RED + "is non-positive");
                        return true;
                    }

                    // New logic: Check if confirmation is required and add warning text.
                    if (worldAmount >= 5) {
                        player.sendMessage(""); // Add a blank line for readability.
                        player.sendMessage(ChatColor.RED + "WARNING: Generating " + worldAmount + " worlds is a resource-intensive task.");
                        player.sendMessage(ChatColor.RED + "This may cause temporary server lag, crashes, or data corruption if not handled properly.");
                        player.sendMessage(""); // Add another blank line.

                        pendingConfirmations.put(player.getUniqueId(), "create:" + worldAmount);
                        player.sendMessage(ChatColor.GOLD + "You are about to create " + worldAmount + " new worlds.");
                        player.sendMessage(ChatColor.YELLOW + "To confirm, type " + ChatColor.GREEN + "/cc confirm" + ChatColor.YELLOW + ". To cancel, type " + ChatColor.RED + "/cc cancel");
                        return true;
                    }

                    // Existing world creation logic for less than 5 worlds.
                    FirstLandWorldConfigAccessor configAccessor = new FirstLandWorldConfigAccessor(plugin);
                    Random random = new Random();

                    Universe.loadFirstLandWorldsToMap(configAccessor);

                    Set<Integer> existingWorldNumbers = configAccessor.getKnownWorlds().stream()
                            .map(worldName -> {
                                try {
                                    return Integer.parseInt(worldName.substring("First_Land_World_".length()));
                                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                                    return -1;
                                }
                            })
                            .filter(number -> number != -1)
                            .collect(Collectors.toSet());

                    int nextAvailableNumber = 0;
                    while (existingWorldNumbers.contains(nextAvailableNumber)) {
                        nextAvailableNumber++;
                    }

                    int worldsCreatedSuccessfully = 0;

                    for (int i = 0; i < worldAmount; i++) {
                        String formattedNumber = String.format("%02d", nextAvailableNumber);
                        String worldName = "First_Land_World_" + formattedNumber;

                        long seed = random.nextLong();
                        boolean success = Universe.processFirstLandWorldCreation(worldName, seed, player, configAccessor, plugin);

                        if (success) {
                            worldsCreatedSuccessfully++;
                            existingWorldNumbers.add(nextAvailableNumber);
                            while (existingWorldNumbers.contains(nextAvailableNumber)) {
                                nextAvailableNumber++;
                            }
                        }
                    }

                    if (worldsCreatedSuccessfully > 0) {
                        player.sendMessage(ChatColor.BLUE + "Successfully created " + worldsCreatedSuccessfully + " new worlds.");
                    } else {
                        player.sendMessage(ChatColor.RED + "No new worlds were created.");
                    }
                    player.sendMessage(ChatColor.BLUE + "================================================");
                    return true;
                } else if (firstArg.equalsIgnoreCase("deleteWorld")) {
                    if (args.length < 2) {
                        player.sendMessage(ChatColor.RED + "Usage: /cc deleteWorld [worldName] or /cc deleteWorld [wildcard]");
                        return true;
                    }

                    String target = args[1];
                    List<String> worldsToDelete = new ArrayList<>();
                    FirstLandWorldConfigAccessor configAccessor = new FirstLandWorldConfigAccessor(plugin);

                    if (target.contains("*")) {
                        // Wildcard deletion logic
                        String prefix = target.substring(0, target.indexOf('*'));
                        configAccessor.getKnownWorlds().forEach(worldName -> {
                            if (worldName.startsWith(prefix) && !Universe.isVanillaWorld(worldName)) {
                                worldsToDelete.add(worldName);
                            }
                        });
                        if (worldsToDelete.isEmpty()) {
                            player.sendMessage(ChatColor.RED + "No worlds found matching the wildcard '" + target + "'.");
                            return true;
                        }
                    } else {
                        // Single world deletion logic
                        if (Universe.isVanillaWorld(target)) {
                            player.sendMessage(ChatColor.RED + "You cannot delete a vanilla world!");
                            return true;
                        }
                        worldsToDelete.add(target);
                    }

                    // Convert the list of worlds to a string to store in the universal map
                    String worldsToDeleteString = String.join(",", worldsToDelete);
                    pendingConfirmations.put(player.getUniqueId(), "delete:" + worldsToDeleteString);

                    player.sendMessage(ChatColor.GOLD + "You are about to delete the following worlds: " + ChatColor.YELLOW + String.join(", ", worldsToDelete));
                    player.sendMessage(ChatColor.YELLOW + "To confirm, type " + ChatColor.GREEN + "/cc confirm" + ChatColor.YELLOW + ". To cancel, type " + ChatColor.RED + "/cc cancel");
                    return true;

                } else if (firstArg.equalsIgnoreCase("confirm")) {
                    String pendingAction = pendingConfirmations.remove(player.getUniqueId());

                    if (pendingAction == null) {
                        player.sendMessage(ChatColor.RED + "You have no pending actions to confirm.");
                        return true;
                    }

                    if (pendingAction.startsWith("delete:")) {
                        String worldsToDeleteString = pendingAction.substring("delete:".length());
                        List<String> worldsToConfirm = Arrays.asList(worldsToDeleteString.split(","));

                        player.sendMessage(ChatColor.BLUE + "Starting deletion of " + worldsToConfirm.size() + " world(s)...");
                        FirstLandWorldConfigAccessor configAccessor = new FirstLandWorldConfigAccessor(plugin);
                        for (String worldName : worldsToConfirm) {
                            boolean success = Universe.deleteFirstLandWorld(worldName, player, configAccessor);
                            if (success) {
                                player.sendMessage(ChatColor.GREEN + "Successfully deleted world '" + worldName + "'.");
                            } else {
                                player.sendMessage(ChatColor.RED + "Failed to delete world '" + worldName + "'.");
                            }
                        }
                    } else if (pendingAction.startsWith("create:")) {
                        // New create logic
                        int worldAmount = Integer.parseInt(pendingAction.substring("create:".length()));

                        FirstLandWorldConfigAccessor configAccessor = new FirstLandWorldConfigAccessor(plugin);
                        Random random = new Random();

                        Universe.loadFirstLandWorldsToMap(configAccessor);

                        Set<Integer> existingWorldNumbers = configAccessor.getKnownWorlds().stream()
                                .map(worldName -> {
                                    try {
                                        return Integer.parseInt(worldName.substring("First_Land_World_".length()));
                                    } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                                        return -1;
                                    }
                                })
                                .filter(number -> number != -1)
                                .collect(Collectors.toSet());

                        int nextAvailableNumber = 0;
                        while (existingWorldNumbers.contains(nextAvailableNumber)) {
                            nextAvailableNumber++;
                        }

                        int worldsCreatedSuccessfully = 0;

                        for (int i = 0; i < worldAmount; i++) {
                            String formattedNumber = String.format("%02d", nextAvailableNumber);
                            String worldName = "First_Land_World_" + formattedNumber;

                            long seed = random.nextLong();
                            boolean success = Universe.processFirstLandWorldCreation(worldName, seed, player, configAccessor, plugin);

                            if (success) {
                                worldsCreatedSuccessfully++;
                                existingWorldNumbers.add(nextAvailableNumber);
                                while (existingWorldNumbers.contains(nextAvailableNumber)) {
                                    nextAvailableNumber++;
                                }
                            }
                        }

                        if (worldsCreatedSuccessfully > 0) {
                            player.sendMessage(ChatColor.BLUE + "Successfully created " + worldsCreatedSuccessfully + " new worlds.");
                        } else {
                            player.sendMessage(ChatColor.RED + "No new worlds were created.");
                        }
                        player.sendMessage(ChatColor.BLUE + "================================================");
                    } else {
                        player.sendMessage(ChatColor.RED + "Unknown pending action. Please contact an admin.");
                    }
                    return true;

                } else if (firstArg.equalsIgnoreCase("cancel")) {
                    if (pendingConfirmations.remove(player.getUniqueId()) != null) {
                        player.sendMessage(ChatColor.GREEN + "Action cancelled.");
                    } else {
                        player.sendMessage(ChatColor.RED + "You have no pending actions to cancel.");
                    }
                    return true;
                }

                } else {
                    player.sendMessage(ChatColor.RED + ">> " + ChatColor.GOLD + "Could not find the argument you wrote " + ChatColor.YELLOW + firstArg);
                }

            }
        return true;
    }
}
