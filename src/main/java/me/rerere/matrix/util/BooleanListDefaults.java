package me.rerere.matrix.util;


/** Stores the two Boolean values used by the object's list adapter. */
public final class BooleanListDefaults {
    private static final DelegatingMutableList<Boolean> DEFAULTS;
    private static boolean enabled;

    static {
        if (!isDisabled()) {
            setEnabled(true);
        }
        @SuppressWarnings("unchecked")
        DelegatingMutableList<Boolean> defaults = (DelegatingMutableList<Boolean>)
                (DelegatingMutableList<?>) DelegatingMutableList.companion.mutableCopy(Boolean.TRUE, Boolean.FALSE);
        DEFAULTS = defaults;
    }

    private BooleanListDefaults() {
        super();
    }

    public static final DelegatingMutableList<Boolean> getDefaults() {
        return DEFAULTS;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean isDisabled() {
        return !isEnabled();
    }
}
