package me.rerere.matrix.i18n;

/** A translated string attached to a language key. */
public final class TranslatedEntry extends LanguageEntry {
    private final String translatedValue;
    private static int compatibilityState;

    static {
        if (legacyCompatibilityStatus() != 0) setCompatibilityState(75);
    }

    public static void setCompatibilityState(int value) { compatibilityState = value; }
    public static int compatibilityState() { return compatibilityState; }
    public static int legacyCompatibilityStatus() { return compatibilityState() == 0 ? 15 : 0; }

    public TranslatedEntry(String key, String translatedValue) {
        super(key);
        this.translatedValue = translatedValue;
    }

    public final String translatedValue() {
        return translatedValue;
    }

    public final String resolveTranslation() {
        return LanguageResourceStore.INSTANCE.getMessage(this);
    }
}
