package org.byeautumn.chuachua.game.firstland;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.byeautumn.chuachua.Chuachua;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.generate.world.pipeline.ChuaWorld;
import org.byeautumn.chuachua.player.InventoryDataAccessor;
import org.byeautumn.chuachua.player.PlayerData;
import org.byeautumn.chuachua.player.PlayerDataAccessor;
import org.byeautumn.chuachua.player.matrix.PlayerSurvivalMatrix;

import java.util.List;
import java.util.UUID;

public class FirstLandGame {

    private final List<UUID> players;

    private final ChuaWorld chuaWorld;

    private final WorldData worldData;

    private static final double FAT_DECAY_DELTA = 50.0 / 720000.0;

    private static final double HYDRATION_DECAY_DELTA = 100.0 / 12000.0;

    private static final double PROTEIN_DECAY_DELTA = 100.0 / 1_680_000.0;

    private static final double CARBS_DECAY_DELTA = 100.0 / 120_000.0;

    private int matrixRunID;

    private GameStatus gameStatus = GameStatus.UNKNOWN;

    private PlayerDataAccessor playerDataAccessor;

    private InventoryDataAccessor inventoryDataAccessor;


    public FirstLandGame(WorldData worldData) {
        this.worldData = worldData;
        this.chuaWorld = new ChuaWorld(worldData.getSeed(), Bukkit.getWorld(worldData.getWorldUUID()));
        System.out.println(chuaWorld.getWorld().getUID() + "chuaWorld.getWorld().getUID()!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        WorldDataAccessor worldDataAccessor = WorldDataAccessor.getInstance();
        System.out.println("WorldDataAccessor!!!!!!!!!!!!!!!!!!!!!!!!!!!!" + worldDataAccessor);
        this.players = worldDataAccessor.getWorldDataPlayers(chuaWorld.getWorld().getUID()).getPlayers();
        this.playerDataAccessor = PlayerDataAccessor.getInstance();
        this.inventoryDataAccessor = InventoryDataAccessor.getInstance();
    }


    public void start() {
        this.gameStatus = GameStatus.ONLINE;
        startPlayerMatrixCheck();
    }

    private void startPlayerMatrixCheck(){
        this.matrixRunID = Bukkit.getScheduler().scheduleSyncRepeatingTask(Chuachua.getInstance, new Runnable() {
            @Override
            public void run() {
                World world = chuaWorld.getWorld();
                List<Player> worldPlayers = world.getPlayers();
//                System.out.println("The Plugin has run here! 'startPlayerMatrixCheck'");
//                if (worldPlayers.isEmpty()){
//                    System.out.println("world is empty stopping player matrix checking. ");
//                    pauseMatrixChecking();
//                    return;
//                }
                for (UUID playerUUID : players){
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (worldPlayers.contains(player)){
                        System.out.println("Started Player Matrix Checking!!!!");;
                        player.sendMessage("[LOG] Started Player Matrix Checking!!!!");
                        PlayerData playerData = playerDataAccessor.getPlayerData(playerUUID, world.getUID(), world.getName());
                        if (playerData != null){
                            if (playerData.getLastMatrixUpdateTime() < 0) {
                                playerData.toBuilder()
                                        .lastMatrixUpdateTime(world.getGameTime())
                                        .build();
                            }
                            else {
                                long lastUpdateTime = playerData.getLastMatrixUpdateTime();
                                System.out.println("Before - Last Update Time Reading: " + world.getGameTime() + " :::: " + lastUpdateTime);
                                long diff = world.getGameTime() - lastUpdateTime;
                                System.out.println("Time difference: " + diff);
                                PlayerSurvivalMatrix survivalMatrix = playerData.getPlayerSurvivalMatrix();
                                survivalMatrix = survivalMatrix.toBuilder()
                                        .hydration(survivalMatrix.getHydration() - HYDRATION_DECAY_DELTA * diff)
                                        .playerNutrition(survivalMatrix.getPlayerNutrition().toBuilder()
                                                .fat(survivalMatrix.getPlayerNutrition().getFat() - FAT_DECAY_DELTA * diff)
                                                .protein(survivalMatrix.getPlayerNutrition().getProtein() - PROTEIN_DECAY_DELTA * diff)
                                                .carbohydrates(survivalMatrix.getPlayerNutrition().getCarbohydrates() - CARBS_DECAY_DELTA * diff)
                                                .build())
                                        .build();
                                System.out.println("survivalMatrix with Hydration: " + survivalMatrix.getHydration());
                                playerData = playerData.toBuilder()
                                        .lastMatrixUpdateTime(world.getGameTime())
                                        .playerSurvivalMatrix(survivalMatrix)
                                        .build();
                                System.out.println("After - Last Update Time Reading: " + world.getGameTime() + " :::: " + playerData.getLastMatrixUpdateTime());
                            }
                            System.out.println("PlayerData with Hydration: " + playerData.getPlayerSurvivalMatrix().getHydration());
                            System.out.println(playerData.toJson());
                            playerDataAccessor.savePlayerDataToCache(playerData);
                        }
                    }
//                    else {
//                        System.out.println(player + " is not in the world config.");
//                        player.sendMessage(ChatColor.RED + "You are not allowed in this world or have been removed from this world");
//                        Universe.teleportToLobby(player, playerDataAccessor, inventoryDataAccessor);
//                    }
                }
            }
        }, 0, 20);

    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }


    public void pauseMatrixChecking(){
        Bukkit.getScheduler().cancelTask(matrixRunID);
        this.gameStatus = GameStatus.OFFLINE;
    }
}
