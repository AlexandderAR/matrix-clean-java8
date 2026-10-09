package me.rerere.matrix.checks.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.entity.Player;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.network.packet.PacketEventSupport;

import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.MaterialPolicy;


/** Checks suspicious block placements associated with packet-driven air placement. */
public class BlockPlacementChecks extends CheckRuntimeState {
    private static boolean checkAirPlace;
    private static final BlockFace[] adjacentFaces;
    private boolean packetPending;

    static {
        adjacentFaces = new BlockFace[] {
                BlockFace.DOWN, BlockFace.UP, BlockFace.SOUTH,
                BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST
        };
    }

    public BlockPlacementChecks(PlayerCheckContext context) {
        super(context, CheckCategory.BADPACKETS);
        packetPending = false;
    }

    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event, PacketWrapper wrapper) {
        if (checkAirPlace && PacketEventSupport.isBlockPlacementPacket(packetType)) {
            packetPending = true;
        }
        return false;
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        if (!packetPending) return;
        packetPending = false;

        Block placed = event.getBlockPlaced();
        if (!event.getBlockAgainst().equals(placed)) return;
        if (event.getClass() != BlockPlaceEvent.class) return;
        if (!canPlaceMaterial(placed.getType())) return;

        BlockState replacedState = event.getBlockReplacedState();
        Material replacedType = replacedState.getType();
        if (replacedType == null) replacedType = Material.AIR;
        Material placedMaterial = BlockEnvironment.getMaterial(placed);
        if (MaterialPolicy.containsSlabOrStep(placedMaterial)
                && MaterialPolicy.containsSlabOrStep(replacedType)) {
            return;
        }

        for (BlockFace face : adjacentFaces) {
            Block neighbor = placed.getRelative(face);
            if (!MaterialPolicy.isAirMaterial(BlockEnvironment.getMaterial(neighbor))) return;
        }

        event.setCancelled(true);
        String reason = formatReason(placed.getType().name());
        this.flag("bp.pe", reason, 0);
    }

    private boolean canPlaceMaterial(Material materialToPlace) {
        if (Matrix.getServerVersion() == ServerVersion.V_1_8_8) {
            return this.player().getItemInHand().getType() == materialToPlace;
        }
        Player player = this.player();
        PlayerInventory inventory = player.getInventory();
        ItemStack mainHand = inventory.getItemInMainHand();
        ItemStack offHand = inventory.getItemInOffHand();
        return mainHand.getType() == materialToPlace || offHand.getType() == materialToPlace;
    }

    public void reloadConfiguration() {
        ConfigurationSection config = this.configuration();
        checkAirPlace = config.getBoolean("check_air_place", true);
    }

    private static String formatReason(String materialName) {
        return "place block without against other block / " + materialName;
    }
}



