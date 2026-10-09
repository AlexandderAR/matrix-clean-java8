package me.rerere.matrix.config;

import java.util.List;
import me.rerere.matrix.i18n.LanguageEntry;
import me.rerere.matrix.i18n.LanguageResourceStore;

/** A named list source delegated to the shared registry. */
public final class NamedListSource extends LanguageEntry {
    private final List<?> values;

    public NamedListSource(String name, List<?> values) {
        super(name);
        this.values = values;
    }

    public List<?> values() {
        return values;
    }

    public List<?> lookup() {
        return LanguageResourceStore.INSTANCE.getList(this);
    }
}
