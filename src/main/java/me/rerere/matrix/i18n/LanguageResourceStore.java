package me.rerere.matrix.i18n;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.file.YamlFile;
import me.rerere.matrix.thirdparty.kotlin.NoWhenBranchMatchedException;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.util.text.ChatColorFormatter;
import me.rerere.matrix.config.NamedListSource;
import me.rerere.matrix.i18n.LanguageCatalog;

/** Reads language strings and list values from the active YAML language file. */
public final class LanguageResourceStore {
    public static final LanguageResourceStore INSTANCE;
    private static final Map<String, String> stringCache;
    private static final Map<String, List<String>> listCache;
    private static YamlFile languageFile;
    private static LocaleMessages languageManager;

    static {
        INSTANCE = new LanguageResourceStore();
        stringCache = new LinkedHashMap<>();
        listCache = new LinkedHashMap<>();
    }

    private LanguageResourceStore() { }

    public synchronized void reload(LocaleMessages manager) {
        languageManager = manager;
        languageFile = manager.configuration();
        stringCache.clear();
        listCache.clear();
        seedMissingDefaults();
    }

    public List<String> getList(NamedListSource entry) {
        List<String> cached = listCache.get(entry.key());
        if (cached != null) return cached;
        List<String> result = readLanguageList(entry);
        listCache.put(entry.key(), result);
        return result;
    }

    private List<String> readLanguageList(NamedListSource entry) {
        if (languageFile == null) {
            return ChatColorFormatter.translateList((List<String>) entry.values(), 1);
        }

        String path = entry.key();
        boolean exists = languageFile.contains(path);
        List<String> value;
        if (exists) {
            value = languageFile.getStringList(path);
        } else {
            value = (List<String>) entry.values();
            languageFile.set(path, value);
            saveLanguageFileIfAvailable();
            return ChatColorFormatter.translateList(value, 1);
        }
        if (value.isEmpty()) {
            value = (List<String>) entry.values();
            languageFile.set(path, value);
            saveLanguageFileIfAvailable();
        }
        return ChatColorFormatter.translateList(value, 1);
    }

    private String readLanguageString(TranslatedEntry entry) {
        if (languageFile == null) {
            return ChatColorFormatter.formatString(entry.translatedValue(), 1);
        }

        String path = entry.key();
        boolean exists = languageFile.contains(path);
        String value = exists ? languageFile.getString(path) : null;
        if (value == null) {
            value = entry.translatedValue();
            languageFile.set(path, value);
            saveLanguageFileIfAvailable();
        }
        return ChatColorFormatter.formatString(value, 1);
    }

    public String getMessage(TranslatedEntry entry) {
        String cached = stringCache.get(entry.key());
        if (cached != null) return cached;
        String value = readLanguageString(entry);
        stringCache.put(entry.key(), value);
        return value;
    }

    private void seedMissingDefaults() {
        if (languageFile == null) return;
        boolean changed = false;
        Iterable<?> entries = LanguageCatalog.INSTANCE.getLanguageEntries();
        for (Object languageValue : entries) {
            LanguageEntry entry = (LanguageEntry) languageValue;
            String path = entry.key();
            if (languageFile.contains(path)) continue;
            if (entry instanceof TranslatedEntry) {
                languageFile.set(path,
                        ((TranslatedEntry) entry).translatedValue());
                changed = true;
            } else if (entry instanceof NamedListSource) {
                languageFile.set(path,
                        ((NamedListSource) entry).values());
                changed = true;
            } else {
                throw new NoWhenBranchMatchedException();
            }
        }
        if (changed) saveLanguageFileIfAvailable();
    }

    private void saveLanguageFileIfAvailable() {
        if (languageManager != null) languageManager.save();
    }
}
