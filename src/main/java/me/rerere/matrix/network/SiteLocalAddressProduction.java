package me.rerere.matrix.network;

import java.net.SocketException;
import me.rerere.matrix.license.LicenseResponseException;

/** Production adapter for site-local address selection. */
final class SiteLocalAddressProduction implements SiteLocalAddressActions {
    SiteLocalAddressProduction() { }

    @Override
    public String enumerate() {
        try {
            return NetworkInput.selectedSiteLocalAddresses();
        } catch (SocketException failure) {
            return SiteLocalAddressProduction.<RuntimeException, String>rethrow(failure);
        }
    }

    @Override
    public RuntimeException replacement(String message) {
        return new LicenseResponseException(message);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable, R> R rethrow(Throwable failure) throws T {
        throw (T) failure;
    }
}
