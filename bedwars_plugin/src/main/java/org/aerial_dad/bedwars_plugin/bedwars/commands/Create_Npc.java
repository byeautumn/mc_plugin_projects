package org.aerial_dad.bedwars_plugin.bedwars.commands;

import org.aerial_dad.bedwars_plugin.Bedwars_plugin;
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
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import javax.annotation.Nonnull;

public class Create_Npc implements CommandExecutor {

    private final Bedwars_plugin plugin;

    public Create_Npc(Bedwars_plugin plugin) {
        this.plugin = plugin;
    }

    private final static String key = "playercreated";

    public void spawnNamedNPC(Player player, @Nonnull EntityType entityType, String npcName) {
        FixedMetadataValue metadataValue = new FixedMetadataValue(plugin, true);
        Location spawnLocation = player.getLocation().add(5, 0, 0);
        Entity npc = player.getWorld().spawnEntity(spawnLocation, entityType);
        npc.setCustomName(npcName);
        npc.setMetadata(key, metadataValue);
        player.sendMessage("Npc spawned at" + npc.getLocation());
        LivingEntity entity = (LivingEntity) npc;
        entity.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_INGOT));
        entity.setAI(false);
        entity.setSilent(true);



    }


    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command cmd, @Nonnull String s, @Nonnull String[] args) {
        if ((sender instanceof Player)) {
            Player player = (Player) sender;

            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Command is not complete.");
            } else { //Command Example "/spawnshop Villager Aerialdad14"
                player.sendMessage("PLUGIN HAS RUN HERE!!!!!!!!");
                String entityName = args[0];
                String npcName = args[1];
                EntityType entityType = null;
                for (EntityType type : EntityType.values()) {
                    if (type.name().equalsIgnoreCase(entityName)) {
                        entityType = type;
                        break;
                    }
                }
                System.out.println(entityType + "!!!!!!!!!!!!!");
                if(null != entityType) {
                    spawnNamedNPC(player, entityType, npcName);

                }else{
                    player.sendMessage(ChatColor.RED + entityName + " is not a valid entity.");

                }

            }

        }
        return true;
    }

}