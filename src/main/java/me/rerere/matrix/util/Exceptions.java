package me.rerere.matrix.util;

/** Preserves the original exception when crossing a callback without checked throws. */
public final class Exceptions {
    private Exceptions() {
    }

    public static RuntimeException propagate(Throwable failure) {
        Exceptions.<RuntimeException>throwUnchecked(failure);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <Failure extends Throwable> void throwUnchecked(Throwable failure) throws Failure {
        throw (Failure) failure;
    }
}
