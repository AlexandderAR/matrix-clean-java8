package me.rerere.matrix.util;

import me.rerere.matrix.util.reflect.ReflectionNameResolver;

/** Lazy probes for the Paper-specific class and arbitrary decoded class names. */
public final class PaperAvailability {
    public static final PaperAvailability INSTANCE;
    private static final String PAPER_CONFIG_CLASS;
    private static final me.rerere.matrix.thirdparty.kotlin.Lazy PAPER_CONFIG_PRESENT;

    static {
        PAPER_CONFIG_CLASS = "com.destroystokyo.paper.PaperConfig".intern();
        INSTANCE = new PaperAvailability();
        PAPER_CONFIG_PRESENT = me.rerere.matrix.thirdparty.kotlin.LazyKt.lazy(
                () -> Boolean.valueOf(hasPaperConfigClass()));
    }

    private PaperAvailability() { }

    public boolean isPaperConfigPresent() {
        return ((Boolean) PAPER_CONFIG_PRESENT.getValue()).booleanValue();
    }

    public boolean hasClass(String name) {
        try {
            Class.forName(ReflectionNameResolver.mapDescriptorName(name));
            return true;
        } catch (ClassNotFoundException absent) {
            return false;
        }
    }

    private static boolean hasPaperConfigClass() {
        try {
            Class.forName(PAPER_CONFIG_CLASS);
            return true;
        } catch (ClassNotFoundException absent) {
            return false;
        }
    }

}


