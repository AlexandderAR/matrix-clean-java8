package me.rerere.matrix.util.time;

import me.rerere.matrix.compat.KotlinTimeAccess;
import me.rerere.matrix.player.PlayerCheckContext;
import me.rerere.matrix.util.DurationConstants;
import me.rerere.matrix.thirdparty.kotlin.Unit;

/** Mutable state for marks measured by the monotonic clock. */
public final class MonotonicTimingState {
    private long accumulatedMark;
    private long lastMeasuredMark;
    private long savedMark;
    private long expirationBaseMark;
    public MonotonicTimingState() {
        long initial = KotlinTimeAccess.subtractDuration(
                KotlinTimeAccess.markNow(), DurationConstants.getThirtySeconds());
        accumulatedMark = initial;
        lastMeasuredMark = initial;
        savedMark = initial;
        expirationBaseMark = initial;
    }

    /** Keeps the earlier mark according to Kotlin's monotonic mark ordering. */
    public void setMarkIfLater(long candidate) {
        if (KotlinTimeAccess.compareMarks(accumulatedMark, candidate) > 0) {
            accumulatedMark = candidate;
        }
    }

    public void synchronizeFromTimer(PlayerCheckContext timer) {
        timer.transactionQueue().workWithCallbacks(
                () -> updateFromTimer(this, timer), MonotonicTimingState::noOpUnit);
    }

    private static Unit updateFromTimer(MonotonicTimingState state, PlayerCheckContext timer) {
        long nextMark = TimeMarkOperations.selectMark(state.accumulatedMark, state.savedMark);
        state.accumulatedMark = nextMark;
        state.lastMeasuredMark = timer.lastTransactionMark();
        state.expirationBaseMark = KotlinTimeAccess.markNow();
        return Unit.INSTANCE;
    }

    private static Unit noOpUnit() { return Unit.INSTANCE; }

    public void subtractFromMark(long duration) {
        accumulatedMark = KotlinTimeAccess.subtractDuration(accumulatedMark, duration);
    }

    /** With canonical xb.5() == false, returns durationBetweenMarks(...) <= threshold. */
    public boolean isDurationAtOrBelowThreshold(long mark) {
        long elapsed = KotlinTimeAccess.subtractMarks(mark, expirationBaseMark);
        long threshold = DurationConstants.getOneSecond();
        return KotlinTimeAccess.compareDurations(elapsed, threshold) <= 0;
    }

    public long advanceMark() {
        accumulatedMark = KotlinTimeAccess.addDurationToMark(
                accumulatedMark, DurationConstants.getFiftyMilliseconds());
        savedMark = lastMeasuredMark;
        return accumulatedMark;
    }

    public long elapsedDuration(long mark) {
        return KotlinTimeAccess.subtractMarks(lastMeasuredMark, mark);
    }

}
