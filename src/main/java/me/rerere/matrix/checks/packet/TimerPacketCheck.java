package me.rerere.matrix.checks.packet;

import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.CheckRegistry;
import me.rerere.matrix.checks.MovementPhysicsCheck;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.player.PlayerUtilities;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.util.time.MillisecondSampler;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TimerPacketCheck extends CheckRuntimeState {
    private static int bufferSize = 30;
    private static int freezeTimeMillis = 500;
    private static int maxExtraPackets = 15;
    private static int limitViolationWeight;
    private static int timerViolationWeight;
    private int bufferPackets;
    private boolean lastPacketWasIdle;
    private long accumulatedMillis;
    private int countedPackets;
    private long lastPacketMillis;
    private long lastFastWindowMillis;
    private double fastWindowScore;
    private int exemptPackets;
    private final long createdTime;
    private boolean rollbackPending;
    private long freezeStartedMillis;
    private int fastWindows;
    private long windowStartedMillis;

    public TimerPacketCheck(PlayerCheckContext context) {
        super(context);
        setCheckType(CheckCategory.BADPACKETS);
        createdTime = MillisecondSampler.currentTimeMillis();
    }

    public void reloadConfiguration() {
        maxExtraPackets = configuration().getInt("max_extra_packets", 15);
        bufferSize = configuration().getInt("buffer_size", 30);
        if (bufferSize < 10) bufferSize = 20;
        freezeTimeMillis = configuration().getInt("freeze_time", 500);
        limitViolationWeight = configuration().getInt("vl_weights.limit", 10);
        timerViolationWeight = configuration().getInt("vl_weights.timer", 4);
    }

    public boolean onPacketSend(PacketTypeCommon type, PacketSendEvent ignoredEvent, PacketWrapper ignoredWrapper) {
        if (type == PacketType$Play$Server.PLAYER_POSITION_AND_LOOK) exemptPackets += 2;
        return false;
    }

    public boolean onPacketReceive(PacketTypeCommon type, PacketReceiveEvent ignoredEvent, PacketWrapper ignoredWrapper) {
        boolean cancel = false;
        if (PacketEventSupport.isPlayerFlying(type)) {
            boolean idle = PacketEventSupport.isPlayerFlyingPacket(type);
            cancel = onFlyingPacket(idle);
        }
        if (type == PacketType$Play$Client.PLAYER_BLOCK_PLACEMENT
                && PlayerUtilities.clientVersion(player())
                        .isNewerThanOrEquals(ClientVersion.V_1_17)) {
            exemptPackets++;
            CheckRegistry checks = playerState().checks();
            MovementPhysicsCheck movementCheck = (MovementPhysicsCheck) checks.firstCheckOfType(MovementPhysicsCheck.class);
            movementCheck.advanceFirstMoveBudget();
        }
        return cancel;
    }

    private boolean onFlyingPacket(boolean idle) {
        if (exemptPackets > 0) {
            exemptPackets--;
            return false;
        }
        if (bufferPackets++ >= bufferSize) {
            long elapsed = Math.abs(System.currentTimeMillis() - windowStartedMillis);
            if (elapsed < (long) bufferSize * 50L - 5L) {
                if (fastWindows++ > 1) {
                    double expectedWindowMillis = (double) bufferSize * 50.0;
                    double speedRatio = expectedWindowMillis / (double) elapsed;
                    boolean recentlyCreated = MillisecondSampler.currentTimeMillis() - createdTime <= 5000L;
                    double roundedRatio = NumberUtils.round3(speedRatio);
                    String message = new StringBuilder("(timer) moved too many times (timer: ")
                            .append(roundedRatio)
                            .append(", t:").append(elapsed)
                            .append(", buffer:").append(bufferSize)
                            .append(", tag: timer)").toString();
                    flag("net.timer", message, recentlyCreated ? 1 : timerViolationWeight);
                    rollbackPending = true;
                    if (speedRatio > 1.25) {
                        freezeStartedMillis = System.currentTimeMillis();
                        playerState().movementContext().angle = true;
                    }
                    if (System.currentTimeMillis() - lastFastWindowMillis < (long) bufferSize * 50L + 50L) {
                        freezeStartedMillis = System.currentTimeMillis();
                    }
                    lastFastWindowMillis = System.currentTimeMillis();
                    fastWindowScore += 2.0;
                    if (fastWindowScore > 5.0) freezeStartedMillis = System.currentTimeMillis();
                }
            } else {
                fastWindows = 0;
                if (fastWindowScore > 1.0) fastWindowScore /= 2.0;
            }
            bufferPackets = 0;
            windowStartedMillis = System.currentTimeMillis();
        }
        if (idle) {
            lastPacketMillis = System.currentTimeMillis();
            lastPacketWasIdle = true;
            return false;
        }
        long elapsed = System.currentTimeMillis() - lastPacketMillis;
        if (lastPacketWasIdle && elapsed > 100L) elapsed = 50L;
        boolean cancel = false;
        accumulatedMillis += elapsed;
        countedPackets++;
        if (accumulatedMillis >= 995L) {
            if (countedPackets > 20 + maxExtraPackets) {
                String message = new StringBuilder("(timer) send ").append(countedPackets)
                        .append(" moving packets in ").append(accumulatedMillis)
                        .append("ms | tag: limit").toString();
                flag("net.timer", message, limitViolationWeight);
            }
            accumulatedMillis = 0L;
            countedPackets = 0;
        }
        if (countedPackets > 20 + maxExtraPackets) {
            cancel = true;
            freezeStartedMillis = System.currentTimeMillis();
        }
        lastPacketMillis = System.currentTimeMillis();
        lastPacketWasIdle = false;
        return cancel;
    }

    public void onPlayerChangedWorld(PlayerChangedWorldEvent ignoredEvent) { exemptPackets += 15; }

    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to != null && event.getCause() != PlayerTeleportEvent.TeleportCause.UNKNOWN) {
            double deltaX = Math.abs(to.getX() - from.getX());
            double deltaZ = Math.abs(to.getZ() - from.getZ());
            if (deltaX + deltaZ > 80.0) exemptPackets += 15;
        }
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location ignoredTo) {
        Player player = event.getPlayer();
        if (rollbackPending) {
            rollbackPending = false;
            if (PlayerUtilities.hasPlayerDataOrEntityFlag(player)) event.setTo(from);
        }
        if (System.currentTimeMillis() - freezeStartedMillis < (long) freezeTimeMillis) event.setTo(from);
    }
}
