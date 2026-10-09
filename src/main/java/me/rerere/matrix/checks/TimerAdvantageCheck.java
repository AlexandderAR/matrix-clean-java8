package me.rerere.matrix.checks;

import me.rerere.matrix.compat.KotlinTimeAccess;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.config.DefaultSettingProperty;
import me.rerere.matrix.listeners.CallbackEventFactory;
import me.rerere.matrix.network.packet.PacketEventSupport;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.DurationConstants;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.util.time.MonotonicTimingState;
import me.rerere.matrix.util.time.TimeMarkOperations;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketSendEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Server;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.PropertyReference1Impl;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Reflection;
import me.rerere.matrix.thirdparty.kotlin.reflect.KProperty;
import me.rerere.matrix.thirdparty.kotlin.time.DurationKt;
import me.rerere.matrix.thirdparty.kotlin.time.DurationUnit;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;

/** Tracks timer advantage and applies the observed movement clamp. */
public final class TimerAdvantageCheck extends CheckRuntimeState {
    private long timerDuration;
    private final MonotonicTimingState durationTracker;
    private int cancelViolation;
    private long lastTimeMark;
    private final DefaultSettingProperty cancelViolationSetting;
    private int movementBudget;
    public static final KProperty<?>[] properties;

    static {
        properties = new KProperty<?>[] {
                Reflection.property1(new PropertyReference1Impl(
                        TimerAdvantageCheck.class, "cancelVl", "getCancelVl()I", 0))
        };
    }

    public TimerAdvantageCheck(PlayerCheckContext playerState) {
        super(playerState, CheckCategory.BADPACKETS);
        this.cancelViolationSetting = CallbackEventFactory.createSecondary(
                this, "cancel_vl", Integer.valueOf(3));
        this.durationTracker = new MonotonicTimingState();
        this.timerDuration = DurationKt.toDuration(-5, DurationUnit.SECONDS);
        this.lastTimeMark = KotlinTimeAccess.markNow();
    }

    public void onPlayerMove(PlayerMoveEvent event, Location from, Location to) {
        if (movementBudget <= 0) return;
        if (violationCount() < getCancelVl()) return;
        if (getCancelVl() < 0) return;

        double distance = GeometryMath.horizontalDistance(from, to);
        if (distance >= 0.2D && distance <= 1.0D) {
            Location adjustedDestination = event.getFrom().clone();
            adjustedDestination.setY(to.getY());
            event.setTo(adjustedDestination);
        }
        movementBudget--;
    }

    public void onTick() {
        durationTracker.synchronizeFromTimer(playerState());
    }

    private void addCancelViolation(int amount) {
        cancelViolation += amount;
    }

    public static void applyCancelViolationBridge(TimerAdvantageCheck check, int mask, int value) {
        if ((mask & 1) != 0) value = 1;
        check.addCancelViolation(value);
    }

    public boolean onPacketSend(PacketTypeCommon packetType, PacketSendEvent event, PacketWrapper wrapper) {
        if (packetType == PacketType$Play$Server.PLAYER_POSITION_AND_LOOK) {
            applyCancelViolationBridge(this, 0, 1);
        }
        return false;
    }

    private static String formatAdvantage(String advantage, String server, String synchronization) {
        return "timer (advantage: " + advantage + ", server: " + server + ", sync: " + synchronization + ")";
    }

    private static String formatServerOnly(String server) {
        return "timer (only server: " + server + ")";
    }

    public void resetTimer() {
        applyCancelViolationBridge(this, 0, 1);
    }

    private final int getCancelVl() {
        return ((Number) cancelViolationSetting.getValue(this, properties[0])).intValue();
    }

    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event, PacketWrapper wrapper) {
        if (!PacketEventSupport.isPlayerFlying(packetType)) return false;

        long currentMark = TimeMarkOperations.markNow();
        if (cancelViolation > 0) {
            cancelViolation--;
            timerDuration = KotlinTimeAccess.subtractDurations(timerDuration,
                    DurationConstants.getFiftyMilliseconds());
            return false;
        }

        long packetInterval = KotlinTimeAccess.subtractDurations(
                DurationConstants.getFiftyMilliseconds(),
                KotlinTimeAccess.elapsedNow(lastTimeMark));
        packetInterval = KotlinTimeAccess.subtractDurations(packetInterval,
                DurationKt.toDuration(3, DurationUnit.NANOSECONDS));
        timerDuration = KotlinTimeAccess.addDurations(timerDuration, packetInterval);
        lastTimeMark = currentMark;

        long previousMark = durationTracker.advanceMark();
        boolean shouldReport = durationTracker.isDurationAtOrBelowThreshold(currentMark);
        boolean expired = KotlinTimeAccess.compareMarks(previousMark,
                KotlinTimeAccess.subtractDuration(currentMark,
                        DurationKt.toDuration(1, DurationUnit.SECONDS))) >= 0
                && KotlinTimeAccess.compareMarks(previousMark, currentMark) <= 0;
        if (expired) timerDuration = DurationKt.toDuration(0, DurationUnit.SECONDS);

        if (KotlinTimeAccess.compareMarks(
                previousMark, KotlinTimeAccess.addDurationToMark(currentMark,
                        DurationConstants.getFortyMilliseconds())) > 0 && shouldReport) {
            long adjustment = KotlinTimeAccess.subtractMarks(previousMark, currentMark);
            String advantage = KotlinTimeAccess.durationToString(adjustment);
            String server = KotlinTimeAccess.durationToString(timerDuration);
            long synchronizationMark = durationTracker.elapsedDuration(currentMark);
            String synchronization = KotlinTimeAccess.durationToString(synchronizationMark);
            String violationDetail = formatAdvantage(advantage, server, synchronization);
            flag("bp.timer", violationDetail, 1);
            movementBudget = Math.min(movementBudget + 1, 10);
            durationTracker.subtractFromMark(DurationConstants.getTenMilliseconds());
        } else if (!shouldReport) {
            durationTracker.setMarkIfLater(currentMark);
        } else if (KotlinTimeAccess.compareDurations(
                KotlinTimeAccess.subtractMarks(currentMark, previousMark),
                DurationKt.toDuration(5, DurationUnit.SECONDS)) > 0
                && KotlinTimeAccess.compareDurations(timerDuration,
                        DurationKt.toDuration(1, DurationUnit.SECONDS)) > 0) {
            String violationDetail = formatServerOnly(KotlinTimeAccess.durationToString(timerDuration));
            flag("bp.timer", violationDetail, 1);
            movementBudget = Math.min(movementBudget + 1, 5);
        }
        return false;
    }

}
