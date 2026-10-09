package me.rerere.matrix.i18n;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.thirdparty.kotlin.collections.CollectionsKt;
import me.rerere.matrix.thirdparty.kotlin.io.CloseableKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.kotlin.text.StringsKt;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.file.YamlFile;

/** Loads Matrix locale YAML files and keeps them aligned with packaged defaults. */
public class LocaleConfiguration {
    public static final BundledLocaleCatalog DEFAULT_POLICY;
    private static final List<String> SUPPORTED_LOCALES;
    public YamlFile configuration;
    private File dataFile;
    private final String locale;
    private static boolean resourceCopyEnabled;

    static {
        resourceCopyEnabled = true;
        DEFAULT_POLICY = new BundledLocaleCatalog((DefaultConstructorMarker) null);
        SUPPORTED_LOCALES = CollectionsKt.listOf(new String[] {
                "en_US", "zh_CN", "ja_JP", "de_DE", "es_ES", "ko_KR", "ru_RU" });
    }

    public LocaleConfiguration(String locale) {
        this.locale = locale;
        this.dataFile = new File(matrix().getDataFolder(), locale);
        copyLocaleResource();
        setConfiguration(createYamlFile(dataFile));
    }

    private static Matrix matrix() {
        return Matrix.Companion.matrixInstance();
    }

    private static <T, E extends Throwable> T rethrow(Throwable failure) throws E {
        throw (E) failure;
    }

    private static File createTempFile(String prefix, String suffix) {
        try {
            return File.createTempFile(prefix, suffix);
        } catch (IOException failure) {
            return LocaleConfiguration.<File, RuntimeException>rethrow(failure);
        }
    }

    private static void copyAndClose(InputStream resource, File destination) {
        Throwable failure = null;
        try {
            Files.copy(resource, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable caught) {
            failure = caught;
            LocaleConfiguration.<Object, RuntimeException>rethrow(caught);
        } finally {
            CloseableKt.closeFinally(resource, failure);
        }
    }

    public YamlFile configuration() {
        YamlFile yaml = configuration;
        if (yaml == null) {
            Intrinsics.throwUninitializedPropertyAccessException("configuration");
            return null;
        }
        return yaml;
    }

    public static List<String> supportedLocales() { return SUPPORTED_LOCALES; }

    private static String localeSuffix(String locale) { return "-" + locale; }

    private static String missingLocaleResource(String path) { return "Missing locale resource: " + path; }

    private static String missingResource(String path) { return "Missing resource: " + path; }

    private String resolveLocaleResource() {
        Locale systemLocale = Locale.getDefault();
        List<String> localeCandidates = Arrays.asList(systemLocale.toString(), systemLocale.getLanguage());
        List<String> nonBlankLocales = new ArrayList<>();
        for (String candidate : localeCandidates) {
            if (!StringsKt.isBlank(candidate)) nonBlankLocales.add(candidate);
        }
        for (String candidate : nonBlankLocales) {
            String resourcePath = localeResource(candidate, locale);
            if (matrix().getResource(resourcePath) != null) return resourcePath;
        }
        return localeResourceRoot(locale);
    }

    private static YamlFile createYamlFile(File file) {
        YamlFile yaml = new YamlFile(file);
        yaml.createOrLoadWithComments();
        return yaml;
    }

    private void copyLocaleResource() {
        if (dataFile.exists()) return;
        String resourcePath = resolveLocaleResource();
        InputStream localeResource = matrix().getResource(resourcePath);
        if (localeResource == null) throw new IllegalStateException(missingResource(resourcePath));
        File parent = dataFile.getParentFile();
        if (parent != null) parent.mkdirs();
        copyAndClose(localeResource, dataFile);
    }

    public void loadSettings() {
        if (!dataFile.exists()) copyLocaleResource();
        setConfiguration(createYamlFile(dataFile));
    }

    public void migrateLocaleResource(String resourceName, int migrationMode, byte migrationByte) {
        String resourcePath = localeResource(resourceName, locale);
        InputStream localeResource = matrix().getResource(resourcePath);
        if (localeResource == null) throw new IllegalArgumentException(missingLocaleResource(resourcePath));

        File temporaryFile = createTempFile("matrix-locale-", localeSuffix(locale));
        try {
            copyAndClose(localeResource, temporaryFile);
            YamlFile defaults = createYamlFile(temporaryFile);
            YamlFile current = configuration();
            for (String key : current.getKeys(true)) {
                if (migrationMode >= 0 && !current.isConfigurationSection(key)) {
                    defaults.set(key, current.get(key));
                }
            }
            defaults.save(dataFile);
            if (migrationByte > 0) {
                // The reference jumps to its key cast with the temporary File on the stack.
                String ignoredKey = (String) (Object) temporaryFile;
            }
        } catch (Throwable failure) {
            temporaryFile.delete();
            LocaleConfiguration.<Object, RuntimeException>rethrow(failure);
        }
        temporaryFile.delete();
    }

    public File dataFile() { return dataFile; }
    public void save() { configuration().save(); }
    public void setDataFile(File file) { dataFile = file; }
    public void setConfiguration(YamlFile yaml) { configuration = yaml; }

    public static String localeResourceRoot(String resourceName) {
        return "locale/en_US/" + resourceName;
    }

    public static String localeResource(String localeName, String resourceName) {
        return "locale/" + localeName + '/' + resourceName;
    }

    public static void setResourceCopyEnabled(boolean enabled) { resourceCopyEnabled = enabled; }
    public static boolean resourceCopyEnabled() { return resourceCopyEnabled; }
    public static boolean resourceCopyDisabled() { return !resourceCopyEnabled; }
}
