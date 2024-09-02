package org.aerial_dad.bedwars_plugin.bedwars.game.Teams;

import org.aerial_dad.bedwars_plugin.bedwars.commands.Bw_general;
import org.aerial_dad.bedwars_plugin.bedwars.game.BwPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TeamManager {


    public List<BwTeam> buildTeams(Queue<BwPlayer> playerQueue, int teamSize, int teamCount) {
        if (checkEnoughPlayers(playerQueue, teamSize, teamCount) == false ){
            return null;
        }

        // Create and initialize a list to return - List<BwTeam> = new

        List<BwTeam> bwTeams = new ArrayList<>();

        // make a loop based on teamCount
        for(int idx = 0; idx < teamCount; ++idx) {
            BwTeam team = new BwTeam();
            for (int idx1 = 0; idx1 < teamSize; ++idx1){
                BwPlayer player = playerQueue.poll();
                team.addPlayer(player);
            }
            bwTeams.add(team);
        }
        // Return the list
        return bwTeams;
    }

    public static boolean checkEnoughPlayers(Queue<BwPlayer> playerQueue, int teamSize, int teamCount){
        if (playerQueue.size() == teamSize * teamCount){
            return true;
        }else{
            return false;
        }
    }


    public static void main(String[] args) {
        Queue<BwPlayer> playerQueue = new ConcurrentLinkedQueue<>();
        for (int idx = 0; idx < 8; ++idx){
            BwPlayer player = new BwPlayer("p" + (idx + 1), null);
            playerQueue.add(player);
        }


        TeamManager tm = new TeamManager();
        List<BwTeam> teams = tm.buildTeams(playerQueue, 2, 2);

        for(BwTeam team : teams) {
            System.out.println("Team: " + team.printSelf());
        }

        List<BwTeam> teams2 = tm.buildTeams(playerQueue, 2, 2);
        for(BwTeam team : teams2) {
            System.out.println("Team: " + team.printSelf());
        }

        System.out.println("Players still in queue: " + playerQueue.size());
    }
}
