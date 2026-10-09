package me.rerere.matrix.model;

/** Immutable key/value parameter used by command parsing. */
public final class Parameter {
    private static boolean enabled;
    private final String key;
    private final String value;

    public Parameter(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public String component1() { return key; }
    public String getValue() { return value; }
    public String component2() { return value; }

    public Parameter copy(String key, String value) {
        return new Parameter(key, value);
    }

    public static Parameter copyDefault(Parameter self, String key, String value, int mask) {
        if ((mask & 1) != 0) key = self.key;
        if ((mask & 2) != 0) value = self.value;
        return self.copy(key, value);
    }

    private static String format(String key, String value) {
        return "Param(key=" + key + ", value=" + value + ")";
    }

    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }
    public static boolean isDisabled() { return !enabled; }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Parameter)) return false;
        Parameter that = (Parameter) other;
        return java.util.Objects.equals(key, that.key)
                && java.util.Objects.equals(value, that.value);
    }

    @Override public int hashCode() {
        return key.hashCode() * 31 + value.hashCode();
    }

    @Override public String toString() { return format(key, value); }
}
