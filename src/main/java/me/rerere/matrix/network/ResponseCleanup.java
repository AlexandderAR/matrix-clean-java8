package me.rerere.matrix.network;

import java.io.InputStream;
import java.net.ProxySelector;
import me.rerere.matrix.thirdparty.kotlin.io.CloseableKt;
import me.rerere.matrix.util.Exceptions;

/** Closes a response body and restores proxy state through the supplied callbacks. */
public final class ResponseCleanup {
    private ResponseCleanup() {}

    public interface Outer {
        void success(ProxySelector selector) throws Throwable;
        void failure(Throwable failure, ProxySelector selector) throws Throwable;
    }
    public interface PreCloseBody { void run(InputStream stream) throws Throwable; }
    public interface RecoveryBoundary { boolean isUnresolved(Throwable failure); }

    public static final class ProductionOuter implements Outer {
        @Override public void success(ProxySelector selector) {
            ProxySelectorRestoration.restoreAfterSelectedSuccess(selector);
        }
        @Override public void failure(Throwable failure, ProxySelector selector) {
            try {
                OuterFailureHandler.wrapRestoreAndRethrow(failure, selector);
            } catch (Throwable propagated) {
                throw Exceptions.propagate(propagated);
            }
        }
    }

    public static void run(InputStream stream, ProxySelector selector,
                           PreCloseBody preClose, RecoveryBoundary recovery,
                           Outer outer) throws Throwable {
        if (preClose == null || recovery == null || outer == null)
            throw new IllegalArgumentException("missing recovery integration");
        try {
            preClose.run(stream);
        } catch (Throwable failure) {
            if (recovery.isUnresolved(failure)) throw failure;
            Throwable closeFailure = null;
            try { CloseableKt.closeFinally(stream, failure); }
            catch (Throwable close) { closeFailure = close; }
            if (closeFailure != null) {
                outer.failure(closeFailure, selector);
                throw new AssertionError("selected outer failure returned");
            }
            outer.failure(failure, selector);
            throw new AssertionError("selected outer failure returned");
        }
        try {
            CloseableKt.closeFinally(stream, null);
        } catch (Throwable closeFailure) {
            outer.failure(closeFailure, selector);
            throw new AssertionError("selected outer failure returned");
        }
        outer.success(selector);
    }
}
