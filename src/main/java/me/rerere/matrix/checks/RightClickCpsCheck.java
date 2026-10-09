package me.rerere.matrix.checks;

import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.time.MillisecondSampler;

/** Counts right-click interactions in fixed time windows. */
public final class RightClickCpsCheck extends CheckRuntimeState {
    private long windowStartedAt;
    private int clickCount;
    private static boolean enabled;
    private static int maximumCps;
    private static int violationWeight;
    private static final long WINDOW_MILLIS = 970L;

    public RightClickCpsCheck(PlayerCheckContext playerContext) {
        super(playerContext);
        this.setCheckType(CheckCategory.SCAFFOLD);
    }

    @Override
    public void reloadConfiguration() {
        me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection section = this.configuration();
        enabled = section.getBoolean("modules.right_cps.enable", true);
        violationWeight = this.configuration().getInt("modules.right_cps.vl_weight", 2);
        maximumCps = this.configuration().getInt("modules.right_cps.max_cps", 29);
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!enabled) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        clickCount++;
        long currentTimeMillis = MillisecondSampler.currentTimeMillis();
        long elapsed = currentTimeMillis - windowStartedAt;
        if (elapsed < WINDOW_MILLIS) return;
        if (clickCount > maximumCps) {
            long reportTime = MillisecondSampler.currentTimeMillis();
            long reportedElapsedMillis = reportTime - windowStartedAt;
            String message = formatViolation(clickCount, reportedElapsedMillis);
            this.flag("sfd.clk", message, violationWeight);
        }
        windowStartedAt = MillisecondSampler.currentTimeMillis();
        clickCount = 0;
    }

    private static String formatViolation(int clicks, long elapsedMillis) {
        return new StringBuilder("right click too fast (cps: ").append(clicks)
                .append(" length: ").append(elapsedMillis).append(")").toString();
    }
}
