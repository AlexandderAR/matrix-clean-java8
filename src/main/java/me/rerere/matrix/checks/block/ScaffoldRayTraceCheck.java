package me.rerere.matrix.checks.block;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3i;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction$Action;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.movement.PlacementMovementSnapshot;
import me.rerere.matrix.geometry.Ray;
import me.rerere.matrix.util.BlockRayTraceHelper;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.BlockCollisionGeometry;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.commands.DeveloperCommand;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.compat.FoliaSupport;

/** Compares the reported block-placement cursor with two movement ray traces. */
public class ScaffoldRayTraceCheck extends CheckRuntimeState {
    public static boolean enforcementEnabled;
    private static int violationWeight;
    private static boolean enabled;
    private int suspiciousPlacements;
    private final AtomicReference<PlacementMovementSnapshot> pendingPlacement;
    private final AtomicLong worldGeneration;
    private boolean cancelNextPlacement;
    private boolean sneaking;

    public ScaffoldRayTraceCheck(PlayerCheckContext context) {
        super(context);
        pendingPlacement = new AtomicReference<>();
        worldGeneration = new AtomicLong();
        cancelNextPlacement = false;
        sneaking = false;
        setCheckType(CheckCategory.SCAFFOLD);
    }

    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        worldGeneration.incrementAndGet();
        pendingPlacement.set(null);
    }

    public void onTick() {
        PlacementMovementSnapshot placement = pendingPlacement.get();
        if (placement == null || placement.getRayTraceLocation() == null
                || !pendingPlacement.compareAndSet(placement, null)) {
            return;
        }
        try {
            processPlacement(placement);
        } catch (Exception failure) {
            Matrix.getInstance().getLogger().log(Level.SEVERE, "Error while raytrace", failure);
        }
    }

    public void reloadConfiguration() {
        enabled = configuration().getBoolean("modules.place.enable", true);
        violationWeight = configuration().getInt("modules.place.vl_weight", 5);
    }

    public void onPlayerMoveWithFlags(PlayerMoveEvent event, Location from, Location to,
            boolean firstFlag, boolean secondFlag) {
        PlacementMovementSnapshot pending = pendingPlacement.get();
        if (pending != null && pending.getRayTraceLocation() == null) {
            Location moved = from.clone();
            moved.setYaw(to.getYaw());
            moved.setPitch(to.getPitch());
            pending.rayTraceLocation = moved;
        }
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        if (cancelNextPlacement) {
            cancelNextPlacement = false;
            event.setCancelled(true);
        }
    }

    public boolean onPacketReceive(PacketTypeCommon type, PacketReceiveEvent event, PacketWrapper<?> wrapper) {
        if (type == PacketType$Play$Client.ENTITY_ACTION) {
            WrapperPlayClientEntityAction$Action action = PacketEventSupport.getEntityAction(wrapper);
            if (action == WrapperPlayClientEntityAction$Action.START_SNEAKING) {
                sneaking = true;
            }
            if (action == WrapperPlayClientEntityAction$Action.STOP_SNEAKING) {
                sneaking = false;
            }
        }
        if (PacketEventSupport.isBlockPlacementPacket(type)) {
            WrapperPlayClientPlayerBlockPlacement packet = (WrapperPlayClientPlayerBlockPlacement) wrapper;
            float cursorX = packet.getCursorPosition().x;
            float cursorY = packet.getCursorPosition().y;
            float cursorZ = packet.getCursorPosition().z;
            Vector3i blockPosition = packet.getBlockPosition();
            Vector reportedHitPosition = new Vector(
                    (float) blockPosition.getX() + cursorX,
                    (float) blockPosition.getY() + cursorY,
                    (float) blockPosition.getZ() + cursorZ);
            boolean sideFace = packet.getFace().getModY() == 0;
            if (sideFace && blockPosition.getY() != -1 && enabled) {
                Player player = player();
                long packetWorldGeneration = worldGeneration.get();
                Location playerLocation = player.getLocation().clone();
                if (packetWorldGeneration != worldGeneration.get()) {
                    return false;
                }
                pendingPlacement.set(new PlacementMovementSnapshot(player,
                        blockPosition.getX(), blockPosition.getY(), blockPosition.getZ(),
                        playerLocation, null, reportedHitPosition, sneaking, packetWorldGeneration));
            }
        }
        return false;
    }

    private void processPlacement(PlacementMovementSnapshot placement) {
        if (placement.getTimestamp() != worldGeneration.get()
                || placement.getPlayerLocation().toVector().distanceSquared(placement.getVelocity()) > 100.0) {
            return;
        }
        World world = player().getWorld();
        Location blockLocation = new Location(world, placement.getBlockX(),
                placement.getBlockY(), placement.getBlockZ());
        if (!FoliaSupport.isLocationOwnedByCurrentRegion(blockLocation)) {
            return;
        }
        Block block = blockLocation.getBlock();
        Material material = BlockEnvironment.getMaterial(block);
        AxisAlignedBox[] boxes = BlockCollisionGeometry.getBlockShapes(
                block, BlockShapeMode.INTERACT, material);
        if (!BlockCollisionGeometry.isUnitCube(boxes)
                || MaterialPolicy.containsSupport(material)
                || !(placement.getPlayerLocation().getY() >= (double) (placement.getBlockY() + 1))) {
            return;
        }
        AxisAlignedBox box = boxes[0];
        double eyeHeight = BlockRayTraceHelper.eyeHeight(
                placement.getPlayer(), playerState());
        Ray[] rays = new Ray[2];
        rays[0] = new Ray(
                placement.getPlayerLocation().toVector().add(new Vector(0.0, eyeHeight, 0.0)),
                placement.getPlayerLocation().getDirection());
        rays[1] = new Ray(
                placement.getPlayerLocation().toVector().add(new Vector(0.0, eyeHeight, 0.0)),
                placement.getRayTraceLocation().getDirection());
        double minimumRayDistance = 1.5;
        for (Ray ray : rays) {
            Vector hit = box.clipSegmentAgainstBox(ray, 0.0f, 8.0f);
            double distance = hit == null ? 1.0 : placement.getVelocity().distance(hit);
            minimumRayDistance = Math.min(distance, minimumRayDistance);
        }
        if (DeveloperCommand.debugScaffold) {
            String color = String.valueOf(ChatColor.GOLD);
            String name = placement.getPlayer().getName();
            double rayDistance = minimumRayDistance;
            boolean legitimate = minimumRayDistance <= 0.084
                    + (placement.isMovementFlagSet() ? 0.15 : 0.0);
            int violationBuffer = suspiciousPlacements;
            boolean movementFlagWasSet = placement.isMovementFlagSet();
            boolean currentlySneaking = sneaking;
            Bukkit.broadcastMessage(new StringBuilder(color).append("[Debug/SfdT] ")
                    .append(name).append(" / min: ").append(rayDistance)
                    .append(" legit: ").append(legitimate).append('/').append(violationBuffer)
                    .append(" sneak: ").append(movementFlagWasSet).append('/').append(currentlySneaking)
                    .toString());
        }
        if (minimumRayDistance > 0.084 + (placement.isMovementFlagSet() ? 0.15 : 0.0)) {
            if (suspiciousPlacements <= 8) {
                suspiciousPlacements += 2;
            }
            if (suspiciousPlacements > 8 && enforcementEnabled) {
                int violationBuffer = suspiciousPlacements;
                double rayDistance = NumberUtils.round3(minimumRayDistance);
                flag("sfd.place.t", "scaffold-like placement | type T (val1: "
                        + violationBuffer + ", val2: " + rayDistance + ")", violationWeight / 2);
                cancelNextPlacement = true;
            }
        } else {
            suspiciousPlacements = Math.max(0, suspiciousPlacements - 1);
        }
    }
}
