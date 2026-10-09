package me.rerere.matrix.checks.player;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityVelocity;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.PotionEffects;
import me.rerere.matrix.world.BlockEnvironment;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import me.rerere.matrix.player.PlayerUtilities;

/** Tracks outgoing velocity packets and checks whether the player received the expected vertical motion. */
public class VelocityResponseCheck extends CheckRuntimeState {
    private static int moduleThreshold;
    private static boolean ignoreRecentLag;
    private long lastTeleportAt;
    private int remainingTicks;
    private static int waitTicks;
    private boolean confirmed;
    private Vector expectedVelocity;
    static {
        waitTicks = 20;
        ignoreRecentLag = true;
    }

    public VelocityResponseCheck(PlayerCheckContext playerState) {
        super(playerState);
        this.confirmed = false;
        this.remainingTicks = 0;
        setCheckType(CheckCategory.VELOCITY);
    }

    /** Original public initialization hook inherited from the check base. */
    @Override
    public void reloadConfiguration() {
        waitTicks = configuration().getInt("wait_time", 20);
        ignoreRecentLag = configuration().getBoolean("ignore_lag", true);
        moduleThreshold = configuration().getInt("modules.respond", 5);
    }

    private boolean shouldCheckExpectedVelocity(Location location, Player player) {
        if (player.isDead()) return false;
        if (System.currentTimeMillis() - this.lastTeleportAt <= 2000L) return false;
        PotionEffectType excludedEffect = PotionEffects.singleton.getJumpBoost();
        if (player.hasPotionEffect(excludedEffect)) return false;
        if (!(player.getFallDistance() <= 3.0F)) return false;
        if (player.isFlying() || player.isInsideVehicle() || player.isSleeping()) return false;
        if (!BlockEnvironment.isPredicateMaterialAtAndAbove(location)) return false;
        if (BlockEnvironment.hasMatchingNeighborAroundCurrentAndAbove(location)) return false;
        if (BlockEnvironment.hasOaWeightWithinUnitHeadVolume(player)) return false;
        if (BlockEnvironment.hasSlimeBlockUnderPlayer(player)) return false;
        if (BlockEnvironment.hasIceBelowPlayer(player)) return false;

        if (ignoreRecentLag) {
            long lastStateTime = playerState().lastActivityTime();
            if (System.currentTimeMillis() - lastStateTime > 1000L) return false;
        }

        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion current =
                Matrix.getServerVersion();
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion v1_8_8 =
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_8_8;
        if (current.isNewerThan(v1_8_8)) {
            if (player.isGliding()) return false;
            if (player.hasPotionEffect(PotionEffectType.LEVITATION)) return false;
        }

        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion versionForSwimming =
                Matrix.getServerVersion();
        me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion v1_13 =
                me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion.V_1_13;
        if (versionForSwimming.isNewerThanOrEquals(v1_13) && (player.isSwimming() || player.isRiptiding())) return false;
        if (Math.abs(player.getWalkSpeed() - 0.2F) > 0.02F) return false;
        if (player.getMaximumNoDamageTicks() < 10) return false;
        World world = player.getWorld();
        if (isNearWorldBorder(world, location)) return false;

        if (Matrix.getServerVersion().isNewerThan(v1_8_8)) {
            double maximumEntityWidth = 0.135;
            if (player.hasPotionEffect(PotionEffectType.SPEED)) {
                PotionEffect speed = player.getPotionEffect(PotionEffectType.SPEED);
                maximumEntityWidth += 0.0265 * (speed.getAmplifier() + 1);
            }
            return !((double) PlayerUtilities.entityWidth(player) > maximumEntityWidth);
        }
        return true;
    }

    public void onPlayerTeleport(PlayerTeleportEvent event) {
        this.lastTeleportAt = System.currentTimeMillis();
    }

    public boolean onPacketSend(PacketTypeCommon packetType, PacketSendEvent event, PacketWrapper wrapper) {
        if (packetType == PacketType$Play$Server.ENTITY_VELOCITY && !event.isCancelled()) {
            WrapperPlayServerEntityVelocity velocityPacket = (WrapperPlayServerEntityVelocity) wrapper;
            Player target = (Player) event.getPlayer();
            if (target != null && velocityPacket.getEntityId() == target.getEntityId()) {
                double x = velocityPacket.getVelocity().x;
                double y = velocityPacket.getVelocity().y;
                double z = velocityPacket.getVelocity().z;
                if (moduleThreshold >= 0 && y > 0.1 && y <= 0.5
                        && Math.abs(x) < 1.0 && Math.abs(z) < 1.0) {
                    clearExpectation();
                    this.expectedVelocity = new Vector(x, y, z);
                    this.confirmed = false;
                    this.remainingTicks = waitTicks;
                }
            }
        }
        return false;
    }

    /** Per-tick countdown and deferred response check. */
    public void onTick() {
        if (this.remainingTicks > 0) {
            this.remainingTicks--;
            if (this.remainingTicks == 0) {
                if (!this.confirmed && this.expectedVelocity != null) {
                    Player player = player();
                    if (player != null && shouldCheckExpectedVelocity(player.getLocation(), player)) {
                        String details = DiagnosticFormat.vector(this.expectedVelocity);
                        String message = "(respond) ignore server velocity: " + details;
                        flag("vel.expect", message, 5);
                    }
                }
                clearExpectation();
            }
        }
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        if (this.expectedVelocity != null && this.remainingTicks > 0) {
            double residual = this.expectedVelocity.getY() - (to.getY() - from.getY());
            if (Math.abs(residual) < 0.001) {
                this.confirmed = true;
                clearExpectation();
            }
        }
    }

    public void onPlayerRespawn(PlayerRespawnEvent event) {
        this.lastTeleportAt = System.currentTimeMillis();
    }

    private boolean isNearWorldBorder(World world, Location location) {
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double halfSize = border.getSize() / 2.0;
        if (Math.abs(Math.abs(location.getX() - center.getX()) - halfSize) < 5.0) return true;
        return Math.abs(Math.abs(location.getZ() - center.getZ()) - halfSize) < 5.0;
    }

    private void clearExpectation() {
        this.expectedVelocity = null;
        this.confirmed = false;
        this.remainingTicks = 0;
    }
}
