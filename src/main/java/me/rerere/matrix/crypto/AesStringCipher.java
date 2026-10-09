package me.rerere.matrix.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/** Encrypts and decrypts UTF-8 strings with AES and Base64-encoded keys. */
public final class AesStringCipher {
    public static final AesStringCipher INSTANCE;
    private static final SecureRandom RANDOM;

    static {
        INSTANCE = new AesStringCipher();
        RANDOM = new SecureRandom();
    }

    private AesStringCipher() {
        super();
    }

    public final String encrypt(String plaintext, String base64Key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plaintextBytes));
        } catch (GeneralSecurityException failure) {
            return AesStringCipher.<String, RuntimeException>throwUnchecked(failure);
        }
    }

    public final String createBase64Key() {
        byte[] keyBytes = new byte[16];
        for (int index = 0; index < keyBytes.length; index++) {
            keyBytes[index] = (byte) RANDOM.nextInt(256);
        }
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    public final String decrypt(String ciphertext, String base64Key) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec key = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] plaintextBytes = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plaintextBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException failure) {
            return AesStringCipher.<String, RuntimeException>throwUnchecked(failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T throwUnchecked(Throwable failure) throws E {
        throw (E) failure;
    }

}
