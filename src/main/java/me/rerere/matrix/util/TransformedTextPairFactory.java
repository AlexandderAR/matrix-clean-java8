package me.rerere.matrix.util;

import java.security.GeneralSecurityException;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.crypto.RsaCrypto;
import me.rerere.matrix.license.CryptoEnvelope;
import me.rerere.matrix.crypto.AesStringCipher;

/** Applies the text transforms and returns their two-string result. */
public final class TransformedTextPairFactory {
    private TransformedTextPairFactory() {
    }

    public TransformedTextPairFactory(DefaultConstructorMarker ignoredMarker) {
        this();
    }

    public final CryptoEnvelope transform(String primary, String secondary)
            throws GeneralSecurityException {
        String decodedPrimary = AesStringCipher.INSTANCE
                .createBase64Key();
        String rsaCiphertext = RsaCrypto.INSTANCE.encrypt(decodedPrimary, secondary);
        String aesCiphertext = AesStringCipher.INSTANCE.encrypt(primary, decodedPrimary);
        return new CryptoEnvelope(rsaCiphertext, aesCiphertext);
    }

}
