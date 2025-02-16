package org.byeautumn.chuachua.block;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.byeautumn.chuachua.common.LocationUtil;
import org.byeautumn.chuachua.common.PlayMode;
import org.byeautumn.chuachua.player.PlayerStatus;
import org.byeautumn.chuachua.player.PlayerTracker;
import org.byeautumn.chuachua.player.PlayerUtil;
import org.byeautumn.chuachua.undo.ActionRecord;
import org.byeautumn.chuachua.undo.ActionRecorder;
import org.byeautumn.chuachua.undo.BlockProperties;
import org.byeautumn.chuachua.undo.BlockPropertiesRecord;

public class BlockListener implements Listener {

    private final static String PLAYER_PLACED_KEY = "playerPlaced";
    private final Chuachua plugin;

    public BlockListener(Chuachua plugin) {
        this.plugin = plugin;
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event){
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();
        PlayerTracker playerTracker = Universe.getPlayerTracker(player);

        if (playerTracker.getStatus() != PlayerStatus.InGame) {
            ActionRecorder recorder = Universe.getActionRecorder(player);
//            System.out.println("Poly selection: " + recorder.isPolySelection());
            if (recorder.isPolySelection() && block.getType() == ActionRecorder.POLY_SELECT_TYPE) {
                System.out.println("Place poly selection at " + LocationUtil.printBlock(block));
                player.sendMessage(ChatColor.LIGHT_PURPLE + LocationUtil.printBlock(block) + recorder.getPolySelectedBlocks().size());
                recorder.polySelect(block);
                PlayerUtil.sendObjectToMainHand(player, ActionRecorder.POLY_SELECT_TYPE);
                System.out.println("Current poly selection size: " + recorder.getPolySelectedBlocks().size());
            }
            else if (recorder.isDiaSelection() && block.getType() == ActionRecorder.DIA_SELECT_TYPE) {
                System.out.println("Place dia selection at " + LocationUtil.printBlock(block));
                player.sendMessage(ChatColor.LIGHT_PURPLE + LocationUtil.printBlock(block) + recorder.getDiaSelectedBlocks().size());
                recorder.diaSelect(block);
                PlayerUtil.sendObjectToMainHand(player, ActionRecorder.DIA_SELECT_TYPE);
                System.out.println("Current dia selection size: " + recorder.getDiaSelectedBlocks().size());
            }
            else {
                ActionRecord action = new BlockPropertiesRecord(block, new BlockProperties(Material.AIR), new BlockProperties(block.getType()));
                recorder.record(action);
            }
        }
        else {
            World currentWorld = block.getWorld();
            Universe.markPlayerPlacedBlock(currentWorld, block);

            FixedMetadataValue metadataValue = new FixedMetadataValue(plugin, true);

            block.setMetadata(PLAYER_PLACED_KEY, metadataValue);
            System.out.println("MetaData has been set to " + block.getType());
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        PlayerTracker playerTracker = Universe.getPlayerTracker(player);

        if (playerTracker.getStatus() != PlayerStatus.InGame) {
            if (playerTracker.getPlayMode() != PlayMode.EDIT) {
                player.sendMessage(ChatColor.RED + "You cannot break this block " + block.getType());
                System.out.println("Player " + player.getDisplayName() + " is not in a game so the block cannot either be mined or broken: " + block.getType());
                event.setCancelled(true);
                return;
            }
            ActionRecorder recorder = Universe.getActionRecorder(player);
            if (recorder.isPolySelection() && block.getType() == ActionRecorder.POLY_SELECT_TYPE) {
                if (Universe.areLocationsIdentical(block.getLocation(), recorder.getLastPolySelection().getLocation())) {
                    recorder.cancelLastPolySelection();
                }
                else {
                    player.sendMessage("You cannot break the poly selection other than the last one.");
                    event.setCancelled(true);
                }
            }
            else if (recorder.isDiaSelection() && block.getType() == ActionRecorder.DIA_SELECT_TYPE) {
                if (Universe.areLocationsIdentical(block.getLocation(), recorder.getLastDiaSelection().getLocation())) {
                    recorder.cancelLastDiaSelection();
                }
                else {
                    player.sendMessage("You cannot break the dia selection other than the last one.");
                    event.setCancelled(true);
                }
            }
            else {
                ActionRecord action = new BlockPropertiesRecord(block, new BlockProperties(block.getType()), new BlockProperties(Material.AIR));
                recorder.record(action);
            }

        }
        else {
            if (!block.hasMetadata(PLAYER_PLACED_KEY)) {
                Location blockLocation = block.getLocation();
                World world = blockLocation.getWorld();
                if (null != world) {
                    world.dropItem(blockLocation, new ItemStack(block.getType(), 1));
                }
                player.sendMessage(ChatColor.RED + "You have just mined " + block.getType());
                System.out.println("This block is NOT player-placed: " + block.getType());
                event.setCancelled(true);
            }
        }
    }
}
