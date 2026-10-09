package me.rerere.matrix.network;

import me.rerere.matrix.crypto.PingKeyMaterial;
import me.rerere.matrix.license.EncodedLicenseEndpoint;
import me.rerere.matrix.crypto.MatrixByteStreamDecoder;

/** Exposes selected literal inputs after the byte-array decoder. */
public final class NetworkSecurityInputs {
    private NetworkSecurityInputs() {
    }

    public static String earlyPingPublicKeyBase64() {
        return MatrixByteStreamDecoder.decodeString(
                PingKeyMaterial.cipherBytes(),
                PingKeyMaterial.saltBytes(),
                PingKeyMaterial.keyId());
    }

    public static String laterRequestUrl() {
        return MatrixByteStreamDecoder.decodeString(
                EncodedLicenseEndpoint.ciphertext(),
                EncodedLicenseEndpoint.salt(),
                EncodedLicenseEndpoint.key());
    }
}
