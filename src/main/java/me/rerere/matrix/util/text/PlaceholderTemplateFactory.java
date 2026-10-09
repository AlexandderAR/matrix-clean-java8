package me.rerere.matrix.util.text;

import java.util.List;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.PropertyReference0;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.PropertyReference0Impl;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Reflection;
import me.rerere.matrix.thirdparty.kotlin.reflect.KProperty;
import me.rerere.matrix.i18n.LanguageCatalog;
import me.rerere.matrix.i18n.TranslatedEntry;
import me.rerere.matrix.config.NamedListSource;

/** Factories and format bridge for descriptor/lore placeholder templates. */
public final class PlaceholderTemplateFactory {
    public static final KProperty<?>[] PROPERTY_REFERENCES;
    private static int[] CONFIGURATION;

    private PlaceholderTemplateFactory() {}

    private static List<?> resolveArguments(NamedListSource template) {
        return template.lookup();
    }

    public static NamedListSource createTemplate(String name, List<?> arguments) {
        return new NamedListSource(name, arguments);
    }

    /** Retains the original diagnostic entry point, including its stdout effects. */
    public static void main(String[] args) {
        runDiagnostics();
    }

    public static void runDiagnostics() {
        Object reloadDescription = LanguageCatalog.INSTANCE.getReloadDescription();
        System.out.println(reloadDescription);
        TranslatedEntry formatter = LanguageCatalog.INSTANCE.getReloadDescription();
        Object formattedDescription = format(formatter);
        System.out.println(formattedDescription);
        NamedListSource template = LanguageCatalog.INSTANCE.getReloadLore();
        Object resolvedArguments = resolveArguments(template);
        System.out.println(resolvedArguments);
        Object languageEntries = LanguageCatalog.INSTANCE.getLanguageEntries();
        System.out.println(languageEntries);
    }

    public static TranslatedEntry createFormatter(String name, String value) {
        return new TranslatedEntry(name, value);
    }

    private static String format(TranslatedEntry formatter) {
        return formatter.resolveTranslation();
    }

    public static void setConfiguration(int[] values) {
        CONFIGURATION = values;
    }

    public static int[] configuration() {
        return CONFIGURATION;
    }


    static {
        if (configuration() != null) {
            setConfiguration(new int[4]);
        }
        String[] propertyNames = { "<v#0>", "desc", "lore", "<v#1>" };
        KProperty<?>[] references = new KProperty<?>[2];
        PropertyReference0 reference0 = new PropertyReference0Impl(
                PlaceholderTemplateFactory.class, propertyNames[1], propertyNames[0], 1);
        references[0] = Reflection.property0(reference0);
        PropertyReference0 reference1 = new PropertyReference0Impl(
                PlaceholderTemplateFactory.class, propertyNames[2], propertyNames[3], 1);
        references[1] = Reflection.property0(reference1);
        PROPERTY_REFERENCES = references;
    }
}
