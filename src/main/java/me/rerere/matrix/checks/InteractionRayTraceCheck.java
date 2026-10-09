package me.rerere.matrix.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.collections.SetsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.PropertyReference1Impl;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Reflection;
import me.rerere.matrix.thirdparty.kotlin.reflect.KClass;
import me.rerere.matrix.thirdparty.kotlin.reflect.KProperty;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.DiggingAction;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.util.Vector3i;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerBlockPlacement;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import me.rerere.matrix.config.DefaultSettingProperty;
import me.rerere.matrix.entity.PositionSnapshot;
import me.rerere.matrix.listeners.CallbackEventFactory;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.geometry.Ray;
import me.rerere.matrix.util.BukkitVectorMath;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.util.SafeBuffer;
import me.rerere.matrix.world.InteractionSnapshot;
import me.rerere.matrix.world.RaytraceResult;
import me.rerere.matrix.world.WorldAccess;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.checks.movement.DistanceAxis;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.Matrix;

public final class InteractionRayTraceCheck extends CheckRuntimeState {
    private double packetX;
    private double packetY;
    private double packetZ;
    private float packetYaw;
    private float packetPitch;
    private int pendingCancellations;
    private long nextActionSequence;
    public static final KProperty<?>[] delegatedProperties = {
        Reflection.property1(new PropertyReference1Impl(InteractionRayTraceCheck.class,
                "strict", "getStrict()Z", 0))
    };
    public static final InteractionRayTraceSettings Companion = new InteractionRayTraceSettings();
    private static boolean raytraceEnabled;
    private static boolean secondaryRaytraceMode;
    private final DefaultSettingProperty strictSetting;
    private final SafeBuffer<InteractionSnapshot> pendingInteractions;

    public InteractionRayTraceCheck(PlayerCheckContext context) {
        super(context);
        pendingInteractions = new SafeBuffer<>(40);
        strictSetting = CallbackEventFactory.createSecondary(this, "strict", Boolean.FALSE);
        setCheckType(CheckCategory.INTERACT);
    }

    public static boolean isRaytraceEnabled() { return raytraceEnabled; }
    public static void setRaytraceEnabled(boolean enabled) { raytraceEnabled = enabled; }
    public static void setSecondaryRaytraceMode(boolean enabled) { secondaryRaytraceMode = enabled; }
    public static boolean isSecondaryRaytraceModeEnabled() { return secondaryRaytraceMode; }
    public static boolean isSecondaryRaytraceModeDisabled() { return !isSecondaryRaytraceModeEnabled(); }
    private boolean getStrict() {
        return (Boolean) strictSetting.getValue(this, delegatedProperties[0]);
    }
    private boolean supportsPacketRaytrace() {
        return Matrix.getServerVersion()
                .isOlderThanOrEquals(ServerVersion.V_1_12_2)
            || Matrix.getServerVersion()
                .isNewerThanOrEquals(ServerVersion.V_1_19);
    }

    public void onBlockPlace(BlockPlaceEvent event) {
        if (pendingCancellations > 0) { pendingCancellations--; event.setCancelled(true); }
    }
    public void onBlockBreak(BlockBreakEvent event) {
        if (pendingCancellations > 0) { pendingCancellations--; event.setCancelled(true); }
    }
    public void onPlayerTeleport(PlayerTeleportEvent ignored) {
        pendingInteractions.clear();
    }

    private static Unit updateMovementAction(double x, double y, double z, float yaw, float pitch,
                                             InteractionSnapshot movementAction) {
        if (movementAction.secondary() == null) {
            movementAction.setSecondary(new PositionSnapshot(x, y, z, yaw, pitch));
        }
        return Unit.INSTANCE;
    }

    public void onTick() {
        if (pendingCancellations > 5) pendingCancellations = 5;
        if (pendingInteractions.isEmpty()) return;
        List<InteractionSnapshot> waiting = new ArrayList<>();
        for (InteractionSnapshot action : pendingInteractions.snapshot()) {
            if (action.secondary() == null) waiting.add(action);
        }
        for (InteractionSnapshot action : waiting) {
            action.setCounter(action.counter() + 1);
            if (action.counter() > 6)
                action.setSecondary(action.primary());
        }
    }

    public boolean onPacketReceive(PacketTypeCommon type, PacketReceiveEvent ignored, PacketWrapper<?> wrapper) {
        if (!supportsPacketRaytrace()) return false;
        
        if (PacketEventSupport.isBlockPlacementPacket(type)) {
            WrapperPlayClientPlayerBlockPlacement placement = requireWrapper(wrapper, WrapperPlayClientPlayerBlockPlacement.class);
            Vector3i position = placement.getBlockPosition();
            me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.world.BlockFace face = placement.getFace();
            if (position.x != -1 && position.y != -1 && position.z != -1) {
                long sequence = nextActionSequence++;
                DistanceAxis actionType = DistanceAxis.PLACE;
                PositionSnapshot location = new PositionSnapshot(packetX, packetY, packetZ, packetYaw, packetPitch);
                BlockFace blockFace = BlockFace.valueOf(face.name());
                pendingInteractions.add(new InteractionSnapshot(sequence, actionType,
                        position.x, position.y, position.z, blockFace, location, null, 128, null));
            }
        } else if (type == PacketType$Play$Client.PLAYER_DIGGING) {
            WrapperPlayClientPlayerDigging digging = requireWrapper(wrapper, WrapperPlayClientPlayerDigging.class);
            if (digging.getAction() == DiggingAction.START_DIGGING) {
                Vector3i position = digging.getBlockPosition();
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.world.BlockFace face = digging.getBlockFace();
                long sequence = nextActionSequence++;
                DistanceAxis actionType = DistanceAxis.BREAK;
                BlockFace blockFace = BlockFace.valueOf(face.name());
                PositionSnapshot location = new PositionSnapshot(packetX, packetY, packetZ, packetYaw, packetPitch);
                pendingInteractions.add(new InteractionSnapshot(sequence, actionType,
                        position.x, position.y, position.z, blockFace, location, null, 128, null));
            }
        } else if (PacketEventSupport.isPlayerFlyingPacket(type)) {
            WrapperPlayClientPlayerPositionAndRotation movement = requireWrapper(wrapper, WrapperPlayClientPlayerPositionAndRotation.class);
            boolean positionChanged = movement.hasPositionChanged();
            boolean rotationChanged = movement.hasRotationChanged();
            final double x = positionChanged ? movement.getPosition().x : packetX;
            final double y = positionChanged ? movement.getPosition().y : packetY;
            final double z = positionChanged ? movement.getPosition().z : packetZ;
            final float yaw = rotationChanged ? movement.getYaw() : packetYaw;
            final float pitch = rotationChanged ? movement.getPitch() : packetPitch;
            if (pendingInteractions.hasElements()) {
                pendingInteractions.forEachLocked(action -> updateMovementAction(
                        x, y, z, yaw, pitch, action));
            }
            packetX = x; packetY = y; packetZ = z; packetYaw = yaw; packetPitch = pitch;
        }
        return false;
    }

    private static <T> T requireWrapper(Object wrapper, Class<T> expectedClass) {
        if (wrapper == null) throw new IllegalStateException("cannot cast packet");
        if (!expectedClass.isInstance(wrapper)) {
            // The input error branch casts the supplied raw Class to KClass before formatting.
            KClass<?> expected = (KClass<?>) (Object) expectedClass;
            throw new ClassCastException(formatCastFailure(wrapper.getClass(), expected));
        }
        return expectedClass.cast(wrapper);
    }

    public void onPlayerInteract(PlayerInteractEvent event) {
        processPendingActions();
        if (pendingInteractions.hasElements()) {
            Block clicked = event.getClickedBlock();
            if (clicked != null) {
                List<InteractionSnapshot> matched = new ArrayList<>();
                for (InteractionSnapshot action : pendingInteractions) {
                    if (action.secondary() == null
                            && action.x() == clicked.getX()
                            && action.y() == clicked.getY()
                            && action.z() == clicked.getZ()
                            && !action.enabled()) matched.add(action);
                }
                if (getStrict() && !matched.isEmpty()) event.setCancelled(true);
            }
        }
        if (event.getClickedBlock() != null && pendingCancellations > 0) {
            pendingCancellations--;
            event.setCancelled(true);
        }
    }

    private void processPendingActions() {
        if (pendingInteractions.isEmpty()) return;
        for (InteractionSnapshot action : pendingInteractions.snapshot()) {
            boolean finished;
            try { finished = validateAction(action); }
            catch (Throwable failure) { finished = true; }
            if (finished) pendingInteractions.remove(action);
        }
    }

    private boolean validateAction(InteractionSnapshot action) throws Throwable {
        Location blockLocation = new Location(player().getWorld(),
                action.x(), action.y(),
                action.z());
        if (!FoliaSupport.INSTANCE.isLocationOwnedByCurrentRegion(blockLocation)) return true;
        if (!PlayerUtilities.hasStandingOrCompatiblePose(player())) return true;
        if (player().isInsideVehicle()) return true;
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_17)
                && action.block(player().getWorld()).getType() == Material.SCAFFOLDING) return true;

        List<Pair<Float, Float>> rotations = new ArrayList<>();
        rotations.add(new Pair<>(action.primary().getYaw(),
                action.primary().getPitch()));
        if (action.secondary() != null) {
            rotations.add(new Pair<>(requiredSecondary(action).getYaw(), requiredSecondary(action).getPitch()));
            rotations.add(new Pair<>(action.primary().getYaw(), requiredSecondary(action).getPitch()));
            rotations.add(new Pair<>(requiredSecondary(action).getYaw(), action.primary().getPitch()));
        }
        Set<Double> eyeHeights = SetsKt.plus(SetsKt.mutableSetOf(new Double[]{1.62, 1.54, 1.27}), player().getEyeHeight());
        List<Ray> rays = new ArrayList<>();
        for (Pair<Float, Float> rotation : rotations) {
            for (double eyeHeight : eyeHeights) {
                rays.add(new Ray(BukkitVectorMath.addCopy(
                         action.primary().getPositionVector(), new Vector(0, eyeHeight, 0)),
                        GeometryMath.directionFromYawPitch(rotation)));
            }
        }
        WorldAccess adapter = Matrix.getInstance().getNmsManager().visitor();
        List<RaytraceResult> hits = new ArrayList<>();
        for (Ray ray : CollectionsKt.toSet(rays)) {
            hits.add(adapter.rayTraceBlocks(player().getWorld(),
                    ray.getOrigin(), ray.getDirection(), 6.0));
        }
        boolean found = false;
        for (RaytraceResult hit : CollectionsKt.filterNotNull(hits)) {
            if (hit.getHitBlock() != null
                    && hit.getHitBlock().getX() == action.x()
                    && hit.getHitBlock().getY() == action.y()
                    && hit.getHitBlock().getZ() == action.z()
                    && hit.getHitFace() == action.face()) { found = true; break; }
        }
        if (rotations.size() == 1) action.setEnabled(found);
        if (found) return true;
        if (rotations.size() == 1) return false;
        List<Block> hitBlocks = new ArrayList<>();
        for (RaytraceResult hit : CollectionsKt.filterNotNull(hits)) {
            Block block = hit.getHitBlock();
            if (block != null) hitBlocks.add(block);
        }
        String joined = CollectionsKt.joinToString(hitBlocks, ", ", "", "", -1, "...", InteractionRayTraceCheck::formatHitBlock);
        Block target = action.block(player().getWorld());
        if (target.getType() == Material.AIR) return true;
        String targetDescription = DiagnosticFormat.blockDescription(target);
        flag("interact.ray", formatRaytraceMismatch(targetDescription, joined), 0);
        pendingCancellations++;
        return true;
    }

    private static PositionSnapshot requiredSecondary(InteractionSnapshot action) {
        PositionSnapshot location = action.secondary();
        Intrinsics.checkNotNull(location);
        return location;
    }
    private static CharSequence formatHitBlock(Block block) {
        String type = block.getType().name();
        org.bukkit.Location blockLocation = block.getLocation();
        String location = DiagnosticFormat.formatLocation(blockLocation);
        return formatBlockDetails(type, location);
    }

    private static String formatRaytraceMismatch(String target, String raytrace) {
        return new StringBuilder("interact with ").append(target)
                .append(" but raytrace to ").append(raytrace).toString();
    }

    private static String formatBlockDetails(String type, String location) {
        return new StringBuilder("(").append(type).append(") ").append(location).toString();
    }

    private static String formatCastFailure(Class<?> source, KClass<?> target) {
        return new StringBuilder("Cannot cast ").append(source).append(" to ").append(target).toString();
    }
}
