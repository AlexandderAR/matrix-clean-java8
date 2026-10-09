package me.rerere.matrix.checks.movement;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;

/** Checks for movement inconsistent with a recent grounded player state. */
public final class FreecamMovementCheck extends me.rerere.matrix.checks.CheckRuntimeState {
    private long lastCheckAt;
    private long lastPacketAt;
    private static boolean checkEnabled;
    public static final me.rerere.matrix.config.FeatureOption ANGLE = new me.rerere.matrix.config.FeatureOption((me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker) null);

    public FreecamMovementCheck(me.rerere.matrix.player.PlayerCheckContext data) {
        super(data, me.rerere.matrix.checks.CheckCategory.BADPACKETS);
        this.lastPacketAt = me.rerere.matrix.util.time.MillisecondSampler.lastSampleMillis();
        this.lastCheckAt = me.rerere.matrix.util.time.MillisecondSampler.lastSampleMillis();
    }

    public void onTick() {
        long now = me.rerere.matrix.util.time.MillisecondSampler.lastSampleMillis();
        if (checkEnabled && now - lastCheckAt > 3500L && now - lastPacketAt <= 75L) {
            Player player = (Player) this.player();
            if (isGroundedPlayer(player)) {
                String message = describeDelay(now - lastCheckAt, now - lastPacketAt);
                this.flag("bp.freecam", message, 2);
                lastCheckAt += 50L;
            }
        }
    }

    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event, PacketWrapper wrapper) {
        if (me.rerere.matrix.network.packet.PacketEventSupport.isPlayerFlying(packetType)) {
            lastCheckAt = me.rerere.matrix.util.time.MillisecondSampler.lastSampleMillis();
        } else {
            lastPacketAt = me.rerere.matrix.util.time.MillisecondSampler.lastSampleMillis();
        }
        return false;
    }

    private static String describeDelay(long sinceCheck, long sincePacket) {
        return "might using freecam (a: " + sinceCheck + ", b: " + sincePacket + ")";
    }

    private static boolean isGroundedPlayer(Player player) {
        boolean survival = player.getGameMode() == GameMode.SURVIVAL;
        boolean allowedMode = survival || player.getGameMode() == GameMode.ADVENTURE;
        return allowedMode && !player.isDead() && player.isOnGround();
    }

    public static final void setEnabled(boolean enabled) { checkEnabled = enabled; }

    public static final boolean enabled() { return checkEnabled; }
}
