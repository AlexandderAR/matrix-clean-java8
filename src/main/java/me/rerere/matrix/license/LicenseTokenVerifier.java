package me.rerere.matrix.license;

import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.config.PrefaceRuntimeContext;
import me.rerere.matrix.crypto.EncodedIndexMaterial;
import me.rerere.matrix.security.PostTokenMismatchHandler;
import me.rerere.matrix.util.BuildMetadata;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;

public final class LicenseTokenVerifier {
    private LicenseTokenVerifier() {
    }

    public interface ExpectedValues {
        void touchContext();
        String first();
        String second();
        String third();
        String fourth();
        String fifth();
    }

    public static final class UnresolvedTokenPath extends Error {
        public UnresolvedTokenPath(String message) {
            super(message);
        }
    }

    static final class PeerValues implements ExpectedValues {
        final LicenseMetadataProvider peer;

        PeerValues(LicenseMetadataProvider peer) {
            this.peer = peer;
        }

        public void touchContext() {
        }

        public String first() {
            return peer.getDisplayLabel();
        }

        public String second() {
            return peer.getBuildNumber();
        }

        public String third() {
            return peer.getRevisionMarker();
        }

        public String fourth() {
            return peer.getBuildVersion();
        }

        public String fifth() {
            return peer.getEmbeddedToken();
        }
    }

    public static String fifthNonemptyPart(String encoded) {
        if (encoded == null) {
            throw new UnresolvedTokenPath("decoded second ur.w value null path");
        }
        List<String> tokenParts = StringsKt.split(encoded, new String[] { "#" }, false, 0);
        ArrayList<String> nonemptyParts = new ArrayList<>();
        for (String tokenPart : tokenParts) {
            if (tokenPart.length() > 0) {
                nonemptyParts.add(tokenPart);
            }
        }
        return nonemptyParts.size() > 4 ? nonemptyParts.get(4) : "";
    }

    public static void verify(String encoded, String first, String second, String fourth, String fifth,
                              ExpectedValues expected) {
        String third = fifthNonemptyPart(encoded);
        if (expected == null) {
            throw new UnresolvedTokenPath("missing selected as/fg actions");
        }
        expected.touchContext();
        compare(first, second, third, fourth, fifth, expected);
    }

    public static void verify(PrefaceRuntimeContext receiver, String encoded,
                              String first, String second, String fourth, String fifth) {
        String third = fifthNonemptyPart(encoded);
        EncodedIndexMaterial indexMaterial = EncodedIndexMaterial.INDEX;
        if (indexMaterial == null) {
            throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        }
        indexMaterial.encodedValue();
        LicenseMetadataProvider peer = BuildMetadata.LICENSE_METADATA;
        if (peer == null) {
            throw new NullPointerException("\"INVOKEVIRTUAL Object npe\" on -1");
        }
        compareProduction(receiver, first, second, third, fourth, fifth, new PeerValues(peer));
    }

    private static void compareProduction(PrefaceRuntimeContext receiver, String first,
                                           String second, String third, String fourth, String fifth,
                                           ExpectedValues expected) {
        if (!Intrinsics.areEqual(first, expected.first())) {
            PostTokenMismatchHandler.runSelected(receiver);
            return;
        }
        if (!Intrinsics.areEqual(second, expected.second())) {
            PostTokenMismatchHandler.runSelected(receiver);
            return;
        }
        if (!Intrinsics.areEqual(third, expected.third())) {
            PostTokenMismatchHandler.runSelected(receiver);
            return;
        }
        if (!Intrinsics.areEqual(fourth, expected.fourth())) {
            PostTokenMismatchHandler.runSelected(receiver);
            return;
        }
        if (!Intrinsics.areEqual(fifth, expected.fifth())) {
            PostTokenMismatchHandler.runSelected(receiver);
            return;
        }
    }

    private static void compare(String first, String second, String third, String fourth, String fifth,
                                ExpectedValues expected) {
        if (!Intrinsics.areEqual(first, expected.first())) {
            throw new UnresolvedTokenPath("first mismatch at 0xd6170");
        }
        if (!Intrinsics.areEqual(second, expected.second())) {
            throw new UnresolvedTokenPath("second mismatch at 0xd6170");
        }
        if (!Intrinsics.areEqual(third, expected.third())) {
            throw new UnresolvedTokenPath("third mismatch at 0xd6170");
        }
        if (!Intrinsics.areEqual(fourth, expected.fourth())) {
            throw new UnresolvedTokenPath("fourth mismatch at 0xd6170");
        }
        if (!Intrinsics.areEqual(fifth, expected.fifth())) {
            throw new UnresolvedTokenPath("fifth mismatch at 0xd6170");
        }
    }
}
