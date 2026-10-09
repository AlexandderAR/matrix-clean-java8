package me.rerere.matrix.crypto;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.text.Charsets;

public final class RsaCrypto {
    public static final RsaCrypto INSTANCE = new RsaCrypto();

    private RsaCrypto() {
    }

    public final String decrypt(String ciphertext, String encodedPrivateKey)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        byte[] encodedKey = Base64.getDecoder().decode(encodedPrivateKey);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedKey);
        KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = rsaKeyFactory.generatePrivate(privateKeySpec);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)), Charsets.UTF_8);
    }

    public final String encrypt(String plaintext, String encodedPublicKey)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        byte[] encodedKey = Base64.getDecoder().decode(encodedPublicKey);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedKey);
        KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = rsaKeyFactory.generatePublic(publicKeySpec);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] plaintextBytes = plaintext.getBytes(Charsets.UTF_8);
        Intrinsics.checkNotNullExpressionValue(plaintextBytes, "getBytes(...)");
        return encoder.encodeToString(cipher.doFinal(plaintextBytes));
    }

    public final Pair<String, String> generateEncodedKeyPair()
            throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return new Pair<>(
                Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()),
                Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
    }
}
