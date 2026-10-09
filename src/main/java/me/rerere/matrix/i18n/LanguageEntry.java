package me.rerere.matrix.i18n;

import me.rerere.matrix.util.IntegerArrayCache;
import me.rerere.matrix.config.NamedListSource;
import me.rerere.matrix.util.text.PlaceholderTemplate;
import me.rerere.matrix.commands.condition.FixedListValue;

/** Language key value with its tagged-entry factory conversion. */
public abstract class LanguageEntry {
    private final String key;

    protected LanguageEntry(String key) {
        this.key = key;
    }

    public String key() { return key; }

    public String toString() { return "LangEntry(key='" + key + "')"; }

    public IntegerArrayCache toTypedEntry() {
        if (this instanceof TranslatedEntry) {
            return new PlaceholderTemplate((TranslatedEntry) this);
        }
        if (this instanceof NamedListSource) {
            return new FixedListValue((NamedListSource) this);
        }
        throw new me.rerere.matrix.thirdparty.kotlin.NoWhenBranchMatchedException();
    }
}
