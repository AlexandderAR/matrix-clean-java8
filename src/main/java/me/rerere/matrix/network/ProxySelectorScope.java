package me.rerere.matrix.network;

import java.net.ProxySelector;

/** Clears the default proxy selector before preparation and restores it only if preparation fails. */
public final class ProxySelectorScope {
    private ProxySelectorScope() {
    }

    public static <T> T run(Actions<T> actions) throws Throwable {
        ProxySelector previous = actions.getDefault();
        try {
            actions.setDefault(null);
            return actions.prepare(previous);
        } catch (Throwable failure) {
            actions.setDefault(previous);
            throw failure;
        }
    }

    public interface Actions<T> {
        ProxySelector getDefault() throws Throwable;
        void setDefault(ProxySelector selector) throws Throwable;
        T prepare(ProxySelector previous) throws Throwable;
    }
}

