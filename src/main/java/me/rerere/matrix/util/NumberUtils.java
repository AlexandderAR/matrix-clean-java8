package me.rerere.matrix.util;

import java.util.OptionalInt;

/** Integer parsing and decimal rounding helpers. */
public class NumberUtils {
    public NumberUtils() {}

    public static OptionalInt parseInt(String text) {
        try { return OptionalInt.of(Integer.parseInt(text)); }
        catch (Exception ignored) { return OptionalInt.empty(); }
    }

    public static float round3(float value) {
        return Math.round(value * 1000.0f) / 1000.0f;
    }

    public static double roundPowerOfTen(double value, int decimalPlaces) {
        double scale = Math.pow(10.0, decimalPlaces);
        return Math.round(value * scale) / scale;
    }

    public static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    public static boolean isInteger(String text) {
        try { Integer.parseInt(text); return true; }
        catch (Exception ignored) { return false; }
    }

}


