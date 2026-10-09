package me.rerere.matrix.checks.movement;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.geometry.AxisAlignedBox;
import me.rerere.matrix.geometry.BlockShapeMode;
import me.rerere.matrix.movement.VerticalVelocitySample;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.PotionEffects;
import me.rerere.matrix.world.BlockCollisionGeometry;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.world.MaterialPolicy;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffectType;
import me.rerere.matrix.player.PlayerUtilities;

/** Detects suspicious repeated vertical velocity packets and vertical movement. */
public final class VerticalVelocityCheck extends CheckRuntimeState {
    private static int configuredLevel;
    private static boolean verticalEnabled;
    private final List<VerticalVelocitySample> pendingVelocities = new CopyOnWriteArrayList<>();
    private long lastViolationAt;

    public VerticalVelocityCheck(PlayerCheckContext playerData) {
        super(playerData);
        setCheckType(CheckCategory.VELOCITY);
    }

    public void reloadConfiguration() {
        configuredLevel = configuration()
                .getInt("modules.vertical", 1);
        verticalEnabled = configuredLevel > 0;
    }

    public boolean nearWorldBorder(World world, Location location) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double halfSize = border.getSize() / 2.0;
        return Math.abs(Math.abs(location.getX() - center.getX()) - halfSize) < 5.0
                || Math.abs(Math.abs(location.getZ() - center.getZ()) - halfSize) < 5.0;
    }

    private static boolean sameVerticalVelocity(double movementY, VerticalVelocitySample sample) {
        return Math.abs(movementY - sample.componentVerticalVelocity()) < 0.01;
    }

    private boolean mayMoveVertically(Location position, Player player) {
        if (player.isDead()
                || player.hasPotionEffect(PotionEffects.singleton.getJumpBoost())
                || !(player.getFallDistance() <= 3.0F)
                || player.isFlying()
                || player.isInsideVehicle()
                || player.isSleeping()
                || !BlockEnvironment.isPredicateMaterialAtAndAbove(position)
                || BlockEnvironment.hasOaWeightWithinUnitHeadVolume(player)
                || BlockEnvironment.hasIceBelowPlayer(player)
                || BlockEnvironment.hasSlimeBlockUnderPlayer(player)) {
            return false;
        }

        long now = System.currentTimeMillis();
        long lastActivityTime = playerState().lastActivityTime();
        if (now - lastActivityTime > 1000L) return false;

        if (serverIsNewerThan1_8_8()) {
            if (player.isGliding() || player.hasPotionEffect(PotionEffectType.LEVITATION)) return false;
        }
        if (serverIsAtLeast1_13()) {
            if (player.isSwimming() || player.isRiptiding()) return false;
        }
        if (!collisionSpaceClear(player.getLocation(), 1.0)) return false;
        if (Math.abs(player.getWalkSpeed() - 0.2F) > 0.02F) return false;
        if (player.getMaximumNoDamageTicks() < 10) return false;
        if (nearWorldBorder(player.getWorld(), position)) return false;

        if (!serverIsNewerThan1_8_8()) return true;
        double maximumAllowedWidth = 0.135;
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            maximumAllowedWidth += 0.0265 * (player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1);
        }
        return !(PlayerUtilities.entityWidth(player) > maximumAllowedWidth);
    }

    public void onPlayerRespawn(PlayerRespawnEvent event) {
        pendingVelocities.clear();
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        if (pendingVelocities.isEmpty()) return;
        double movementY = to.getY() - from.getY();
        for (VerticalVelocitySample sample : pendingVelocities) {
            long receivedAt = sample.componentTimestamp();
            if (System.currentTimeMillis() - receivedAt > 1000L) {
                if (mayMoveVertically(from, event.getPlayer())) {
                    if (System.currentTimeMillis() - lastViolationAt < 2000L) {
                        String message = formatVelocityMessage(
                                sample.componentVerticalVelocity());
                        flag("vel.vet", message, configuredLevel);
                    }
                    lastViolationAt = System.currentTimeMillis();
                }
                pendingVelocities.remove(sample);
            }
        }
        pendingVelocities.removeIf(sample -> sameVerticalVelocity(movementY, sample));
    }

    public boolean onPacketSend(
            PacketTypeCommon type, PacketSendEvent event, PacketWrapper<?> wrapper) {
        if (type != PacketType$Play$Server.ENTITY_VELOCITY) {
            return false;
        }
        WrapperPlayServerEntityVelocity velocityPacket =
                (WrapperPlayServerEntityVelocity) wrapper;
        Player player = (Player) event.getPlayer();
        if (player == null) return false;
        if (velocityPacket.getEntityId() != player.getEntityId()) return false;
        double horizontalVelocityX = velocityPacket.getVelocity().x;
        double verticalVelocity = velocityPacket.getVelocity().y;
        double horizontalVelocityZ = velocityPacket.getVelocity().z;
        if (verticalVelocity > 0.1 && verticalEnabled && pendingVelocities.size() < 10) {
            pendingVelocities.add(new VerticalVelocitySample(verticalVelocity, System.currentTimeMillis()));
        }
        return false;
    }

    private boolean collisionSpaceClear(Location location, double height) {
        AxisAlignedBox playerSpace = new AxisAlignedBox(
                location.toVector(), height + 0.5, 2.1);
        World world = location.getWorld();
        for (int x = (int) playerSpace.minX() - 2;
             x <= playerSpace.maxX() + 1.0; x++) {
            for (int y = (int) playerSpace.minY() - 2;
                 y <= playerSpace.maxY() + 1.0; y++) {
                for (int z = (int) playerSpace.minZ() - 2;
                     z <= playerSpace.maxZ() + 1.0; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (MaterialPolicy.isAirMaterial(block.getType())) continue;
                    AxisAlignedBox[] collisionBoxes = BlockCollisionGeometry.getBlockShapes(
                            block, BlockShapeMode.COLLISION);
                    for (AxisAlignedBox collisionBox : collisionBoxes) {
                        if (collisionBox.intersects(playerSpace)) return false;
                    }
                }
            }
        }
        return true;
    }

    private static String formatVelocityMessage(double verticalVelocity) {
        return "(vertical) reduce vertical velocity: " + verticalVelocity;
    }

    private static boolean serverIsNewerThan1_8_8() {
        return Matrix.getServerVersion().isNewerThan(
                ServerVersion.V_1_8_8);
    }

    private static boolean serverIsAtLeast1_13() {
        return Matrix.getServerVersion().isNewerThanOrEquals(
                ServerVersion.V_1_13);
    }
}
