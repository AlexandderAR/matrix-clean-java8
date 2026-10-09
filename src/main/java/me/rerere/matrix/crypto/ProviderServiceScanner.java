package me.rerere.matrix.crypto;

import java.security.Provider;
import java.security.Security;
import java.util.Iterator;
import java.util.Set;

/** Enumerates provider services; the selected AES cipher mismatch path is unresolved. */
public final class ProviderServiceScanner {
    public static final class UnresolvedNativePath extends Error {
        public UnresolvedNativePath(String message) { super(message); }
    }

    private ProviderServiceScanner() {}

    public static void scanSystemProviders() {
        scanProvided(Security.getProviders());
    }

    public static void scanProvided(Provider[] providers) {
        if (providers == null) throw new UnresolvedNativePath("null provider array outside selected JNI path");
        for (Provider provider : providers) {
            if (provider == null) throw new UnresolvedNativePath("null provider outside selected Provider cast path");
            Set<Provider.Service> services = provider.getServices();
            Iterator<Provider.Service> iterator = services.iterator();
            while (iterator.hasNext()) {
                Provider.Service service = iterator.next();
                ProviderServiceClassifier.Classification classification =
                    ProviderServiceClassifier.selectedService(service);
                if (classification == ProviderServiceClassifier.Classification.UNRECOGNIZED_AES_CIPHER) {
                    throw new UnresolvedNativePath("native service mismatch enters 0x70c2a");
                }
            }
        }
    }
}
