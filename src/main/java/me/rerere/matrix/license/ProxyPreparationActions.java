package me.rerere.matrix.license;

import java.net.ProxySelector;
import me.rerere.matrix.crypto.PreparedNormalEntry;
import me.rerere.matrix.network.ProxySelectorScope;

/** Captures the token and receiver while ProxySelectorScope temporarily changes the default. */
public final class ProxyPreparationActions implements ProxySelectorScope.Actions<PreparedNormalEntry.Prepared> {
    private final String token;
    private final me.rerere.matrix.license.LicenseRuntimeBridge receiver;

    public ProxyPreparationActions(String token, me.rerere.matrix.license.LicenseRuntimeBridge receiver) {
        this.token = token;
        this.receiver = receiver;
    }

    @Override public ProxySelector getDefault() { return ProxySelector.getDefault(); }

    @Override public void setDefault(ProxySelector selector) {
        ProxySelector.setDefault(selector);
    }

    @Override
    public PreparedNormalEntry.Prepared prepare(ProxySelector previousSelector)
            throws Throwable {
        return LicenseRequestPreparer.prepare(token, previousSelector, receiver);
    }
}
