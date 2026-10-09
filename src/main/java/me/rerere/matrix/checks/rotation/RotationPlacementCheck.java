package me.rerere.matrix.checks.rotation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;

/** Flags legacy clients that repeatedly place blocks without changing rotation. */
public class RotationPlacementCheck extends CheckRuntimeState {
    private long lastObservedAt;
    private float lastPitch;
    private float lastYaw;
    public static boolean WEIGHT;
    private long lastTeleportAt;
    private static final Set<Material> LEGACY_SUPPORT_BLOCKS;

    public RotationPlacementCheck(PlayerCheckContext player) {
        super(player);
        setCheckType(CheckCategory.SCAFFOLD);
    }

    private static String formatRotation(float yaw, float pitch) {
        return "equal rotation | " + yaw + ", " + pitch;
    }

    public void onTeleport(PlayerTeleportEvent event) { lastTeleportAt = System.currentTimeMillis(); }

    public void onBlockPlace(BlockPlaceEvent event) {
        if (System.currentTimeMillis() - lastObservedAt >= 1000L) return;
        if (!playerState().clientVersion().isOlderThan(ClientVersion.V_1_17)) return;
        Player player = event.getPlayer();
        if (!MaterialPolicy.isAirMaterial(player.getLocation().getBlock().getType())) return;
        if (!BlockEnvironment.isPredicateMaterialAtAndAbove(player)) return;
        if (BlockEnvironment.isSpecialPredicateAtOrAbove(player)) return;
        if (BlockEnvironment.hasPlayerNeighborhoodCondition(player)) return;
        Location location = player.getLocation();
        if (LEGACY_SUPPORT_BLOCKS.contains(location.getBlock().getRelative(BlockFace.DOWN).getType())) return;
        if (!WEIGHT) return;
        String detail = formatRotation(
                NumberUtils.round3(lastYaw),
                NumberUtils.round3(lastPitch));
        flag("sfd.eqr", detail, 5);
    }

    public boolean onPacketReceive(PacketTypeCommon type, PacketReceiveEvent event, PacketWrapper packet) {
        if (type != PacketType$Play$Client.PLAYER_ROTATION
                && type != PacketType$Play$Client.PLAYER_POSITION_AND_ROTATION) return false;
        WrapperPlayClientPlayerPositionAndRotation rotation = (WrapperPlayClientPlayerPositionAndRotation) packet;
        float yaw = rotation.getYaw();
        float pitch = rotation.getPitch();
        if (pitch == lastPitch && yaw == lastYaw) {
            long now = System.currentTimeMillis();
            if (now - lastTeleportAt > 1000L + playerState().latencyMilliseconds()) {
                Player player = player();
                if (!player.isInsideVehicle()) lastObservedAt = System.currentTimeMillis();
            }
        }
        lastYaw = yaw;
        lastPitch = pitch;
        return false;
    }

    static {
        if (Matrix.getServerVersion() == ServerVersion.V_1_8_8) {
            Material pane = java.util.Objects.requireNonNull(Material.getMaterial("STAINED_GLASS_PANE"));
            Material glass = java.util.Objects.requireNonNull(Material.getMaterial("THIN_GLASS"));
            Material fence = java.util.Objects.requireNonNull(Material.getMaterial("IRON_FENCE"));
            LEGACY_SUPPORT_BLOCKS = Collections.unmodifiableSet(
                    EnumSet.of(pane, glass, fence, Material.CHEST, Material.ANVIL));
        } else {
            LEGACY_SUPPORT_BLOCKS = Collections.emptySet();
        }
    }
}
