package me.rerere.matrix.util;

/** Inclusive cursor that wraps to its starting value when the next value is outside its bounds. */
public final class IntRangeCursor {
    private int currentValue;
    private final int direction;
    private final int startValue;
    private final int endValue;
    private static final String ERROR_MESSAGE = "from and to must be different and in short range".intern();

    public IntRangeCursor(int from, int to) {
        this.startValue = from;
        this.endValue = to;
        this.direction = from < to ? 1 : -1;
        if (!isInShortRange(from) || !isInShortRange(to) || from == to) {
            throw new IllegalStateException(ERROR_MESSAGE);
        }
        this.currentValue = from;
    }

    private static boolean isInShortRange(int value) {
        return me.rerere.matrix.util.PingManagerSupport.fitsSignedShort(value);
    }

    public int upperBound() { return endValue; }
    public int lowerBound() { return startValue; }
    public int currentValue() { return currentValue; }

    /** Returns the prior value, then advances or wraps to the range's lower endpoint. */
    public int advance() {
        int previous = currentValue;
        int candidate = currentValue + direction;
        boolean valid = candidate <= endValue && candidate >= startValue;
        currentValue = valid ? candidate : startValue;
        return previous;
    }

    public int normalizeCandidate(int value) {
        int candidate = value + direction;
        return candidate >= startValue && candidate <= endValue ? candidate : startValue;
    }
}
