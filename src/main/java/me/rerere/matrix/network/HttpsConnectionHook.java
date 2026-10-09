package me.rerere.matrix.network;

import javax.net.ssl.HttpsURLConnection;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;

/** Kotlin function adapter for HTTPS connection values. */
public final class HttpsConnectionHook implements Function1<Object, Unit> {
    public static final HttpsConnectionHook INSTANCE;
    private static int backingValue;

    static {
        if (getBackingValue() != 0) setBackingValue(56);
        INSTANCE = new HttpsConnectionHook();
    }

    public HttpsConnectionHook() {}

    @Override
    public Unit invoke(Object connection) {
        apply((HttpsURLConnection) connection);
        return Unit.INSTANCE;
    }

    public final void apply(HttpsURLConnection connection) {
    }

    public static void setBackingValue(int value) { backingValue = value; }
    public static int getBackingValue() { return backingValue; }

    public static int getGuardedValue() {
        int value = getBackingValue();
        if (value == 0) return 48;
        return 0;
    }
}
