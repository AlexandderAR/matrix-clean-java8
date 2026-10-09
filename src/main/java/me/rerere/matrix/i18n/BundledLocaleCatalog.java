package me.rerere.matrix.i18n;

import java.io.InputStream;
import java.util.List;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.thirdparty.kotlin.io.CloseableKt;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.DefaultConstructorMarker;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.file.YamlFile;

/** Reads display names from the plugin's bundled locale resources. */
public final class BundledLocaleCatalog {
    private static boolean enabled;

    private BundledLocaleCatalog() {
    }

    public BundledLocaleCatalog(DefaultConstructorMarker marker) {
        this();
    }

    public List<String> getLocales() {
        return LocaleConfiguration.supportedLocales();
    }

    public String getDisplayName(String localeId) throws Throwable {
        Matrix matrix = Matrix.getInstance();
        String resourcePath = "locale/" + localeId + ".yml";
        InputStream input = matrix.getResource(resourcePath);
        if (input == null) return null;

        Throwable failure = null;
        try {
            YamlFile configuration = new YamlFile();
            configuration.load(input);
            return configuration.getString("name");
        } catch (Throwable loadFailure) {
            failure = loadFailure;
            throw loadFailure;
        } finally {
            CloseableKt.closeFinally(input, failure);
        }
    }

    public static void setEnabled(boolean enabled) { BundledLocaleCatalog.enabled = enabled; }
    public static boolean isEnabled() { return enabled; }
    public static boolean isDisabled() { return !enabled; }

    static {
        setEnabled(true);
    }
}
