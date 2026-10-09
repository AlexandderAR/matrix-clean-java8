package me.rerere.matrix.license;

/** Preserves the original response exception's message-only stderr output. */
public final class LicenseResponseException extends IllegalStateException {
    public LicenseResponseException(String message) {
        super(message);
    }

    @Override
    public void printStackTrace() {
        System.err.println(getMessage());
    }
}
