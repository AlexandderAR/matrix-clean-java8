package me.rerere.matrix.license.identity;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import me.rerere.matrix.api.MatrixApiResponseMessages;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.util.ByteHexFormatter;

/** Builds and renders the MD5 candidate derived from the four canonical input parts. */
public final class HardwareIdentityRegion {
    private HardwareIdentityRegion() { }

    public static String fromParts(String first, int region, String third, String fourth)
            throws NoSuchAlgorithmException {
        String digestInput = MatrixApiResponseMessages.concatenate(
                first, region, third, fourth);
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] inputBytes = digestInput.getBytes(
                me.rerere.matrix.thirdparty.kotlin.text.Charsets.UTF_8);
        byte[] digestBytes = digest.digest(inputBytes);
        return me.rerere.matrix.thirdparty.kotlin.collections.ArraysKt.joinToString(
                digestBytes, "", "", "", -1, "...",
                (Function1<? super Byte, ? extends CharSequence>) ByteHexFormatter.INSTANCE);
    }
}
