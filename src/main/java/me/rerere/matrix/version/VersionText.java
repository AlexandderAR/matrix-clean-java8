package me.rerere.matrix.version;

import java.util.List;
import java.util.Objects;
import me.rerere.matrix.thirdparty.kotlin.text.Regex;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt__StringsKt;

/** A version value that exposes its normalized text and numeric components. */
public final class VersionText {
    private final String value;
    private VersionText(String value) {
        this.value = value;
    }

    public static VersionText fromVersionText(String value) {
        return new VersionText(value);
    }

    public String versionText() {
        return value;
    }

    public static int majorVersion(String versionText) {
        return parseComponent(versionText, 0);
    }

    public static int minorVersion(String versionText) {
        return parseComponent(versionText, 1);
    }

    public static int patchVersion(String versionText) {
        return parseComponent(versionText, 2);
    }

    public static String firstVersionText(String value) {
        return value;
    }

    public static int compareVersionTexts(String left, String right) {
        int comparison = Integer.compare(majorVersion(left), majorVersion(right));
        if (comparison != 0) {
            return comparison;
        }
        comparison = Integer.compare(minorVersion(left), minorVersion(right));
        if (comparison != 0) {
            return comparison;
        }
        return Integer.compare(patchVersion(left), patchVersion(right));
    }

    public static String identityVersionText(String value) {
        return value;
    }

    public static boolean sameVersionText(String left, String right) {
        return Objects.equals(left, right);
    }

    public static String formatVersionText(String versionText) {
        String normalized = normalizeVersionText(versionText);
        return formatComponents(normalized, majorVersion(versionText),
                minorVersion(versionText), patchVersion(versionText));
    }

    private static String formatComponents(String normalized, int major, int minor, int patch) {
        return new StringBuilder("{safe=").append(normalized)
                .append(", major=").append(major)
                .append(", minor=").append(minor)
                .append(", patch=").append(patch)
                .append("}").toString();
    }

    private static String normalizeVersionText(String versionText) {
        return new Regex("[^0-9._]").replace(versionText, "")
                .replace('_', '.');
    }

    private static int parseComponent(String versionText, int componentIndex) {
        try {
            String normalized = normalizeVersionText(versionText);
            List<String> components = StringsKt__StringsKt.split(
                    (CharSequence) normalized, new String[] { "." }, false, 0);
            return Integer.parseInt(components.get(componentIndex));
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static int versionTextHash(String versionText) {
        return versionText.hashCode();
    }

    private static boolean versionValueEquals(String versionText, Object other) {
        // xb.5() is false in the supplied closed-own-callgraph evidence.
        return other instanceof VersionText
                && Objects.equals(versionText, ((VersionText) other).value);
    }

    @Override
    public boolean equals(Object other) {
        return versionValueEquals(value, other);
    }

    @Override
    public int hashCode() {
        return versionTextHash(value);
    }

    @Override
    public String toString() {
        return formatVersionText(value);
    }
}
