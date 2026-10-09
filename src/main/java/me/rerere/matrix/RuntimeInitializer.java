package me.rerere.matrix;

import me.rerere.matrix.i18n.LanguageCatalog;

/** Invokes the no-argument startup operation on the configured singleton. */
public class RuntimeInitializer {
    public RuntimeInitializer() {
        super();
    }

    public static void main(String[] arguments) {
        LanguageCatalog.INSTANCE.getReloadDescription();
    }
}
