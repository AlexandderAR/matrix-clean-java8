package me.rerere.matrix.util.text;

import me.rerere.matrix.thirdparty.kotlin.Pair;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt__StringsJVMKt;
import me.rerere.matrix.i18n.TranslatedEntry;
import me.rerere.matrix.license.LicenseResponseException;
import me.rerere.matrix.util.IntegerArrayCache;
import me.rerere.matrix.util.text.ChatColorFormatter;

/** Mutable message template that supports keyed placeholder replacement. */
public final class PlaceholderTemplate extends IntegerArrayCache {
    private String template;

    public PlaceholderTemplate(TranslatedEntry translatedEntry) {
        super((DefaultConstructorMarker) null);
        this.template = translatedEntry.resolveTranslation();
    }

    public String getTemplate() {
        return this.template;
    }

    public PlaceholderTemplate formatTemplate() {
        this.template = ChatColorFormatter.formatString(this.template, 1);
        return this;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public PlaceholderTemplate replacePairs(byte replacementMode, Pair<?, ?>... replacements) {
        for (Pair<?, ?> replacementPair : replacements) {
            try {
                if (replacementMode > 0) {
                    return this;
                }
                String placeholder = this.placeholderSyntax(replacementPair.getFirst());
                String replacementText = (String) replacementPair.getSecond();
                this.template = StringsKt__StringsJVMKt.replace(
                        this.template, placeholder, replacementText, false);
            } catch (LicenseResponseException failure) {
                throw failure;
            }
        }
        return this;
    }

    private String placeholderSyntax(Object placeholderName) {
        return "{" + placeholderName + "}";
    }

    public String currentTemplate() {
        return this.template;
    }
}
