package me.rerere.matrix.network;

import java.net.ProxySelector;
import me.rerere.matrix.thirdparty.kotlin.ResultKt;
import me.rerere.matrix.thirdparty.kotlin.Unit;

/** Restores the JVM default proxy selector after the selected successful path. */
public final class ProxySelectorRestoration {
    private ProxySelectorRestoration() {
    }

    public static ProxySelector capturePreviousDefault() {
        return ProxySelector.getDefault();
    }

    public static void restoreAfterSelectedSuccess(ProxySelector previous) {
        ProxySelector.setDefault(previous);
        ResultKt.throwOnFailure(Unit.INSTANCE);
    }
}
