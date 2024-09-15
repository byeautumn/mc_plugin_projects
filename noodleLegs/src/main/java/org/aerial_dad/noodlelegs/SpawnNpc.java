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

import java.lang.reflect.Field;

public class SpawnNpc implements CommandExecutor {

    private final NoodleLegs plugin;

    public SpawnNpc(NoodleLegs plugin) {
        this.plugin = plugin;
    }

    private final static String key = "lobbynpc";

    @Override
    public boolean onCommand( CommandSender sender,  Command cmd,  String s, String[] args) {
        System.out.println("HELLOOOOOOOOO______________");
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
                spawnNamedNPC(player, type, npcName);


            }

        }
        return true;
    }

    public void spawnNamedNPC(Player player, EntityType entityType, String npcName) {
        FixedMetadataValue metadataValue = new FixedMetadataValue(plugin, true);
        Location spawnLocation = player.getLocation().add(5, 0, 0);
        Entity npc = player.getWorld().spawnEntity(spawnLocation, entityType);
        npc.setCustomName(npcName);
        npc.setMetadata(key, metadataValue);
        player.sendMessage("Npc spawned at" + npc.getLocation());
        LivingEntity entity = (LivingEntity) npc;
        entity.setHealth(20);



    }
}
