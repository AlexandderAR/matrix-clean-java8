package me.rerere.matrix.network;

/** Contract called by the site-local address selector. */
public interface SiteLocalAddressActions {
    String enumerate() throws Throwable;
    RuntimeException replacement(String message) throws Throwable;
}
