package me.rerere.matrix.util.text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import net.md_5.bungee.api.ChatColor;

/** Converts alternate color codes and the Matrix six-digit hex form. */
public final class ChatColorFormatter {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&(#[A-Fa-f0-9]{6})");

    private ChatColorFormatter() { }

    public static List<String> translateList(List<String> values, char alternateColorCode) {
        ArrayList<String> translatedValues = new ArrayList<String>(values.size());
        for (String value : values) {
            translatedValues.add(translateColorCode(value, alternateColorCode));
        }
        return translatedValues;
    }

    public static List<String> translateList(List<String> values, int colorFlag) {
        char alternateColorCode = (colorFlag & 1) != 0 ? '&' : (char) (colorFlag & 1);
        return translateList(values, alternateColorCode);
    }

    public static String formatString(String text, int colorFlag) {
        char alternateColorCode = (colorFlag & 1) != 0 ? '&' : (char) (colorFlag & 1);
        return translateColorCode(text, alternateColorCode);
    }

    public static String translateColorCode(String text, char alternateColorCode) {
        String formatted = formatHexColors(text);
        return org.bukkit.ChatColor.translateAlternateColorCodes(alternateColorCode, formatted);
    }

    private static String formatHexColors(String text) {
        String formattedText = text;
        Matcher matcher = HEX_COLOR_PATTERN.matcher(formattedText);
        while (matcher.find()) {
            String matchedColorCode = formattedText.substring(matcher.start(), matcher.end());
            Intrinsics.checkNotNullExpressionValue(matchedColorCode, "substring(...)");
            String hexDigits = matchedColorCode.substring(1);
            Intrinsics.checkNotNullExpressionValue(hexDigits, "substring(...)");
            String replacement = ChatColor.of(hexDigits).toString();
            formattedText = formattedText.replace(matchedColorCode, replacement);
            matcher = HEX_COLOR_PATTERN.matcher(formattedText);
        }
        return formattedText;
    }

    public static String translateWithDefaultColorCode(String text) {
        return translateColorCode(text, '&');
    }
}
