package me.rerere.matrix.network;

import java.net.ProxySelector;
import me.rerere.matrix.license.LicenseResponseException;

/** Restores proxy configuration before propagating the HTTPS failure. */
public final class OuterFailureHandler {
    private OuterFailureHandler() {
    }

    public static void wrapRestoreAndRethrow(Throwable failure, ProxySelector selector) throws Throwable {
        run(failure, selector, new Production());
    }

    public static void run(Throwable failure, ProxySelector selector, Effects effects) throws Throwable {
        try {
            effects.print(failure);
            throw effects.replacement();
        } catch (Throwable replacementFailure) {
            effects.restore(selector);
            throw replacementFailure;
        }
    }

    public static void restoreAndRethrow(Throwable failure, ProxySelector selector, Effects effects)
            throws Throwable {
        effects.restore(selector);
        throw failure;
    }

    public interface Effects {
        void print(Throwable failure) throws Throwable;
        Throwable replacement() throws Throwable;
        void restore(ProxySelector selector) throws Throwable;
    }

    public static final class Production implements Effects {
        public Production() {
        }

        @Override
        public void print(Throwable failure) {
            failure.printStackTrace();
        }

        @Override
        public Throwable replacement() {
            return new LicenseResponseException("HTTPS ERROR! Check Your Network Connection!");
        }

        @Override
        public void restore(ProxySelector selector) {
            ProxySelector.setDefault(selector);
        }
    }
}
