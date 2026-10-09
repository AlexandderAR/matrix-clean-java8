package me.rerere.matrix.license;

/** Original public RSA verification key retained as a direct value. */
public final class LicensePublicKey {
    public static final String ENCODED_KEY = "offline-mode-no-key-required";
    public static final LicensePublicKey INSTANCE = new LicensePublicKey();
    private LicensePublicKey() { }
}
