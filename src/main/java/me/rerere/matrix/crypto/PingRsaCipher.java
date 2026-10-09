package me.rerere.matrix.crypto;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

/** Encrypts UTF-8 text with an RSA public key encoded as Base64 X.509 data. */
public final class PingRsaCipher {
    private PingRsaCipher() { }

    public static String encrypt(String plaintext, String encodedX509PublicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        byte[] encodedKey = Base64.getDecoder().decode(encodedX509PublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] cleartext = plaintext.getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = cipher.doFinal(cleartext);
        return Base64.getEncoder().encodeToString(ciphertext);
    }
}
