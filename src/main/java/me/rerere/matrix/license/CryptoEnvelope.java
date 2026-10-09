package me.rerere.matrix.license;

import java.security.GeneralSecurityException;
import me.rerere.matrix.crypto.AesStringCipher;
import me.rerere.matrix.crypto.RsaCrypto;
import me.rerere.matrix.util.TransformedTextPairFactory;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Holds the RSA-wrapped AES key and the AES-encrypted license payload. */
public final class CryptoEnvelope {
    private final String key;
    private final String data;
    public static final TransformedTextPairFactory COMPANION = new TransformedTextPairFactory(null);

    public CryptoEnvelope(String key, String data) {
        this.key = key;
        this.data = data;
    }
    public String getKey() { return key; }
    public String getData() { return data; }
    public String componentKey() { return key; }
    public String componentData() { return data; }
    public CryptoEnvelope copy(String key, String data) { return new CryptoEnvelope(key, data); }

    public static CryptoEnvelope copyWithDefaults(CryptoEnvelope source, String key, String data,
                                                    int mask) {
        if ((mask & 1) != 0) key = source.key;
        if ((mask & 2) != 0) data = source.data;
        return source.copy(key, data);
    }

    public String decrypt(String encodedPrivateKey) {
        try {
            String decryptedKey = RsaCrypto.INSTANCE.decrypt(key, encodedPrivateKey);
            return AesStringCipher.INSTANCE.decrypt(data, decryptedKey);
        } catch (GeneralSecurityException failure) {
            return CryptoEnvelope.<String, RuntimeException>throwUnchecked(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T throwUnchecked(Throwable failure) throws E {
        throw (E) failure;
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) return true;
        if (!(candidate instanceof CryptoEnvelope)) return false;
        CryptoEnvelope other = (CryptoEnvelope) candidate;
        return Intrinsics.areEqual(key, other.key) && Intrinsics.areEqual(data, other.data);
    }
    @Override
    public int hashCode() {
        return key.hashCode() * 31 + data.hashCode();
    }

    @Override
    public String toString() {
        return "Envelop(key=" + key + ", data=" + data + ")";
    }
}
