package me.rerere.matrix.license;

public final class EncodedLicenseEndpoint {
    private EncodedLicenseEndpoint() {
    }

    public static byte[] ciphertext() {
        return new byte[] {
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
        };
    }

    public static byte[] salt() {
        return new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    }

    public static int key() {
        return 0;
    }
}
