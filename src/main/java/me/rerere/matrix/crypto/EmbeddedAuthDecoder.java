package me.rerere.matrix.crypto;

import java.util.Arrays;

/** Decodes the bundled authentication payload. */
public final class EmbeddedAuthDecoder {
    public EmbeddedAuthDecoder() {
    }

    /**
     * Decodes hex input using an xorshift64* byte stream, an RC4-style pass,
     * then XORs the result with the first key. The integer parameter rotates
     * each first-pass byte right by the requested number of bits.
     */
    public static byte[] decode(String hex, byte[] firstKey, byte[] permutationKey,
                                long state, int rotateRight) {
        if (hex == null || firstKey == null || permutationKey == null) {
            throw new NullPointerException();
        }
        if (firstKey.length == 0 || permutationKey.length == 0) {
            throw new IllegalArgumentException("empty key outside selected path");
        }

        byte[] encryptedBytes = new byte[(hex.length() + 1) / 2];
        for (int offset = 0; offset < encryptedBytes.length; offset++) {
            String octet = hex.substring(offset * 2, Math.min(offset * 2 + 2, hex.length()));
            encryptedBytes[offset] = (byte) Integer.parseInt(octet, 16);
        }

        byte[] streamDecoded = new byte[encryptedBytes.length];
        long randomState = state;
        long streamWord = 0L;
        int remainingStreamBytes = 0;
        for (int offset = 0; offset < encryptedBytes.length; offset++) {
            if (remainingStreamBytes == 0) {
                randomState ^= randomState >>> 12;
                randomState ^= randomState << 25;
                randomState ^= randomState >>> 27;
                streamWord = randomState * 2685821657736338717L;
                remainingStreamBytes = 8;
            }
            streamDecoded[offset] = (byte) (encryptedBytes[offset] ^ (byte) streamWord);
            streamWord >>>= 8;
            remainingStreamBytes--;
        }

        byte[] permutedInput = Arrays.copyOf(streamDecoded, streamDecoded.length);
        if (rotateRight != 0) {
            for (int i = 0; i < permutedInput.length; i++) {
                int value = streamDecoded[i] & 0xff;
                permutedInput[i] = (byte) ((value >>> rotateRight)
                        | (value << (8 - rotateRight)));
            }
        }

        byte[] permutation = new byte[256];
        for (int index = 0; index < permutation.length; index++) permutation[index] = (byte) index;
        int swapIndex = 0;
        for (int index = 0; index < 256; index++) {
            swapIndex = (swapIndex + (permutation[index] & 0xff)
                    + (permutationKey[index % permutationKey.length] & 0xff)) & 0xff;
            byte previous = permutation[index];
            permutation[index] = permutation[swapIndex];
            permutation[swapIndex] = previous;
        }

        byte[] output = new byte[permutedInput.length];
        int firstIndex = 0;
        int secondIndex = 0;
        for (int offset = 0; offset < permutedInput.length; offset++) {
            firstIndex = (firstIndex + 1) & 0xff;
            secondIndex = (secondIndex + (permutation[firstIndex] & 0xff)) & 0xff;
            byte previous = permutation[firstIndex];
            permutation[firstIndex] = permutation[secondIndex];
            permutation[secondIndex] = previous;
            int keyIndex = ((permutation[firstIndex] & 0xff)
                    + (permutation[secondIndex] & 0xff)) & 0xff;
            output[offset] = (byte) (permutedInput[offset] ^ permutation[keyIndex]);
        }
        for (int offset = 0; offset < output.length; offset++) {
            output[offset] ^= firstKey[offset % firstKey.length];
        }
        return output;
    }
}
