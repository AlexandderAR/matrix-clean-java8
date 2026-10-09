package me.rerere.matrix.checks.interaction;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import me.rerere.matrix.geometry.BlockShapeMode;

/** Checks whether a right-click ray reaches the clicked block through the reported shapes. */
public final class BlockInteractionVisibilityCheck extends me.rerere.matrix.checks.CheckRuntimeState {
    private static final Set<Material> NON_LEGACY_MATERIALS = Arrays.stream(Material.values())
            .filter(BlockInteractionVisibilityCheck::isNonLegacyMaterial)
            .collect(Collectors.toSet());

    public static boolean scale = true;
    private static boolean enabled;
    private static String mode;
    private me.rerere.matrix.checks.player.BlockInteractionSnapshot currentInteraction;
    private boolean cancelNextInteract;
    private double visibilityBuffer;

    public BlockInteractionVisibilityCheck(me.rerere.matrix.player.PlayerCheckContext playerData) {
        super(playerData);
        visibilityBuffer = 0.0D;
        super.setCheckType(me.rerere.matrix.checks.CheckCategory.INTERACT);
    }

    /** Periodic callback ages a pending click and inspects it after two ticks. */
    public void onTick() {
        me.rerere.matrix.checks.player.BlockInteractionSnapshot interaction = currentInteraction;
        if (interaction == null) return;
        interaction.clickSequence++;
        if (interaction.clickSequence <= 2) return;
        interaction.secondLocation = interaction.firstLocation;
        try {
            inspectInteraction(interaction);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        currentInteraction = null;
    }

    /** Player interaction callback, including optional debug shape dumps. */
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!enabled || !mode.equals("v1")) return;
        if (cancelNextInteract) {
            cancelNextInteract = false;
            event.setCancelled(true);
        }
        Block clicked = event.getClickedBlock();
        if (clicked != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            captureInteraction(event.getPlayer(), clicked, event);
        }
        if (!me.rerere.matrix.commands.DeveloperCommand.debugInteraction || clicked == null) return;

        me.rerere.matrix.geometry.AxisAlignedBox[] collisionBoxes = me.rerere.matrix.world.BlockCollisionGeometry.getBlockShapes(
                clicked, BlockShapeMode.COLLISION);
        me.rerere.matrix.geometry.AxisAlignedBox[] interactionBoxes = me.rerere.matrix.world.BlockCollisionGeometry.getBlockShapes(
                clicked, BlockShapeMode.INTERACT);
        Bukkit.broadcastMessage(shapeDetails(
                clicked.getType().name().toLowerCase(Locale.ROOT), clicked.getData()));
        for (me.rerere.matrix.geometry.AxisAlignedBox box : collisionBoxes) {
            Bukkit.broadcastMessage(collisionBoxMessage(box.toString()));
        }
        for (me.rerere.matrix.geometry.AxisAlignedBox box : interactionBoxes) {
            Bukkit.broadcastMessage(blockBoundsMessage(box.toString()));
        }
        if (me.rerere.matrix.Matrix.getServerVersion().isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_17)) {
            Bukkit.broadcastMessage(boundingBoxMessage(clicked.getBoundingBox().toString()));
        }
    }

    private void inspectInteraction(
            me.rerere.matrix.checks.player.BlockInteractionSnapshot interaction) {
        if (me.rerere.matrix.Matrix.getServerVersion().isNewerThanOrEquals(
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_14)
                && interaction.material == Material.SCAFFOLDING) return;
        Player player = interaction.player;
        if (player.isInsideVehicle()) return;
        double eyeHeight = me.rerere.matrix.util.BlockRayTraceHelper.eyeHeight(player,
                (me.rerere.matrix.player.PlayerCheckContext) playerState());
        Location eyeLocation = player.getEyeLocation();
        Block clicked = interaction.block;
        if (me.rerere.matrix.util.GeometryMath.horizontalManhattanDistance(
                eyeLocation, clicked.getLocation()) < 3.0D) return;

        me.rerere.matrix.geometry.AxisAlignedBox clickedBox = me.rerere.matrix.world.BlockCollisionGeometry.defaultBlockBounds(clicked);
        clickedBox.setMaxY(clickedBox.maxY() + 0.5D);
        clickedBox.grow(0.1D, 0.1D, 0.1D);
        me.rerere.matrix.geometry.Ray ray = new me.rerere.matrix.geometry.Ray(
                interaction.secondLocation.toVector().add(new Vector(0.0D, eyeHeight, 0.0D)),
                interaction.secondLocation.getDirection());
        boolean obstructionFound = false;
        Vector clickedBlockIntersection = clickedBox.clipSegmentAgainstBox(ray, 0.0F, 8.0F);
        if (clickedBlockIntersection == null) {
            obstructionFound = true;
            visibilityBuffer += 1.0D;
            if (visibilityBuffer >= 2.0D && scale) {
                cancelNextInteract = true;
                report("interact.dir", String.format(
                        "interacting with a block without looking at it (block: %s, buf: %s)",
                        interaction.material.name().toLowerCase(Locale.ROOT),
                        me.rerere.matrix.util.NumberUtils.round3(visibilityBuffer)), 0);
            }
        } else {
            me.rerere.matrix.world.WorldAccess worldAccess = me.rerere.matrix.Matrix.getInstance()
                    .getNmsManager().visitor();
            Block adjacentBlock = interaction.interactionEvent.getClickedBlock()
                    .getRelative(interaction.interactionEvent.getBlockFace());
            me.rerere.matrix.geometry.AxisAlignedBox playerBounds = new me.rerere.matrix.geometry.AxisAlignedBox(
                    player.getLocation().toVector(), 0.3F,
                    me.rerere.matrix.world.CollisionGeometry.getStandingHeight(player));
            List<Block> nearbyBlocks = me.rerere.matrix.util.BlockRayTraceHelper.collectBlocks(
                    interaction.secondLocation, eyeHeight, NON_LEGACY_MATERIALS, 7);
            for (Block candidate : nearbyBlocks) {
                me.rerere.matrix.geometry.AxisAlignedBox[] interactionShapes =
                        me.rerere.matrix.world.BlockCollisionGeometry.getBlockShapes(
                                candidate, BlockShapeMode.INTERACT);
                if (candidate.equals(clicked)) break;
                if (playerBounds.intersectsAny(interactionShapes)
                        || candidate.isLiquid() || candidate.equals(adjacentBlock)) continue;
                float breakSpeed = me.rerere.matrix.Matrix.getInstance()
                        .getNmsManager().visitor().getBlockBreakSpeed(
                        player, candidate);
                if ((int) (1.0F / breakSpeed) <= 2) continue;
                for (me.rerere.matrix.geometry.AxisAlignedBox interactionShape : interactionShapes) {
                    if (interactionShape.clipSegmentAgainstBox(ray, 0.0F, 7.0F) == null) continue;
                    obstructionFound = true;
                    visibilityBuffer += candidate.getLocation().distance(clicked.getLocation());
                    if (visibilityBuffer >= 2.0D && scale) {
                        cancelNextInteract = true;
                        report("interact.vis", String.format(
                                "interact with an invisible block (block: %s, pass: %s, buf: %s)",
                        interaction.material.name().toLowerCase(Locale.ROOT),
                                candidate.getType().name().toLowerCase(Locale.ROOT),
                                me.rerere.matrix.util.NumberUtils.round3(visibilityBuffer)), 0);
                    }
                    break;
                }
            }
        }
        if (!obstructionFound) {
            if (visibilityBuffer > 5.0D) visibilityBuffer /= 2.0D;
            visibilityBuffer = Math.max(0.0D, visibilityBuffer - 0.5D);
        }
    }

    private void captureInteraction(Player player, Block block, PlayerInteractEvent event) {
        currentInteraction = new me.rerere.matrix.checks.player.BlockInteractionSnapshot(player, block, block.getType(),
                player.getLocation().clone(), null, event, 0);
    }

    public static boolean isNonLegacyMaterial(Material material) {
        return !material.name().startsWith("LEGACY_");
    }

    private static String shapeDetails(String materialName, byte data) {
        return "Shapes of " + materialName + "(0x" + data + "):";
    }
    private static String collisionBoxMessage(String box) {
        return "# Collision: " + box;
    }
    private static String blockBoundsMessage(Object bounds) {
        return "# Bounding: " + bounds;
    }
    private static String boundingBoxMessage(Object bounds) {
        return "Bounding: " + bounds;
    }

    public void reloadConfiguration() {
        enabled = configuration().getBoolean("check_interact_block", true);
        mode = configuration().getString("mode", "v1");
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        me.rerere.matrix.checks.player.BlockInteractionSnapshot interaction = currentInteraction;
        if (interaction == null) return;
        Location updated = from.clone();
        updated.setYaw(to.getYaw());
        updated.setPitch(to.getPitch());
        interaction.secondLocation = updated;
        try {
            inspectInteraction(interaction);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        currentInteraction = null;
    }

    private boolean report(String key, String message, int severity) {
        return super.flag(key, message, severity);
    }
}
