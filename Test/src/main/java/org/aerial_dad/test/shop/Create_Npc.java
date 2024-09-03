package org.aerial_dad.test.shop;

import org.aerial_dad.test.Test;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import javax.annotation.Nonnull;
import java.util.List;

public class Create_Npc implements CommandExecutor, TabCompleter {

    private final Test plugin;

    public Create_Npc(Test plugin) {
        this.plugin = plugin;
    }

    private final static String key = "playercreated";

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command cmd, @Nonnull String s, @Nonnull String[] args) {
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
                ShopConfig.getInstance().setShopType(type);
                spawnNamedNPC(player, type, npcName);


            }

        }
        return true;
    }

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
        entity.setInvulnerable(true);



    }





    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        return null;
    }
}