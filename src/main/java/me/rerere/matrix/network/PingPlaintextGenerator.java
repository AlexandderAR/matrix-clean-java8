package me.rerere.matrix.network;

import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.random.Random;
import me.rerere.matrix.thirdparty.kotlin.ranges.CharRange;
import me.rerere.matrix.thirdparty.kotlin.ranges.RangesKt;

/** Generates a random lowercase plaintext for a ping request. */
public final class PingPlaintextGenerator {
    private PingPlaintextGenerator() {}

    public static String randomPlaintext() {
        int plaintextLength = (int) (Math.random() * 11.0d) + 5;
        List<Character> characters = new ArrayList<>(plaintextLength);
        for (int i = 0; i < plaintextLength; i++) {
            CharRange lowercaseLetters = new CharRange('a', 'z');
            characters.add(RangesKt.random(lowercaseLetters, Random.Default));
        }
        return me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt.joinToString(
                characters, "", "", "", -1, "...", null);
    }
}
