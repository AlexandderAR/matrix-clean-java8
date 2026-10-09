package me.rerere.matrix.network;

/** Selects the configured site-local address and preserves its runtime failure translation. */
public final class SiteLocalAddressesSelected {
    private SiteLocalAddressesSelected() {
    }

    public static String run(SiteLocalAddressActions actions) throws Throwable {
        try {
            return actions.enumerate();
        } catch (Exception failure) {
            throw actions.replacement("Failed to get local IP address");
        }
    }

    public static String selected() throws Throwable {
        return run(new SiteLocalAddressProduction());
    }
}
