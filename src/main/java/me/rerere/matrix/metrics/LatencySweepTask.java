package me.rerere.matrix.metrics;

import java.util.HashMap;
import java.util.Map;
import me.rerere.matrix.violation.Violation;
import me.rerere.matrix.checks.CheckCategory;
import me.rerere.matrix.player.PlayerSessionRegistry;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.checks.player.ViolationTotals;

/** Periodically forwards each tracked metric after its configured sample count. */
public final class LatencySweepTask implements Runnable {
    private final PlayerSessionRegistry sessions;
    private final HashMap<CheckCategory, Integer> samplesSinceDecay;

    public LatencySweepTask(PlayerSessionRegistry sessions) {
        this.sessions = sessions;
        this.samplesSinceDecay = new HashMap<CheckCategory, Integer>();
    }

    @Override
    public void run() {
        for (PlayerCheckContext playerState : sessions.sessions().values()) {
            ViolationTotals samples = playerState.playerStorage();
            Map<CheckCategory, Violation> sampleMap = samples.totals();
            for (Map.Entry<CheckCategory, Violation> entry : sampleMap.entrySet()) {
                CheckCategory category = entry.getKey();
                Violation sample = entry.getValue();
                Integer previousCount = samplesSinceDecay.get(category);
                int sampleCount = (previousCount == null ? 0 : previousCount.intValue()) + 1;
                samplesSinceDecay.put(category, Integer.valueOf(sampleCount));
                int decayInterval = Math.max(category.getDecayInterval(), 1);
                if (sampleCount >= decayInterval) {
                    samplesSinceDecay.put(category, Integer.valueOf(0));
                    sample.subtract(category.getDecayAmount());
                }
            }
        }
    }
}
