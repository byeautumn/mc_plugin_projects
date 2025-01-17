package org.byeautumn.chuachua.block;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.byeautumn.chuachua.Chuachua;
import org.byeautumn.chuachua.Universe;
import org.byeautumn.chuachua.player.PlayerStatus;
import org.byeautumn.chuachua.player.PlayerTracker;

public class BlockListener implements Listener {

    private final static String PLAYER_PLACED_KEY = "playerPlaced";
    private final Chuachua plugin;

    public BlockListener(Chuachua plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){

        Block block = event.getBlockPlaced();  //NBTTagCompound blockNBT =
        World currentWorld = block.getWorld();
        Universe.markPlayerPlacedBlock(currentWorld, block);

        FixedMetadataValue metadataValue = new FixedMetadataValue(plugin, true);

        block.setMetadata(PLAYER_PLACED_KEY, metadataValue);
        System.out.println("MetaData has been set to " + block.getType());

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!block.hasMetadata(PLAYER_PLACED_KEY)) {
            PlayerTracker playerTracker = Universe.getPlayerTracker(player);
            if (playerTracker.getStatus() == PlayerStatus.InGame) {
                Location blockLocation = block.getLocation();
                World world = blockLocation.getWorld();
                if (null != world) {
                    world.dropItem(blockLocation, new ItemStack(block.getType(), 1));
                }
                player.sendMessage(ChatColor.RED + "You have just mined " + block.getType());
                System.out.println("This block is NOT player-placed: " + block.getType());
            } else {
                player.sendMessage(ChatColor.RED + "You cannot break this block " + block.getType());
                System.out.println("Player " + player.getDisplayName() + " is not in a game so the block cannot either be mined or broken: " + block.getType());
            }

            event.setCancelled(true);
        }

    }
}
