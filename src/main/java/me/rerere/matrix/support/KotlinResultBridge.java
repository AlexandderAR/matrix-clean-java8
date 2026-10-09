package me.rerere.matrix.support;

import me.rerere.matrix.thirdparty.kotlin.Result$Failure;

public final class KotlinResultBridge {
    private KotlinResultBridge() {}

    public static Object construct(Object value) {
        return value;
    }

    public static Throwable exceptionOrNull(Object result) {
        return result instanceof Result$Failure ? ((Result$Failure) result).exception : null;
    }
}
