package me.rerere.matrix.crypto;

import java.lang.management.ManagementFactory;
import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.runtime.JvmArgumentMatch;
import me.rerere.matrix.runtime.JvmArgumentScanner;
import me.rerere.matrix.crypto.ProviderServiceScanner;
import me.rerere.matrix.network.PrefaceFieldDecoder;

/** Continues the preface path after its native prologue. */
public final class RuntimePrefaceBootstrap {
    private RuntimePrefaceBootstrap() {
        super();
    }

    public static PrefaceFieldDecoder.Fields afterNativePrologue(
            PrefaceRuntimeContext nativePrologue) {
        java.lang.management.RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        JvmArgumentMatch match = JvmArgumentScanner.scan(runtime);
        if (match != JvmArgumentMatch.NONE) {
            throw new UnresolvedNativePath(
                    "native JVM-argument match action is outside selected path");
        }
        ProviderServiceScanner.scanSystemProviders();
        return PrefaceFieldDecoder.afterRuntimeGuards(nativePrologue);
    }

    public static final class UnresolvedNativePath extends Error {
        public UnresolvedNativePath(String message) {
            super(message);
        }
    }
}
