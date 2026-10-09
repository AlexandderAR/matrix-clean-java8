package me.rerere.matrix.crypto;

import java.security.Provider;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;

/** Recovered directly from canonical ASM for SwProviderServicePredicateV1. */
public final class ProviderServiceClassifier {
    private ProviderServiceClassifier() { }

    public static Classification selectedService(Provider.Service service) {
        if (!Intrinsics.areEqual(service.getAlgorithm(), "AES")) return Classification.OTHER_SERVICE;
        if (!Intrinsics.areEqual(service.getType(), "Cipher")) return Classification.OTHER_SERVICE;
        String className = service.getClassName();
        if (StringsKt.startsWith(className, "com.sun.crypto.provider.AESCipher", false))
            return Classification.RECOGNIZED_AES_CIPHER;
        if (StringsKt.startsWith(className, "org.bouncycastle.jcajce.provider.symmetric.AES", false))
            return Classification.RECOGNIZED_AES_CIPHER;
        return Classification.UNRECOGNIZED_AES_CIPHER;
    }

    public enum Classification {
        OTHER_SERVICE,
        RECOGNIZED_AES_CIPHER,
        UNRECOGNIZED_AES_CIPHER
    }
}
