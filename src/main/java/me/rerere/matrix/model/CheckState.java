package me.rerere.matrix.model;

/** Two integer attributes with the original class-level boolean switch. */
public final class CheckState {
    private final int primaryValue;
    private final int secondaryValue;
    private static boolean enabled;

    public CheckState(int primaryValue, int secondaryValue) {
        this.primaryValue = primaryValue;
        this.secondaryValue = secondaryValue;
    }

    public int primaryValue() { return primaryValue; }
    public int secondaryValue() { return secondaryValue; }
    public static void setEnabled(boolean value) { enabled = value; }
    public static boolean isEnabled() { return enabled; }
    public static boolean isDisabled() { return !isEnabled(); }

}
