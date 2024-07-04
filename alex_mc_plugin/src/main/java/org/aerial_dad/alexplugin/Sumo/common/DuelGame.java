package org.aerial_dad.alexplugin.Sumo.common;

import org.aerial_dad.alexplugin.Sumo.common.constants.GameConstants;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class DuelGame {
    public enum DuelGameState {
        CREATED,
        ONGOING,
        ENDED,
        PULSED,
        COUNTING_DOWN,
        TERMINATION_SCHEDULED

    }

    private DuelGameState  gameState;

//    private Player player1;
//    private Player player2;

    private final Player[] players = new Player[2];

    private final ItemStack replayActionItem = new ItemStack(Material.PLAYER_HEAD);

    private final ItemStack quitActionItem = new ItemStack(Material.WARPED_DOOR);

    private final Location[] spawnLocations = new Location[2];

    private final World world;

    private final Timer timer = new Timer();

    TimerTask terminationTask = new TimerTask() {
        @Override
        public void run() {
            terminate();
        }
    };

    public DuelGame(World world) {
        this.world = world;
        this.gameState = DuelGameState.CREATED;
        setActionItems();
        setSpawnLocations();
    }

    private void setSpawnLocations() {
        double x1 = getCenter().getX() + GameConstants.SPAWN_X_LOCATION_OFFSET;
        double x2 = getCenter().getX() - GameConstants.SPAWN_X_LOCATION_OFFSET;
        this.spawnLocations[0] = new Location(this.world, x1, getCenter().getY(), getCenter().getZ(), 90f, 0f);
        this.spawnLocations[1] = new Location(this.world, x2, getCenter().getY(), getCenter().getZ(), 270f, 0f);
    }

    private void setActionItems() {
        ItemMeta replayItemMeta = this.replayActionItem.getItemMeta();
        if(null == replayItemMeta) {
            System.out.println("ItemMeta is null. This is NOT expected!");
        } else {
            replayItemMeta.setDisplayName(GameConstants.GAME_ACTION_REPLAY_DISPLAY_NAME);
            this.replayActionItem.setItemMeta(replayItemMeta);
        }
        ItemMeta quitItemMeta = this.quitActionItem.getItemMeta();
        if(null == quitItemMeta) {
            System.out.println("ItemMeta is null. This is NOT expected!");
        } else {
            quitItemMeta.setDisplayName(GameConstants.GAME_ACTION_QUIT_DISPLAY_NAME);
            this.quitActionItem.setItemMeta(quitItemMeta);
        }
    }

//    public Player getPlayer1() {
//        return player1;
//    }
//
//    public Player getPlayer2() {
//        return player2;
//    }

    public Location getCenter() {
        return this.world.getSpawnLocation();
    }

    public DuelGameState getGameState() {
        return gameState;
    }

    public World getWorld() {
        return world;
    }

    public int admit(Player player) {
        if(this.gameState == DuelGameState.TERMINATION_SCHEDULED) return -1;
        for(int idx = 0; idx < this.players.length; ++idx) {
            if(this.players[idx] == null) {
                this.players[idx] = player;
                doAdmit(player);
                return idx;
            }
        }

        System.out.println("Duel is full, we are not allowing any more players to join.");
        return -1;
    }

    private void doAdmit(Player player) {
        System.out.println("Admitting player with UUID: " + player.getUniqueId());
        Global.playerToDuelGameMap.put(player.getUniqueId(), this);

        spawn(player);
        hideActionStackItems(player);
        player.setInvulnerable(true);
        if(isFull()) {
            start();
        }
    }

    public void spawn(Player player) {
        int index = getIndexOfPlayer(player);
        System.out.println("Spawning player index: " + index);
        player.teleport(this.spawnLocations[index]);
    }

    public boolean isFull() {
        for(Player player : this.players) {
            if(player == null) {
                return false;
            }
        }
        return true;
    }

    private void start() {
        if(!isFull()) {
            int missingPlayers = 0;
            for(Player player : this.players) {
                if(null == player) {
                    ++missingPlayers;
                }
            }
            System.err.println("The game needs all players to join before it can start. Still waiting for " + missingPlayers + " player(s).");
            return;
        }
        this.gameState = DuelGameState.COUNTING_DOWN;
        countDown(GameConstants.MILLIS_BEFORE_COUNT_DOWN, GameConstants.COUNT_DOWN_START_NUMBER);
        this.gameState = DuelGameState.ONGOING;

        for(Player player : this.players) {
            player.setInvulnerable(false);
            spawn(player);
            hideActionStackItems(player);
        }
    }

    private void countDown(final long waitMillis, final int countDownStartNumber) {
        try {
            Thread.sleep(waitMillis);
            int countDownNumber = countDownStartNumber;
            while(countDownNumber > 0) {
                for(Player player : this.players) {
                    player.sendTitle(Integer.toString(countDownNumber), "", 10, 70, 10);
                }
                Thread.sleep(1000);
                --countDownNumber;
            }
            for(Player player : this.players) {
                player.sendTitle(GameConstants.GAME_START_TITLE, "", 10, 70, 10);
            }
            Thread.sleep(1000);
            for(Player player : this.players) {
                player.resetTitle();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
    public void terminate() {
        this.gameState =DuelGameState.ENDED;
        for(Player player : this.players) {
            if(null != player) {
                player.teleport(Global.mainLobbyWorld.getSpawnLocation());
            }
        }
        Arrays.fill(this.players, null);
    }

    public void pause() {
        this.gameState = DuelGameState.PULSED;
        for(Player player : this.players) {
            player.setInvulnerable(true);
            spawn(player);
            showActionStackItems(player);
        }
    }

    public void defeat(Player loser) {
        loser.sendTitle(GameConstants.DEFEAT_TITLE_MESSAGE, GameConstants.DEFEAT_SUBTITLE_MESSAGE + loser.getDisplayName(), 10, 70, 10);

        Player winner = getTheOtherPlayer(loser);
        if(winner != null) {
            winner.sendTitle(GameConstants.VICTORY_TITLE_MESSAGE, GameConstants.VICTORY_SUBTITLE_MESSAGE + winner.getDisplayName(), 10, 70, 10);
        } else {
            System.err.println("There must be something wrong - the winner is null.");
        }

        pause();
        this.timer.schedule(this.terminationTask, GameConstants.TERMINATION_MILLIS);
        this.gameState = DuelGameState.TERMINATION_SCHEDULED;
    }

    private void showActionStackItems(Player player) {
        player.getInventory().setItem(GameConstants.REPLAY_ITEM_STACK_INDEX,
                this.replayActionItem);
        player.getInventory().setItem(GameConstants.QUIT_ITEM_STACK_INDEX,
                this.quitActionItem);
    }

    private void hideActionStackItems(Player player) {
        player.getInventory().remove(this.replayActionItem);
        player.getInventory().remove(this.quitActionItem);
    }

    public Player getTheOtherPlayer(@Nonnull Player thisPlayer) {
        if(!isFull()) {
            System.out.println("getTheOtherPlayer Failed since there is NO other player.");
            return null;
        }
        int index = getIndexOfPlayer(thisPlayer);
        return this.players[Math.abs(index - 1)];
    }

    public boolean isPlayerInGame(@Nonnull Player player) {
        int index = getIndexOfPlayer(player);
        return index == 0 || index == 1;
    }

    public int getIndexOfPlayer(@Nonnull Player player) {
        for(int idx = 0; idx < this.players.length; ++idx) {
            if(this.players[idx] != null && player.getUniqueId().equals(this.players[idx].getUniqueId())) {
                return idx;
            }
        }

        return -1;
    }

    public void removePlayer(@Nonnull Player player) {
        System.out.println("Player UUID: " + player.getUniqueId());

        int index = getIndexOfPlayer(player);
        if(index >= 0 && index < this.players.length) {
            this.players[index] = null;
        } else {
            System.err.println("Remove player failed since the player is not in this game: " + player.getDisplayName());
            return;
        }
        Global.playerToDuelGameMap.remove(player.getUniqueId());

    }

}
