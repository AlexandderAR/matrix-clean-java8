package me.rerere.matrix.checks.movement;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.MutableGeometryState;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.player.PlayerUtilities;

/** Client position packet flight check. */
public final class PositionPacketFlyCheck extends CheckRuntimeState {
    private int accumulatedViolations;
    private long lastCheckMillis;
    private double lastY;
    private static final long CHECK_INTERVAL_MILLIS = 995L;

    public PositionPacketFlyCheck(PlayerCheckContext tracker) {
        super(tracker, CheckCategory.BADPACKETS);
    }

    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event, PacketWrapper wrapper) {
        if (packetType == PacketType$Play$Client.PLAYER_POSITION_AND_ROTATION
                || packetType == PacketType$Play$Client.PLAYER_POSITION) {
            return handlePosition((WrapperPlayClientPlayerPositionAndRotation) wrapper);
        }
        return false;
    }

    private static String formatMessage(int count, double magnitude) {
        return "packet fly (t: " + count + ", m: " + magnitude + ")";
    }

    private boolean handlePosition(WrapperPlayClientPlayerPositionAndRotation packet) {
        double currentY = packet.getPosition().getY();
        double deltaY = currentY - lastY;
        boolean reported = false;
        if (deltaY > 10.0) {
            accumulatedViolations++;
            if (deltaY > 320.0) accumulatedViolations += 5;
            long now = MillisecondSampler.currentTimeMillis();
            if (now - lastCheckMillis >= CHECK_INTERVAL_MILLIS) {
                if (accumulatedViolations > 10
                        && Math.abs(super.player().getVelocity().getY()) < 2.0) {
                    int severity = accumulatedViolations >= 30 ? 100 : 20;
                    super.flag("net.pkf", formatMessage(accumulatedViolations,
                            NumberUtils.round3(deltaY)), severity);
                    MutableGeometryState anchorState = super.playerState().movementContext();
                    if (anchorState.capacity != null
                            && anchorState.capacity.getWorld() == super.playerState().getPlayer().getWorld()) {
                        FoliaSupport.INSTANCE.runSyncForPlayer(super.player(), this::runDeferred);
                    }
                    reported = true;
                }
                accumulatedViolations = 0;
                lastCheckMillis = now;
            }
        }
        lastY = currentY;
        return reported;
    }

    private me.rerere.matrix.thirdparty.kotlin.Unit runDeferred() {
        org.bukkit.entity.Player player = super.player();
        org.bukkit.Location anchor = super.playerState().movementContext().capacity;
        PlayerUtilities.teleportViaPlatform(player, anchor);
        return me.rerere.matrix.thirdparty.kotlin.Unit.INSTANCE;
    }
}
