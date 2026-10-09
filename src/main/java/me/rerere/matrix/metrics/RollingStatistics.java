package me.rerere.matrix.metrics;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.util.collections.CollectionSupport;
import me.rerere.matrix.compat.FoliaSupport;

/** Maintains the rolling scheduler interval sample and exposes a capped metric. */
public final class RollingStatistics extends BukkitRunnable {
    private static double currentMetric;
    private static long lastClock;
    private static final Deque<Long> intervals;
    private static final DecimalFormat formatter;
    public static final RollingStatistics INSTANCE;
    private static boolean enabled;

    private RollingStatistics() {
        super();
    }

    private static void refreshMetric() {
        currentMetric = calculateMetric();
    }

    private double averageIntervals() {
        int totalMillis = 0;
        Iterator<Long> iterator = intervals.iterator();
        while (iterator.hasNext()) {
            totalMillis += iterator.next().longValue();
        }
        return (double) totalMillis / 40.0;
    }

    public static double calculateMetric() {
        try {
            double average = INSTANCE.averageIntervals();
            double result = Double.parseDouble(formatter.format(1000.0 / average));
            return me.rerere.matrix.thirdparty.kotlin.ranges.RangesKt.coerceAtMost(result, 20.0);
        } catch (Exception failure) {
            return 20.0;
        }
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastClock;
        lastClock = now;
        CollectionSupport.removeFirst(intervals);
        CollectionSupport.append(intervals, Long.valueOf(elapsed));
    }

    public static double metric() {
        return currentMetric;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static boolean disabled() {
        return !enabled();
    }

    static {
        if (!enabled()) {
            setEnabled(true);
        }
        INSTANCE = new RollingStatistics();
        formatter = new DecimalFormat("0.00");
        currentMetric = 20.0;
        lastClock = System.currentTimeMillis();
        intervals = new ArrayDeque<Long>(Collections.nCopies(40, Long.valueOf(50L)));
        if (!FoliaSupport.INSTANCE.isUsingFolia()) {
            Plugin plugin = (Plugin) Matrix.Companion.matrixInstance();
            INSTANCE.runTaskTimer(plugin, 1L, 1L);
            BukkitScheduler scheduler = Bukkit.getScheduler();
            Plugin asyncPlugin = (Plugin) Matrix.Companion.matrixInstance();
            scheduler.runTaskTimerAsynchronously(asyncPlugin, RollingStatistics::refreshMetric, 0L, 40L);
        }
    }
}
