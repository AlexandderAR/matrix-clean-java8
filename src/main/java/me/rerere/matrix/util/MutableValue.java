package me.rerere.matrix.util;

public class MutableValue {
    private Object containedValue;

    public MutableValue() {
        containedValue = null;
    }

    public <T> T get(Class<T> type) {
        return type.cast(containedValue);
    }

    public boolean canEqual(Object other) {
        return other instanceof MutableValue;
    }

    public void set(Object value) {
        containedValue = value;
    }

    @Override public boolean equals(Object candidate) {
        if (candidate == this) return true;
        if (!(candidate instanceof MutableValue)) return false;
        MutableValue other = (MutableValue) candidate;
        if (!other.canEqual(this)) return false;
        Object thisValue = containedValue;
        Object otherValue = other.containedValue;
        return thisValue == null
                ? otherValue == null
                : thisValue.equals(otherValue);
    }

    @Override public int hashCode() {
        Object value = containedValue;
        return 59 + (value == null ? 43 : value.hashCode());
    }
}
