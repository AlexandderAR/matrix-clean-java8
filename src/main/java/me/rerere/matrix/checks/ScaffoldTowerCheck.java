package me.rerere.matrix.checks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffectType;

import me.rerere.matrix.util.PotionEffects;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.util.GeometryMath;
import me.rerere.matrix.util.time.MillisecondSampler;
import me.rerere.matrix.util.NumberUtils;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.metrics.RollingStatistics;
import me.rerere.matrix.world.BlockEnvironment;
import me.rerere.matrix.thirdparty.org.jetbrains.annotations.NotNull;

public class ScaffoldTowerCheck extends CheckRuntimeState {
    private int previousBlockY;
    private int previousBlockX;
    private int previousBlockZ;
    private double previousPlayerY;
    private static int cancelViolationLevel;
    private static int towerPenalty;
    private static boolean enabled;
    private final Queue<Long> placementIntervals;
    private long lastTowerPlacementTime;
    private long lastHorizontalPlacementTime;
    private static String message;

    public ScaffoldTowerCheck(PlayerCheckContext player) {
        super(player);
        lastTowerPlacementTime = MillisecondSampler.currentTimeMillis();
        placementIntervals = new ArrayDeque<>();
        setCheckType(CheckCategory.SCAFFOLD);
    }
    @Override
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        if (event.isCancelled() || !enabled) return;
        Block placed = event.getBlockPlaced();
        Player player = event.getPlayer();
        if (player.isFlying()) return;
        if (Matrix.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_12)
                && player.hasPotionEffect(PotionEffectType.LEVITATION)) return;
        if (BlockEnvironment.hasFaPredicateAroundCurrentAndAbove(player.getLocation())) return;
        if (player.hasPotionEffect(PotionEffects.singleton.getJumpBoost())) return;
        Block against = event.getBlockAgainst();
        if (placed.getY() == previousBlockY + 1 && placed.getX() == previousBlockX && placed.getZ() == previousBlockZ
                && against.getX() == placed.getX() && against.getZ() == placed.getZ()
                && against.getY() == placed.getY() - 1) {
            Pair<Double, Double> distanceMetrics = distancesToBlock(player, placed);
            // Pair order is (3D distance, horizontal distance).
            if (distanceMetrics.getSecond() < 0x1.2147ae147ae14p0
                    && distanceMetrics.getFirst() < 0x1.5c28f5c28f5c3p0) {
                Location location = player.getLocation();
                if (location.getY() > placed.getY() && location.getY() <= placed.getY() + 2
                        && location.getY() != previousPlayerY) {
                    evaluateTower(event, placed, distanceMetrics);
                }
            }
        }
        if (placed.getY() == against.getY()) lastHorizontalPlacementTime = MillisecondSampler.currentTimeMillis();
        previousBlockX = placed.getX();
        previousBlockY = placed.getY();
        previousBlockZ = placed.getZ();
        previousPlayerY = player.getLocation().getY();
    }

    private Pair<Double, Double> distancesToBlock(@NotNull Player player, @NotNull Block block) {
        Location location = player.getLocation();
        double deltaX = block.getX() + 0.5 - location.getX();
        double deltaY = block.getY() + 0.5 - location.getY();
        double deltaZ = block.getZ() + 0.5 - location.getZ();
        double distance3d = Math.sqrt(Math.pow(deltaX, 2.0)
                + Math.pow(deltaY, 2.0) + Math.pow(deltaZ, 2.0));
        double horizontalDistance = Math.sqrt(Math.pow(deltaX, 2.0) + Math.pow(deltaZ, 2.0));
        return new Pair<>(distance3d, horizontalDistance);
    }

    private void evaluateTower(@NotNull BlockPlaceEvent event, @NotNull Block placed,
                               @NotNull Pair<Double, Double> distanceMetrics) {
        long now = MillisecondSampler.currentTimeMillis();
        long elapsed = now - lastTowerPlacementTime;
        lastTowerPlacementTime = now;
        placementIntervals.add(elapsed);
        if (placementIntervals.size() < 5) return;
        double average = (double) GeometryMath.roundedLongAverage(
                new ArrayList<>(placementIntervals));
        long minimumInterval = 297L - (MillisecondSampler.currentTimeMillis() - lastHorizontalPlacementTime <= 500L ? 200L : 0L);
        if (average < minimumInterval && playerState().latencyMilliseconds() < 160L
                && RollingStatistics.metric() > 19.0) {
            String reason = formatTowerReason(elapsed, minimumInterval,
                    NumberUtils.round3(distanceMetrics.getFirst()),
                    NumberUtils.round3(distanceMetrics.getSecond()),
                    average, placed.getType().isBlock());
            flag("sfd.twr", reason, towerPenalty);
            if (violationCount() >= cancelViolationLevel) {
                event.setBuild(false);
                event.setCancelled(true);
            }
        }
        placementIntervals.poll();
    }

    private static String formatTowerReason(long elapsed, long minimumInterval,
            double distance, double horizontalDistance, double average, boolean block) {
        return new StringBuilder("(tower) quickly build blocks up (t: ").append(elapsed).append('/')
                .append(minimumInterval).append(", d_n: ").append(distance).append(", d_h: ")
                .append(horizontalDistance).append(", avg: ").append(average)
                .append(", block: ").append(block).append(')').toString();
    }

    public static void setMessage(String value) { message = value; }
    public static String getMessage() { return message; }

    @Override
    public void reloadConfiguration() {
        cancelViolationLevel = configuration().getInt("cancel_vl", 10);
        enabled = configuration().getBoolean("modules.tower.enable", true);
        towerPenalty = configuration().getInt("modules.tower.vl_weight");
    }
}
