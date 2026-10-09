package me.rerere.matrix.compat;

import me.rerere.matrix.network.PlayerMetadataPacketListener;

/** Static Matrix collection holder with two packet-side delegate entry points. */
public final class MatrixFeatureBridge {
    private MatrixFeatureBridge() { }

    public MatrixFeatureBridge(me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker marker) {
        this();
    }

    public final void setEnabled(boolean enabled) {
        PlayerMetadataPacketListener.setListenerEnabled(enabled);
    }

    public final boolean isFeatureEnabled() {
        return PlayerMetadataPacketListener.isListenerEnabled();
    }
}
