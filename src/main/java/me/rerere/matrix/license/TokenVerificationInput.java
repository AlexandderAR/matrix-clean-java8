package me.rerere.matrix.license;

import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.crypto.EmbeddedAuthDecoder;

/** Input constants and forwarding behavior for selected token verification. */
public final class TokenVerificationInput {
    private static final byte[] FIRST_TOKEN_KEY = new byte[] {
            52, 97, 54, 99, 109, 109, 55, 83, 75, 82, 108, 51, 83, 106, 81, 118, 69, 54, 51, 48
    };
    private static final byte[] SECOND_TOKEN_KEY = new byte[] {
            121, 48, 119, 76, 116, 55, 48, 101, 50, 77, 88, 53, 72, 86, 50, 89, 90, 51, 83, 77
    };

    private TokenVerificationInput() { }

    public static String decodeSelected(PrefaceRuntimeContext selected) {
        if (selected == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        return decodeEncoded(selected.getUserKey());
    }

    public static String decodeEncoded(String encoded) {
        if (encoded == null) throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        byte[] decoded = EmbeddedAuthDecoder.decode(
                encoded, FIRST_TOKEN_KEY, SECOND_TOKEN_KEY, 5556565728L, 0);
        return me.rerere.matrix.thirdparty.kotlin.text.StringsKt.decodeToString(decoded);
    }

    public static void verifySelected(PrefaceRuntimeContext selected, String first, String second,
                                      String third, String fourth) {
        String decoded = decodeSelected(selected);
        LicenseTokenVerifier.verify(selected, decoded, first, second, third, fourth);
    }
}



