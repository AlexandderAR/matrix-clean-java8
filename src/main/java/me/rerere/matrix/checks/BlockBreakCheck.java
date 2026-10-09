package me.rerere.matrix.checks;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.DiggingAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.compat.bukkit.EfficiencyEnchantment;
import me.rerere.matrix.world.BlockCollisionGeometry;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.util.BlockRayTraceHelper;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.util.BukkitPlayerAccess;
import me.rerere.matrix.player.PlayerCheckContext;

/** Detects block breaking outside the player's observed line of sight. */
public final class BlockBreakCheck extends CheckRuntimeState {
    public static boolean timeout;
    private static final int BLOCK_SCAN_LIMIT = 7;
    private static final long COOLDOWN_MILLIS = 500L;
    private long lastFinishedDigging;
    private static boolean checkBreakBlock;
    private static final Set<Material> nonLegacyMaterials;
    private static String mode;

    public BlockBreakCheck(PlayerCheckContext config) {
        super(config);
        super.setCheckType(CheckCategory.INTERACT);
    }

    public boolean onPacketReceive(PacketTypeCommon type, PacketReceiveEvent event,
                                   PacketWrapper<?> wrapper) {
        if (type == PacketType$Play$Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging digging = (WrapperPlayClientPlayerDigging) wrapper;
            if (digging.getAction() == DiggingAction.FINISHED_DIGGING) {
                lastFinishedDigging = System.currentTimeMillis();
            }
        }
        return false;
    }

    public void reloadConfiguration() {
        me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection section =
                super.configuration();
        checkBreakBlock = section.getBoolean("check_break_block", true);
        mode = super.configuration().getString("mode", "v1");
    }

    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getClass() != BlockBreakEvent.class) return;
        if (!checkBreakBlock) return;
        if (!mode.equals("v1")) return;
        org.bukkit.block.Block brokenBlock = event.getBlock();
        if (brokenBlock.getType() == Material.AIR) return;
        org.bukkit.entity.Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) return;
        if (!MaterialPolicy.containsBedBridge(brokenBlock.getType())) return;
        if (player.hasMetadata("EVENT_STOP")) return;

        boolean suspiciousNeighbor = false;
        boolean foundBreakingBlock = false;
        Material suspiciousType = Material.AIR;
        org.bukkit.Location playerLocation = player.getLocation();
        double distance = BlockRayTraceHelper.eyeHeight(player, super.playerState());
        List<org.bukkit.block.Block> candidates = BlockRayTraceHelper.collectBlocks(
                playerLocation, distance, nonLegacyMaterials, BLOCK_SCAN_LIMIT);

        for (org.bukkit.block.Block candidate : candidates) {
            if (candidate.equals(brokenBlock)) {
                foundBreakingBlock = true;
                break;
            }
            AxisAlignedBox[] rays = BlockCollisionGeometry.getWorldShapes(
                    candidate, BlockShapeMode.INTERACT);
            if (rays.length == 0 || rays.length > 1) continue;

            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion serverVersion =
                    Matrix.getServerVersion();
            Material candidateType = candidate.getType();
            if (serverVersion.isNewerThanOrEquals(
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_14)
                    && candidateType == Material.SCAFFOLDING) continue;
            if (candidateType == Material.SAND) continue;
            if (Matrix.getServerVersion().isOlderThan(
                    me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_13)
                    && MaterialPolicy.containsSupport(candidateType)) continue;
            if (candidateType == Material.FIRE) continue;

            if (MaterialPolicy.isAirMaterial(candidateType)
                    || candidate.isLiquid()
                    || !BlockCollisionGeometry.isUnitCube(rays)) {
                continue;
            }

            boolean adjacentLiquid = false;
            if (candidateType == Material.COBBLESTONE || candidateType == Material.STONE) {
                org.bukkit.block.BlockFace[] faces = BlockEnvironment.HORIZONTAL_AND_DIAGONAL_FACES;
                for (org.bukkit.block.BlockFace face : faces) {
                    if (candidate.getRelative(face).isLiquid()) {
                        adjacentLiquid = true;
                        break;
                    }
                }
                if (candidate.getRelative(org.bukkit.block.BlockFace.UP).isLiquid()
                        || candidate.getRelative(org.bukkit.block.BlockFace.DOWN).isLiquid()) {
                    adjacentLiquid = true;
                }
            }
            if (adjacentLiquid) continue;
            suspiciousNeighbor = true;
            suspiciousType = candidateType;
        }

        org.bukkit.inventory.ItemStack held = player.getItemInHand();
        boolean ignoreBecauseOfTool = brokenBlock.getType() == Material.NETHERRACK
                && held != null && held.getType() != Material.AIR
                && held.getType().name().endsWith("PICKAXE");
        if (held != null && held.hasItemMeta()) {
            org.bukkit.enchantments.Enchantment efficiency = EfficiencyEnchantment.INSTANCE.getEfficiency();
            if (held.getItemMeta().hasEnchant(efficiency)
                    && held.getEnchantmentLevel(efficiency) > 3) {
                ignoreBecauseOfTool = true;
            }
        }
        if (suspiciousType == Material.HOPPER) ignoreBecauseOfTool = true;
        if (System.currentTimeMillis() - lastFinishedDigging > COOLDOWN_MILLIS) {
            ignoreBecauseOfTool = true;
        }
        if (player.isInsideVehicle()) ignoreBecauseOfTool = true;

        if (!foundBreakingBlock && !ignoreBecauseOfTool
                && !BukkitPlayerAccess.isExternalEventCall() && timeout) {
            String detail = formatUnexpectedBlock(String.valueOf(brokenBlock.getType()));
            if (super.flag("interact.break", detail, 0)) {
                event.setCancelled(true);
            }
        }

        if (suspiciousNeighbor && !ignoreBecauseOfTool
                && !BukkitPlayerAccess.isExternalEventCall() && timeout) {
            Material brokenType = brokenBlock.getType();
            if (suspiciousType == brokenType
                    && (suspiciousType == Material.STONE || suspiciousType == Material.COBBLESTONE)) {
                return;
            }
            String detail = formatThroughWalls(
                    suspiciousType.name().toLowerCase(), brokenType.name().toLowerCase());
            if (super.flag("interact.break", detail, 0)) {
                event.setCancelled(true);
            }
        }
    }

    private static boolean isNonLegacy(Material material) {
        return !material.name().startsWith("LEGACY_");
    }

    private static String formatUnexpectedBlock(String block) {
        return "breaking a block without looking at it (block: " + block + ")";
    }

    private static String formatThroughWalls(String through, String block) {
        return "break block through walls (through: " + through + ", block: " + block + ")";
    }

    static {
        nonLegacyMaterials = Arrays.stream(Material.values())
                .filter(BlockBreakCheck::isNonLegacy)
                .collect(Collectors.toSet());
    }
}
