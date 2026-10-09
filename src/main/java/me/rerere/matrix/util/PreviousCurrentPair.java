package me.rerere.matrix.util;

/** Mutable previous/current values with the bytecode's two update modes. */
public final class PreviousCurrentPair {
    private Object previous;
    private Object current;

    public PreviousCurrentPair(Object initial) {
        this.current = initial;
        this.previous = initial;
    }

    /** Shift current into previous, then install the supplied current value. */
    public void advance(Object value) {
        this.previous = this.current;
        this.current = value;
    }

    public Object previous() {
        return previous;
    }

    /** Set both values to the same object. */
    public void reset(Object value) {
        this.previous = value;
        this.current = value;
    }

    public Object current() {
        return current;
    }
}
