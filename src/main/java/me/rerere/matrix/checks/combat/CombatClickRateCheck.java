package me.rerere.matrix.checks.combat;

import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.event.PacketReceiveEvent;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketType$Play$Client;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.protocol.player.ClientVersion;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.wrapper.PacketWrapper;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.checks.movement.MovementMetrics;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;

/** Measures attack animation cadence, timing spread, and swing consistency. */
public final class CombatClickRateCheck extends CheckRuntimeState {
    private static int maximumClicksPerSecond;
    private static int timingWindowSize;
    private static boolean checkClickRate;
    private static boolean checkSwingOrder;
    private static boolean checkTimingSpread;
    private static boolean dynamicViolationWeight;

    private long lastClickTime;
    private long lastRateWindowTime;
    private int clicksInWindow;
    private boolean swingReceived;
    private double averagedClickRate;
    private int lastClickRate;
    private final List<Integer> clickIntervals = new ArrayList<Integer>();

    static {
        timingWindowSize = 20;
        maximumClicksPerSecond = 11;
    }

    public CombatClickRateCheck(PlayerCheckContext playerData) {
        super(playerData);
        averagedClickRate = 0.0D;
        clicksInWindow = 0;
        swingReceived = false;
        setCheckType(CheckCategory.CLICK);
    }

    /** Reads the check's settings from its inherited configuration section. */
    @Override
    public void reloadConfiguration() {
        ConfigurationSection config = configuration();
        maximumClicksPerSecond = config.getInt("max_cps", 11);
        timingWindowSize = config.getInt("buffer_size", 20);
        checkClickRate = config.getBoolean("check_cps", true);
        checkSwingOrder = config.getBoolean("check_swing", true);
        checkTimingSpread = config.getBoolean("check_timing", true);
        dynamicViolationWeight = config.getBoolean("dynamic_cps_vl_weight", true);
    }

    public double getAveragedClickRate() { return averagedClickRate; }
    public int getLastClickRate() { return lastClickRate; }

    private static String timingViolationMessage(int average, double standardDeviation) {
        return "exhibiting suspicious clicks patterns during the combat // timing // avg: "
                + average + ", stdDev: " + standardDeviation;
    }

    /** Reports a low-variance timing sample when the buffer is full. */
    private void inspectTimingWindow() {
        int averageInterval = MovementMetrics.integerAverage(clickIntervals);
        double squaredDeviation = 0.0D;
        for (Integer interval : clickIntervals) {
            squaredDeviation += Math.pow(interval.intValue() - averageInterval, 2.0D);
        }
        double standardDeviation = Math.sqrt(squaredDeviation / clickIntervals.size());
        if (averageInterval <= 2 && standardDeviation < 0.9D && checkTimingSpread) {
            double weight = NumberUtils.round3(standardDeviation);
            violation("ka.clk.tim", timingViolationMessage(averageInterval, weight), 8);
        }
    }

    private static String clickRateMessage(int actual, int maximum) {
        return "reached " + actual + "/" + maximum + " cps during the combat // cps";
    }

    /** Handles one entity-state pulse supplied by the parent packet bridge. */
    public void onCheckPulse(int entityId) {
        recordClick();
    }

    private void recordClick() {
        if (swingReceived && checkSwingOrder) {
        ClientVersion client = playerData().clientVersion();
        boolean legacyClientOnNewServer = client == ClientVersion.V_1_8
                    && Matrix.getServerVersion()
                    .isNewerThan(ServerVersion.V_1_8_8);
            if (!legacyClientOnNewServer) {
                violation("ka.clk.swg", swingViolationMessage(playerData().clientVersion().getReleaseName()), 3);
            }
        }
        swingReceived = true;

        long now = MillisecondSampler.currentTimeMillis();
        long elapsed = now - lastClickTime;
        if (elapsed >= 1500L) {
            lastClickTime = now;
            return;
        }

        int intervalTicks = Math.round((float) elapsed / 50.0F);
        clickIntervals.add(Integer.valueOf(intervalTicks));
        clicksInWindow++;
        if (now - lastRateWindowTime > 1000L) {
            averagedClickRate += clicksInWindow;
            averagedClickRate /= 2.0D;
            lastClickRate = clicksInWindow;
            if (clicksInWindow > maximumClicksPerSecond && checkClickRate) {
                if (dynamicViolationWeight) {
                    int level = 4 + Math.min(7, (clicksInWindow - maximumClicksPerSecond) / 2);
                    violation("ka.clk.cps", dynamicClickRateMessage(
                            clicksInWindow, maximumClicksPerSecond), level);
                } else {
                    violation("ka.clk.cps", clickRateMessage(
                            clicksInWindow, maximumClicksPerSecond), 4);
                }
            }
            lastRateWindowTime = now;
            clicksInWindow = 0;
        }

        if (clickIntervals.size() >= timingWindowSize) {
            inspectTimingWindow();
            clickIntervals.clear();
        }
        lastClickTime = now;
    }

    private static String dynamicClickRateMessage(int actual, int maximum) {
        return "reached " + actual + "/" + maximum + " cps during the combat // cps (dynamic)";
    }

    /** Packet callback resets the swing marker for the next attack. */
    public boolean onPacketReceive(PacketTypeCommon packetType, PacketReceiveEvent event,
                                   PacketWrapper wrapper) {
        if (packetType == PacketType$Play$Client.ANIMATION
                || packetType == PacketType$Play$Client.PUNCH) {
            swingReceived = false;
        }
        return false;
    }

    private static String swingViolationMessage(String releaseName) {
        return "attack other entities without clicking // swing // ver: " + releaseName;
    }

    private PlayerCheckContext playerData() {
        return (PlayerCheckContext) playerState();
    }

    private boolean violation(String key, String message, int weight) {
        return super.flag(key, message, weight);
    }
}
