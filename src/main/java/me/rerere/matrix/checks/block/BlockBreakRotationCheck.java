package me.rerere.matrix.checks.block;

import java.util.Map;
import java.util.LinkedHashMap;
import org.bukkit.Location;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.Vector;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.TuplesKt;
import me.rerere.matrix.thirdparty.kotlin.collections.MapsKt;
import me.rerere.matrix.thirdparty.kotlin.ranges.RangesKt;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.util.text.DiagnosticFormat;
import me.rerere.matrix.features.FeatureToggleBridge;

/** Tracks repeated block breaks performed at an implausible view angle. */
public final class BlockBreakRotationCheck extends CheckRuntimeState {
    private Vector previousDirection;
    private static boolean enabled;
    private static final long SUSPICIOUS_BREAK_LIMIT;
    public static final FeatureToggleBridge indexMarker;
    private int suspiciousBreaks;
    private static String reportLabel;

    static {
        SUSPICIOUS_BREAK_LIMIT = 6167416197735251988L;
        indexMarker = new FeatureToggleBridge();
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public BlockBreakRotationCheck(PlayerCheckContext playerData) {
        super(playerData, CheckCategory.BLOCK);
        this.previousDirection = new Vector(0.0D, 1.0D, 0.0D);
    }

    public void onBlockBreak(BlockBreakEvent event) {
        Vector playerDirection = event.getPlayer().getLocation().getDirection().normalize();
        Location blockCenter = event.getBlock().getLocation().clone().add(0.5D, 0.5D, 0.5D);
        Vector towardBlock = blockCenter.subtract(((CheckRuntimeState) this)
                .player().getEyeLocation()).toVector().normalize();
        double centerAngle = Math.toDegrees((double) playerDirection.angle(towardBlock));
        double previousAngle = Math.toDegrees((double) playerDirection.angle(previousDirection));

        boolean decayCounter = Double.isNaN(centerAngle) || centerAngle >= 1.5D;
        if (!decayCounter) {
            decayCounter = Double.isNaN(previousAngle) || previousAngle <= 20.0D;
        }
        int nextCount = decayCounter ? suspiciousBreaks - 2 : suspiciousBreaks + 1;
        suspiciousBreaks = RangesKt.coerceIn(nextCount, 0, (int) SUSPICIOUS_BREAK_LIMIT);

        if (suspiciousBreaks >= 3 && enabled) {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("a_c", DiagnosticFormat.formatDouble(centerAngle));
            details.put("a_p", DiagnosticFormat.formatDouble(previousAngle));
            if (((CheckRuntimeState) this).flag(
                    "blk.rot", "suspected breaking action", details, 4)) {
                event.setCancelled(true);
            }
        }
        this.previousDirection = playerDirection;
    }

    public static void setReportLabel(String value) {
        reportLabel = value;
    }

    public static String reportLabel() {
        return reportLabel;
    }

}
