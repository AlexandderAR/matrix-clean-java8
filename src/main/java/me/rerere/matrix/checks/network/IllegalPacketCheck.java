package me.rerere.matrix.checks.network;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;

/** Validates incoming position/look values and applies the original repair/report path. */
public class IllegalPacketCheck extends CheckRuntimeState {
    private boolean kickScheduled;
    private long lastWarningTime;

    public IllegalPacketCheck(PlayerCheckContext context) {
        super(context);
        this.kickScheduled = false;
        this.setCheckType(CheckCategory.BADPACKETS);
    }

    private static boolean isFiniteFloat(float value) {
        return Double.NEGATIVE_INFINITY < (double) value & (double) value < Double.POSITIVE_INFINITY;
    }

    public static boolean isFiniteTriple(double x, double y, double z) {
        return isFinite(x) && isFinite(y) && isFinite(z)
                && Math.abs(x) < 30000000.0 && Math.abs(z) < 30000000.0
                && Math.abs(y) < 30000000.0;
    }

    private static boolean isFinite(double value) {
        return Double.NEGATIVE_INFINITY < value & value < Double.POSITIVE_INFINITY;
    }

    public static boolean hasFiniteAngles(float yaw, float pitch) {
        return isFiniteFloat(yaw) && isFiniteFloat(pitch)
                && !Float.isNaN(yaw) && !Float.isNaN(pitch);
    }

    public static boolean hasFiniteCoordinates(org.bukkit.Location location) {
        return isFiniteTriple(location.getX(), location.getY(), location.getZ());
    }

    private static String positionMessage(String positionText) {
        return "(valid) illegal position packet | " + positionText;
    }

    private void scheduleInvalidDataKick() {
        if (kickScheduled) return;
        final Player player;
        try {
            player = this.playerState().getPlayer();
        } catch (Throwable failure) {
            throw IllegalPacketCheck.<RuntimeException>rethrowUnchecked(failure);
        }
        if (player != null && player.isOnline()) {
            kickScheduled = true;
            BukkitScheduler scheduler = Bukkit.getScheduler();
            scheduler.runTask(Matrix.getInstance(),
                    () -> kickForInvalidPacket(player));
        }
    }

    private static void kickForInvalidPacket(Player player) {
        player.kickPlayer("Invalid Packet Data\nPlease disable Optfine FastMath feature!!");
    }

    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event,
                                  PacketWrapper wrapper) {
        if (PacketEventSupport.isPlayerFlying(packetType)
                && !PacketEventSupport.isPlayerFlyingPacket(packetType)) {
            WrapperPlayClientPlayerPositionAndRotation positionAndLookPacket =
                    (WrapperPlayClientPlayerPositionAndRotation) wrapper;
            if (!isFiniteTriple(positionAndLookPacket.getPosition().x,
                    positionAndLookPacket.getPosition().y, positionAndLookPacket.getPosition().z)) {
                logIfPastInterval("[Matrix] Received invalid pos packet!");
                this.flag("net.crash", positionMessage(positionAndLookPacket.getPosition().toString()), 1000);
                scheduleInvalidDataKick();
                return true;
            }
            float yaw = positionAndLookPacket.getYaw();
            float pitch = positionAndLookPacket.getPitch();
            if (!hasFiniteAngles(yaw, pitch)) {
                positionAndLookPacket.setYaw(0.0f);
                positionAndLookPacket.setPitch(0.0f);
                logIfPastInterval("[Matrix] Received invalid look packet!");
                this.flag("net.crash", "(valid) illegal look packet", 1000);
                scheduleInvalidDataKick();
                return true;
            }
        }
        if (PacketEventSupport.isPlayerRotationPacket(packetType)) {
            WrapperPlayClientPlayerPositionAndRotation lookPacket =
                    (WrapperPlayClientPlayerPositionAndRotation) wrapper;
            float yaw = lookPacket.getYaw();
            float pitch = lookPacket.getPitch();
            boolean changed = false;
            if (yaw == Float.NEGATIVE_INFINITY) {
                lookPacket.setYaw(0.0f);
                changed = true;
                this.flag("net.crash", "send illegal look packet: T1", 1);
            }
            if (pitch == Float.NEGATIVE_INFINITY) {
                lookPacket.setPitch(0.0f);
                changed = true;
                this.flag("net.crash", "send illegal look packet: T2", 1);
            }
            if (yaw >= 100000.0f) {
                lookPacket.setYaw(0.0f);
                changed = true;
            }
            if (Math.abs(pitch) > 90.0f) {
                lookPacket.setPitch(0.0f);
                changed = true;
            }
            if (changed) event.markForReEncode(true);
        }
        return false;
    }

    private void logIfPastInterval(String message) {
        if (System.currentTimeMillis() - lastWarningTime > 1000L) {
            System.out.println(message);
            lastWarningTime = System.currentTimeMillis();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException rethrowUnchecked(Throwable failure) throws T {
        throw (T) failure;
    }
}
