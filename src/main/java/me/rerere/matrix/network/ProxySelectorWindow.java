package me.rerere.matrix.network;

import java.net.ProxySelector;

/** Captures the process-wide proxy selector and temporarily clears it. */
public final class ProxySelectorWindow {
    private ProxySelectorWindow() {
    }

    public static ProxySelector captureAndClear(Actions actions) {
        ProxySelector previous = actions.getDefault();
        actions.setDefault(null);
        return previous;
    }

    public static ProxySelector captureAndClear() {
        ProxySelector previous = ProxySelector.getDefault();
        ProxySelector.setDefault(null);
        return previous;
    }

    public interface Actions {

        ProxySelector getDefault();

        void setDefault(ProxySelector selector);
    }
}
