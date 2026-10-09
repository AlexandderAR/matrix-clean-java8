package me.rerere.matrix.version;

import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.com.github.retrooper.packetevents.manager.server.ServerVersion;
import me.rerere.matrix.compat.ServerVersionSwitchMap;
import me.rerere.matrix.Matrix;

/** Lazily computes the packet protocol bucket for the current server version. */
public final class ProtocolVersionBucket {
    public static final ProtocolVersionBucket INSTANCE = new ProtocolVersionBucket();
    private static final Lazy<Integer> CACHED_BUCKET = LazyKt.lazy(new Function0<Integer>() {
        @Override
        public Integer invoke() {
            return Integer.valueOf(computeBucket());
        }
    });

    private ProtocolVersionBucket() { }

    private static int computeBucket() {
        ServerVersion current = Matrix.Companion.serverVersion();
        int indexCategory = ServerVersionSwitchMap.LEGACY_CASES[current.ordinal()];
        if (indexCategory == 1) {
            return 6;
        }

        int throughOneFifteen = current.compareTo(ServerVersion.V_1_15_2);
        if (throughOneFifteen <= 0 && current.compareTo(ServerVersion.V_1_12) >= 0) {
            return 7;
        }

        if (current.compareTo(ServerVersion.V_1_16_5) > 0) {
            return 9;
        }
        return current.compareTo(ServerVersion.V_1_14) >= 0 ? 8 : 9;
    }

    public int cachedBucket() {
        return CACHED_BUCKET.getValue().intValue();
    }

}
