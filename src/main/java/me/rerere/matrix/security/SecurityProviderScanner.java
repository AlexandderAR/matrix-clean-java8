package me.rerere.matrix.security;

import java.security.Provider;
import java.security.Security;
import java.util.Iterator;
import java.util.Set;
import me.rerere.matrix.crypto.ProviderServiceClassifier;
import me.rerere.matrix.crypto.ProviderServiceClassifier.Classification;
import me.rerere.matrix.license.ProviderMismatchRefusal;

/** Scans installed security providers for unrecognized AES cipher services. */
public final class SecurityProviderScanner {
    private static final Mismatch PRODUCTION_MISMATCH = new ProductionMismatch();

    private SecurityProviderScanner() { }

    public static void scanSystemProviders() {
        scanProvided(Security.getProviders(), PRODUCTION_MISMATCH);
    }

    public static void scanProvided(Provider[] providers, Mismatch mismatch) {
        if (providers == null) {
            throw new NullPointerException("\"ARRAYLENGTH npe\" on -1");
        }
        for (Provider provider : providers) {
            if (provider == null) {
                throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
            }
            Set<Provider.Service> services = provider.getServices();
            Iterator<Provider.Service> iterator = services.iterator();
            while (iterator.hasNext()) {
                Provider.Service service = iterator.next();
                if (ProviderServiceClassifier.selectedService(service)
                        == Classification.UNRECOGNIZED_AES_CIPHER) {
                    mismatch.reject();
                }
            }
        }
    }

    public interface Mismatch {
        void reject();
    }

    private static final class ProductionMismatch implements Mismatch {
        private ProductionMismatch() { }
        @Override public void reject() {
            ProviderMismatchRefusal.reject();
        }
    }
}

