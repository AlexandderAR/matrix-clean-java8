package me.rerere.matrix.checks.player;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.Location;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.commands.DeveloperCommand;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.runtime.MatrixTasks;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.util.RuntimeGuard;

/** Tracks same-position rotation activity while a player mines. */
public final class RotationMiningCheck extends CheckRuntimeState {
    private static boolean recording;
    private static boolean monitorEnabled;
    public static final RuntimeGuard angle = new RuntimeGuard();
    private long lastBlockBreakAt;
    private double rotationBuffer;
    private int rotationCount;

    public RotationMiningCheck(PlayerCheckContext playerData) {
        super(playerData, CheckCategory.AUTOBOT);
        lastBlockBreakAt = System.currentTimeMillis();
    }

    public void onMove(PlayerMoveEvent event, Location from, Location to) {
        float yawDelta = GeometryMath.angularDistance(from.getYaw(), to.getYaw());
        float pitchDelta = GeometryMath.angularDistance(from.getPitch(), to.getPitch());
        if (yawDelta > 0.0F
                && pitchDelta == 0.0F
                && Math.abs(to.getPitch()) > 10.0F
                && from.distance(to) == 0.0) {
            rotationCount++;
        }
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SURVIVAL && recording) {
            double elapsedSeconds = (System.currentTimeMillis() - lastBlockBreakAt) / 1000.0;
            if (elapsedSeconds < 1.0) {
                double rotationsPerSecond = rotationCount / elapsedSeconds;
                rotationBuffer += rotationsPerSecond - 10.0;
                rotationBuffer = me.rerere.matrix.thirdparty.kotlin.ranges.RangesKt
                        .coerceIn(rotationBuffer, -50.0, 50.0);

                if (DeveloperCommand.debugBaritone) {
                    String summary = rotationSummary(rotationCount, rotationsPerSecond,
                            elapsedSeconds, rotationBuffer);
                    MatrixTasks.broadcast(summary);
                }

                if (rotationBuffer > 35.0) {
                    Map<String, Object> details = new LinkedHashMap<>();
                    details.put("type", "#mine_rots");
                    details.put("d", DiagnosticFormat.formatDouble(elapsedSeconds));
                    details.put("r", Integer.toString(rotationCount));
                    details.put("buf", DiagnosticFormat.formatDouble(rotationBuffer));
                    boolean violationRecorded = flag(
                            "bot.baritone", "baritone like actions", details, 3);
                    if (violationRecorded) {
                        event.setCancelled(true);
                        rotationBuffer -= 10.0;
                    }
                }
            }
        }
        rotationCount = 0;
        lastBlockBreakAt = System.currentTimeMillis();
    }

    public static void setRecording(boolean enabled) { recording = enabled; }

    public static boolean recording() { return recording; }
    public static void setMonitorEnabled(boolean enabled) { monitorEnabled = enabled; }
    public static boolean monitorEnabled() { return monitorEnabled; }
    public static boolean monitorDisabled() { return !monitorEnabled(); }

    private static String rotationSummary(int count, double rate, double delaySeconds,
                                          double buffer) {
        return "#" + count + " rots | rate: " + rate + " | delay: "
                + delaySeconds + " s | buf: " + buffer;
    }

    static {
        monitorEnabled = true;
    }
}
