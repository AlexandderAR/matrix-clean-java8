package me.rerere.matrix.license;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Iterator;
import javax.net.ssl.HttpsURLConnection;
import javax.security.auth.x500.X500Principal;
import me.rerere.matrix.crypto.CertificatePinRefusal;
import me.rerere.matrix.crypto.Sha256Digest;
import me.rerere.matrix.api.MatrixApiResponseMessages;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.ArrayIteratorKt;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;

/** Certificate chain pinning checks performed after the HTTPS connection is opened. */
public final class CertificateChainVerifier {
    public interface HaltEffect {
        void halt(int status);
    }

    public static final class MatchedChain {
        public final Certificate[] certificates;
        public final boolean intermediatePin;
        public final boolean rootPin;

        private MatchedChain(Certificate[] certificates, boolean intermediatePin, boolean rootPin) {
            this.certificates = certificates;
            this.intermediatePin = intermediatePin;
            this.rootPin = rootPin;
        }
    }

    public static final class UnsupportedNativePath extends Error {
        public UnsupportedNativePath(String message) { super(message); }
    }

    private CertificateChainVerifier() {}

    private static void requireSelected(boolean pathIsSupported, String message) {
        if (!pathIsSupported) throw new UnsupportedNativePath(message);
    }

    private static void requireName(boolean nameMatches) {
        if (!nameMatches) throw new IllegalArgumentException("Failed requirement.");
    }

    private static String principalName(X500Principal principal) {
        if (principal == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        return principal.getName();
    }

    private static byte[] encodedForNativeSha(X509Certificate certificate) throws CertificateEncodingException {
        byte[] encoded = certificate.getEncoded();
        if (encoded == null) throw new NullPointerException("\"IASTORE npe\" on -1");
        return encoded;
    }

    /** Preserves the original terminal fallback when no injected halt effect is supplied. */
    private static void rejectArray(HaltEffect effect) {
        if (effect == null) {
            Runtime.getRuntime().halt(0);
        } else {
            effect.halt(0);
        }
    }

    public static void checkArrayPrefix(Certificate[] certificates, HaltEffect haltEffect) {
        if (certificates == null) throw new NullPointerException("\"ARRAYLENGTH npe\" on -1");
        for (Certificate certificate : certificates) {
            if (!(certificate instanceof X509Certificate)) {
                rejectArray(haltEffect);
                break;
            }
        }
        if (certificates.length != 3) rejectArray(haltEffect);
    }

    public static MatchedChain verifyAfterConnect(HttpsURLConnection connection) throws Exception {
        if (connection == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        Certificate[] chain = connection.getServerCertificates();
        checkArrayPrefix(chain, null);

        for (int index = 0; index < chain.length; index++) {
            X509Certificate certificate = (X509Certificate) chain[index];
            if (index > 0) {
                X509Certificate previous = (X509Certificate) chain[index - 1];
                requireName(Intrinsics.areEqual(
                    principalName(certificate.getSubjectX500Principal()),
                    principalName(previous.getIssuerX500Principal())));
            }
            if (index == 0) {
                requireName(Intrinsics.areEqual(principalName(certificate.getSubjectX500Principal()), "CN=localhost"));
                requireName(Intrinsics.areEqual(principalName(certificate.getIssuerX500Principal()), "CN=Offline,O=Offline Mode,C=XX"));
            } else if (index == 2) {
                requireName(StringsKt.contains(principalName(certificate.getIssuerX500Principal()), "GlobalSign Root CA", false));
            }
        }

        boolean intermediatePin = false;
        boolean rootPin = false;
        Iterator<?> chainIterator = ArrayIteratorKt.iterator(chain);
        while (chainIterator.hasNext()) {
            X509Certificate certificate = (X509Certificate) chainIterator.next();
            String subject = principalName(certificate.getSubjectX500Principal());
            String issuer = principalName(certificate.getIssuerX500Principal());
            String digest = MatrixApiResponseMessages.sha256Label(
                    Sha256Digest.digestHex(encodedForNativeSha(certificate)));
            if (StringsKt.contains(subject, "CN=Offline", false)
                    && StringsKt.contains(issuer, "Offline Mode", false)
                    && Intrinsics.areEqual(digest, "sha256:0000000000000000000000000000000000000000000000000000000000000000")) {
                intermediatePin = true;
            }
            if (StringsKt.contains(subject, "CN=Offline Root", false)
                    && StringsKt.contains(issuer, "Offline Root CA", false)
                    && Intrinsics.areEqual(digest, "sha256:0000000000000000000000000000000000000000000000000000000000000000")) {
                rootPin = true;
            }
        }
        if (!intermediatePin || !rootPin) CertificatePinRefusal.reject();
        return new MatchedChain(chain, intermediatePin, rootPin);
    }
}


