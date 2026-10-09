package me.rerere.matrix.listeners;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.commands.DeveloperCommand;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.entity.TNTPrimed;

/** Implements Matrix's TNT placement and explosion behavior. */
public final class TntBehaviorListener implements Listener {
    private static final int FUSE_TICKS = 15;

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (DeveloperCommand.blockEventFlag) {
            event.blockList().clear();
        }
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        ServerVersion serverVersion = Matrix.Companion.serverVersion();
        boolean modernBlockData = serverVersion.isNewerThanOrEquals(ServerVersion.V_1_13);
        String blockDescription;
        if (modernBlockData) {
            Block clickedBlock = event.getClickedBlock();
            BlockData blockData = clickedBlock == null ? null : clickedBlock.getBlockData();
            blockDescription = blockData == null ? null : blockData.getAsString();
        } else {
            Block clickedBlock = event.getClickedBlock();
            MaterialData materialData = null;
            Block dataBlock = event.getClickedBlock();
            if (dataBlock != null) {
                BlockState state = dataBlock.getState();
                if (state != null) materialData = state.getData();
            }
            blockDescription = formatBlockData(clickedBlock, materialData);
        }

        Bukkit.broadcastMessage(blockDescription == null ? "null" : blockDescription);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!DeveloperCommand.blockEventFlag) {
            return;
        }

        Block placedBlock = event.getBlockPlaced();
        if (placedBlock.getType() != Material.TNT) {
            return;
        }
        event.getBlockPlaced().setType(Material.AIR);

        TNTPrimed primedTnt = event.getBlockPlaced().getWorld().spawn(
                event.getBlockPlaced().getLocation().clone().add(0.5, 0.0, 0.5), TNTPrimed.class);
        primedTnt.setFuseTicks(FUSE_TICKS);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!DeveloperCommand.blockEventFlag) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            event.setDamage(0.0);
        }
    }

    private static String formatBlockData(Block block, MaterialData materialData) {
        return new StringBuilder().append(block).append(" - ").append(materialData).toString();
    }
}
