package me.rerere.matrix.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import me.rerere.matrix.util.UrlIntTransform;

/** Decodes byte arrays with the MatrixKDFv1 keystream and per-byte feedback. */
public final class MatrixByteStreamDecoder {
    private static final byte[] LABEL;

    private MatrixByteStreamDecoder() {
    }

    private static byte[] seed(byte[] salt, int roundCount) {
        ByteBuffer seed = ByteBuffer.allocate(salt.length + LABEL.length + 6);
        seed.put(salt);
        seed.put(LABEL);
        seed.put((byte) roundCount);
        seed.put((byte) 74);
        seed.putInt(1);
        return seed.array();
    }

    private static byte[] digest(byte[] salt, int roundCount) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(seed(salt, roundCount));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private static int[] state(byte[] salt, int roundCount, byte[] digest) {
        int[] stateWords = new int[16];
        stateWords[0] = -559038242;
        stateWords[1] = -1095783199;
        stateWords[2] = -889275714;
        stateWords[3] = -1378760723;
        for (int i = 0; i < 8; i++) {
            int offset = i * 4;
            stateWords[i + 4] = ((digest[offset] & 0xff) << 24)
                    | ((digest[offset + 1] & 0xff) << 16)
                    | ((digest[offset + 2] & 0xff) << 8)
                    | (digest[offset + 3] & 0xff);
        }
        stateWords[12] = roundCount;
        stateWords[13] = ((salt[0] & 0xff) << 24)
                | ((salt[1] & 0xff) << 16)
                | ((salt[2] & 0xff) << 8)
                | (salt[3] & 0xff);
        stateWords[14] = 485163226;
        stateWords[15] = 0;
        return stateWords;
    }

    private static long bigEndianLong(byte[] bytes, int offset) {
        long result = 0L;
        for (int i = 0; i < 8; i++) {
            result = (result << 8) | (bytes[offset + i] & 0xffL);
        }
        return result;
    }

    public static byte[] decodeBytes(byte[] input, byte[] salt, int roundCount) {
        if (input == null || salt == null) {
            throw new NullPointerException();
        }
        if (salt.length < 16) {
            throw new IllegalArgumentException("salt must contain 16 bytes");
        }

        byte[] digest = digest(salt, roundCount);
        int[] initialState = state(salt, roundCount, digest);
        byte[] output = new byte[input.length];
        int previousCipherByte = salt[0] & 0xff;
        long firstGeneratorWord = bigEndianLong(digest, 0);
        long secondGeneratorWord = bigEndianLong(digest, 8);
        int[] transformedBlock = null;

        for (int i = 0; i < output.length; i++) {
            if ((i & 63) == 0) {
                int[] blockState = initialState.clone();
                blockState[15] += i >>> 6;
                transformedBlock = UrlIntTransform.transform(blockState, blockState.clone());
            }

            int stateWord = transformedBlock[(i & 63) >>> 2];
            int streamByte = (stateWord >>> (24 - ((i & 3) << 3))) & 0xff;
            int encryptedByte = input[i] & 0xff;
            int feedbackByte = ((previousCipherByte << 3) | (previousCipherByte >>> 5)) & 0xff;
            int feedbackMixedByte = encryptedByte ^ feedbackByte;
            previousCipherByte = encryptedByte;
            int mixedByte = feedbackMixedByte ^ streamByte;

            long previousFirstWord = firstGeneratorWord;
            long previousSecondWord = secondGeneratorWord;
            long nextGeneratorWord = previousFirstWord ^ (previousFirstWord << 23);
            nextGeneratorWord ^= nextGeneratorWord >>> 17;
            nextGeneratorWord ^= previousSecondWord;
            nextGeneratorWord ^= previousSecondWord >>> 26;
            firstGeneratorWord = previousSecondWord;
            secondGeneratorWord = nextGeneratorWord;
            int rotation = (int) (nextGeneratorWord + previousSecondWord) & 7;
            int rotatedByte = ((mixedByte >>> rotation) | (mixedByte << (8 - rotation))) & 0xff;
            output[i] = (byte) (((((rotatedByte + 89) & 0xff) * 223) - roundCount) & 0xff);
        }
        return output;
    }

    public static String decodeString(byte[] input, byte[] salt, int roundCount) {
        return new String(decodeBytes(input, salt, roundCount), StandardCharsets.UTF_8);
    }

    static {
        LABEL = "MatrixKDFv1".getBytes(StandardCharsets.UTF_8);
    }
}
