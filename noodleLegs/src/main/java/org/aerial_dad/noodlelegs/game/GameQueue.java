package org.aerial_dad.noodlelegs.game;

import org.aerial_dad.noodlelegs.Universe;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import sun.awt.image.ImageWatched;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class GameQueue {

    private Queue<Player> queue;

    private final Location queueLocation;


    public GameQueue(Location queueLocation){
        this.queueLocation = queueLocation;
    }

    public void addPlayer(Player player){
        if(this.queue == null){
            this.queue = new LinkedList<>();
        }
        this.queue.add(player);
        Universe.teleport(player, this.queueLocation);

        updatePlayerTracker(player, PlayerStatus.InQueue);
    }

    public int getPlayerCountInQueue(){
        if(this.queue == null) {
            return 0;
        }
        return this.queue.size();
    }

    public Player pollPlayer(){
        if (this.queue.isEmpty()){
            System.err.println("Cannot poll players since game queue is empty.");
            return null;
        }
        return this.queue.poll();
    }

    public void removePlayer(Player player) {
        getQueue().remove(player);
        updatePlayerTracker(player, PlayerStatus.Unknown);
    }

    private Queue<Player> getQueue() {
        return this.queue;
    }

    private void updatePlayerTracker(Player player, PlayerStatus playerStatus) {
        // Once player is in the queue, it is illegal to change player status to InGame by the GameQueue instance.
        if (playerStatus == PlayerStatus.InGame) {
            System.out.println("Update player tracker to an illegal status: " + playerStatus.name());
            return;
        }
        PlayerTracker playerTracker = Universe.getPlayerTracker(player);
        if(playerStatus == PlayerStatus.InQueue) {
            playerTracker.update(playerStatus,this);
        } else if(playerStatus == PlayerStatus.Unknown) {
            playerTracker.update(playerStatus);
        }
    }
}
