package me.rerere.matrix.util;

/** Applies a 20-round transformation to a 16-word state. */
public final class UrlIntTransform {
    public UrlIntTransform() {
    }

    private static void quarterRound(int[] state, int a, int b, int c, int d) {
        state[a] += state[b];
        state[d] = Integer.rotateLeft(state[d] ^ state[a], 16);
        state[c] += state[d];
        state[b] = Integer.rotateLeft(state[b] ^ state[c], 12);
        state[a] += state[b];
        state[d] = Integer.rotateLeft(state[d] ^ state[a], 8);
        state[c] += state[d];
        state[b] = Integer.rotateLeft(state[b] ^ state[c], 7);
    }

    private static void postmix(int[] state) {
        for (int index = 0; index < 16; index++) {
            state[index] ^= Integer.rotateRight(state[(index + 1) & 15], index % 13 + 1);
        }
    }

    public static int[] transform(int[] initialState, int[] inputWords) {
        int[] state = initialState.clone();
        for (int round = 0; round < 20; round += 2) {
            quarterRound(state, 0, 4, 8, 12);
            quarterRound(state, 1, 5, 9, 13);
            quarterRound(state, 2, 6, 10, 14);
            quarterRound(state, 3, 7, 11, 15);
            quarterRound(state, 0, 5, 10, 15);
            quarterRound(state, 1, 6, 11, 12);
            quarterRound(state, 2, 7, 8, 13);
            quarterRound(state, 3, 4, 9, 14);
            postmix(state);
        }
        for (int index = 0; index < 16; index++) {
            state[index] += inputWords[index];
        }
        return state;
    }

    private static int[] parseHexWords(String encodedWords) {
        int[] words = new int[16];
        for (int index = 0; index < 16; index++) {
            int start = index * 8;
            words[index] = (int) Long.parseLong(encodedWords.substring(start, start + 8), 16);
        }
        return words;
    }

    public static void main(String[] arguments) {
        int[] transformed = transform(parseHexWords(arguments[0]), parseHexWords(arguments[1]));
        StringBuilder output = new StringBuilder();
        for (int word : transformed) {
            output.append(String.format("%08x", word));
        }
        System.out.println(output);
    }
}

