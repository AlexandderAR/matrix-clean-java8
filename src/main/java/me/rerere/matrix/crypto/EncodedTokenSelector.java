package me.rerere.matrix.crypto;

import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;

public final class EncodedTokenSelector {
    private static final byte[] KEY1 = new byte[] {
            52, 97, 54, 99, 109, 109, 55, 83, 75, 82,
            108, 51, 83, 106, 81, 118, 69, 54, 51, 48
    };
    private static final byte[] KEY2 = new byte[] {
            121, 48, 119, 76, 116, 55, 48, 101, 50, 77,
            88, 53, 72, 86, 50, 89, 90, 51, 83, 77
    };

    private EncodedTokenSelector() {
    }

    public static String fromInput(PrefaceRuntimeContext input) {
        if (input == null) {
            throw new UnsupportedNativePath("null ur before native argument getter");
        }
        return fromEncoded(input.getUserKey());
    }

    public static String fromEncoded(String encodedInput) {
        if (encodedInput == null) {
            throw new UnsupportedNativePath("null encoded input outside selected decoder contract");
        }
        byte[] decodedBytes = EmbeddedAuthDecoder.decode(encodedInput, KEY1, KEY2, 5556565728L, 0);
        return selectDecoded(StringsKt.decodeToString(decodedBytes));
    }

    public static String selectDecoded(String decoded) {
        if (decoded == null) {
            throw new UnsupportedNativePath("null decoded input outside selected route");
        }
        List<String> pieces = StringsKt.split(decoded, new String[] {"#"}, false, 0);
        List<String> nonemptyPieces = new ArrayList<>();
        for (String piece : pieces) {
            if (piece.length() > 0) {
                nonemptyPieces.add(piece);
            }
        }
        if (nonemptyPieces.size() <= 3) {
            return "";
        }
        return nonemptyPieces.get(3);
    }

    public static final class UnsupportedNativePath extends Error {
        public UnsupportedNativePath(String message) {
            super(message);
        }
    }
}
