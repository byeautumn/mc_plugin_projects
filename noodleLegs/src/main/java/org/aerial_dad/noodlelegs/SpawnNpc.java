package org.aerial_dad.noodlelegs;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.permissions.Permission;

import java.lang.reflect.Field;

public class SpawnNpc implements CommandExecutor {

    public SpawnNpc() {
    }

    private final static String key = "shop";

    protected NoodleLegs getPlugin() {
        return NoodleLegs.getInstance;
    }

    @Override
    public boolean onCommand( CommandSender sender,  Command cmd,  String s, String[] args) {
        if ((sender instanceof Player)) {
            Player player = (Player) sender;

            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Command is not complete.");
            } else { //Command Example "/spawnshop Villager Aerialdad14"
                player.sendMessage("PLUGIN HAS RUN HERE!!!!!!!!");
                String npcName = args[1];
                EntityType type;

                try {
                    type = EntityType.valueOf(args[0].toUpperCase());

                }catch(IllegalArgumentException exception){
                    player.sendMessage("invalid Entity " + args[0]);

                    return true;

                }
                if (!player.isOp()){
                    System.out.println("'" +player + "' does not have perms to spawn a shop");
                }else {
                    spawnNamedNPC(type, npcName, player.getLocation());
                    player.sendMessage("Npc spawned at" + player.getLocation());
                }
            }

        }
        return true;
    }

    public void spawnNamedNPC(EntityType entityType, String npcName, Location shopSpawnLocation) {
        FixedMetadataValue metadataValue = new FixedMetadataValue(getPlugin(), true);
        Entity npc = shopSpawnLocation.getWorld().spawnEntity(shopSpawnLocation, entityType);
        npc.setCustomName(npcName);
        npc.setMetadata(key, metadataValue);
        System.out.println(" Npc has meta data of '" + key + "'.");
        LivingEntity entity = (LivingEntity) npc;
        entity.setHealth(20);


    }
}
