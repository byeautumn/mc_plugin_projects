package org.aerial_dad.alexplugin.Sumo.commands;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;


import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class NpcCommand implements CommandExecutor, TabCompleter, Listener {

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event){
        Player player = event.getPlayer();
        Location fromLocation = event.getFrom();
        Location toLocation = event.getTo();
        if(null == toLocation) {
            System.out.println("The 'TO' location is null");
            return;
        }
        Vector playerVelocity = new Vector(toLocation.getX() - fromLocation.getX(),
                toLocation.getY() - fromLocation.getY(),
                toLocation.getZ() - fromLocation.getZ());
        float playerHeadMovmentPitch = toLocation.getPitch() - fromLocation.getPitch();

        float playerHeadMovmentYaw = toLocation.getYaw() - fromLocation.getYaw();
//        System.out.println("The code run here !!!!!!!!!!!!!!!!!!");
//        System.out.println(" Player velocity: " + playerVelocity);
//        Vector playerVelocity = player.getVelocity();
        List<Entity> entitiesnearby = player.getNearbyEntities(10, 10, 10 );

        for(Entity entity : entitiesnearby) {
            if (EntityType.PILLAGER == entity.getType()){
                entity.setVelocity(playerVelocity);
                entity.setRotation(playerHeadMovmentYaw, playerHeadMovmentPitch);
                System.out.println("Entity velocity: " + entity.getVelocity());
            }

        }

    }


    public static void spawnNamedNPC(Player player, String npcName) {
        Location spawnLocation = player.getLocation().add(5 ,0, 0 );
        Entity npc = player.getWorld().spawnEntity(spawnLocation, EntityType.PILLAGER);
        npc.setCustomName(npcName);
        player.sendMessage("Npc spawned at" + npc.getLocation());
        LivingEntity villager = (LivingEntity) npc;
        villager.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
        villager.setAI(false);
        villager.setSilent(true);



    }




    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command cmd, @Nonnull String s, @Nonnull String[] args) {
        if ((sender instanceof Player)){
            Player player = (Player) sender;

            if(args.length == 0){
                player.sendMessage(ChatColor.RED + "please provide a name for your spawned player");
            }else{
                String npcName = args[0];
                player.getVelocity();
                spawnNamedNPC(player, npcName);
            }

            return false;
        }







        return true;
    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender commandSender,@Nonnull Command command,@Nonnull String s,@Nonnull String[] strings) {
        return null;
    }
}
