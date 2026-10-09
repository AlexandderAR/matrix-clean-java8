package me.rerere.matrix.i18n;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import me.rerere.matrix.config.NamedListSource;
import me.rerere.matrix.i18n.LanguageEntry;
import me.rerere.matrix.i18n.TranslatedEntry;
import me.rerere.matrix.util.text.PlaceholderTemplateFactory;
import me.rerere.matrix.thirdparty.kotlin.Lazy;
import me.rerere.matrix.thirdparty.kotlin.LazyKt;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;

/** Static language entries used by command and interface text. */
public final class LanguageCatalog {
    private static final TranslatedEntry reloadSuccess;
    public static final LanguageCatalog INSTANCE;
    private static final TranslatedEntry reloadDescription;
    private static final NamedListSource tpsLore;
    private static final Lazy<List<LanguageEntry>> languageEntries;
    private static final NamedListSource reloadLore;

    private LanguageCatalog() { }

    public final List<LanguageEntry> getLanguageEntries() {
        return languageEntries.getValue();
    }

    public final TranslatedEntry getReloadDescription() { return reloadDescription; }
    public final NamedListSource getReloadLore() { return reloadLore; }
    public final NamedListSource getTpsLore() { return tpsLore; }
    public final TranslatedEntry getReloadSuccess() { return reloadSuccess; }

    /** Reflectively gathers all static fields derived from the language-entry base type. */
    private static List<LanguageEntry> collectLanguageEntries() {
        Field[] declaredFields = LanguageCatalog.class.getDeclaredFields();
        List<Field> matchingFields = new ArrayList<>();
        for (Field field : declaredFields) {
            if (LanguageEntry.class.isAssignableFrom(field.getType())) {
                matchingFields.add(field);
            }
        }

        List<LanguageEntry> entries = new ArrayList<>(matchingFields.size());
        for (Field field : matchingFields) {
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(null);
            } catch (IllegalAccessException failure) {
                return (List<LanguageEntry>) LanguageCatalog.<RuntimeException>throwUnchecked(failure);
            }
            Intrinsics.checkNotNull(value,
                    "null cannot be cast to non-null type me.rerere.matrix.data.locale.LangEntry");
            entries.add((LanguageEntry) value);
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> Object throwUnchecked(Throwable failure) throws E {
        throw (E) failure;
    }

    static {
        INSTANCE = new LanguageCatalog();
        languageEntries = LazyKt.lazy(() -> collectLanguageEntries());
        reloadDescription = PlaceholderTemplateFactory.createFormatter(
                "command.reload.desc", "&aReload the plugin");
        reloadSuccess = PlaceholderTemplateFactory.createFormatter(
                "command.reload.success", "Reloaded the plugin");
        reloadLore = PlaceholderTemplateFactory.createTemplate(
                "gui.reload.lore", CollectionsKt.listOf("Reload the plugin"));
        tpsLore = PlaceholderTemplateFactory.createTemplate(
                "gui.tps.lore", CollectionsKt.listOf("Show the server TPS"));
    }
}
