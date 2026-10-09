package me.rerere.matrix.violation;

import java.util.Comparator;
import java.util.Map;
import me.rerere.matrix.checks.player.ViolationTotals;
import me.rerere.matrix.player.PlayerCheckContext;

/** Orders player state entries by descending total violation level. */
public final class ViolationLevelComparator implements Comparator<Object> {
    public ViolationLevelComparator() { }

    @Override
    public int compare(Object left, Object right) {
        Object rightValue = ((Map.Entry<?, ?>) right).getValue();
        int rightRank = ((ViolationTotals) ((PlayerCheckContext) rightValue).playerStorage())
                .sumTotals();
        Object leftValue = ((Map.Entry<?, ?>) left).getValue();
        int leftRank = ((ViolationTotals) ((PlayerCheckContext) leftValue).playerStorage())
                .sumTotals();
        return Integer.compare(rightRank, leftRank);
    }
}
