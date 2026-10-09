package me.rerere.matrix.commands;

import java.util.List;
import org.bukkit.command.CommandSender;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.config.MatrixConfigBridge;
import me.rerere.matrix.i18n.LocaleConfiguration;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.thirdparty.kotlin.Unit;

/** Command for selecting the locale used by the plugin. */
public final class LanguageCommand extends AbstractCommandDescriptor {
    public LanguageCommand() {
        super("lang", "<locale>",
              localizedDefaultTitle(),
              "matrix.command.lang");
    }

    private static String localizedDefaultTitle() {
        String title = LocaleMessages.getLocaleValue("commands.lang");
        return title != null ? title : "Switch display language";
    }

    /** Public callback entry: successful asynchronous locale switch. */
    public Unit languageSwitchSucceeded(String locale, CommandSender sender) {
        String translated = localeDisplayName(locale);
        if (translated == null) translated = locale;
        sender.sendMessage(formatLanguageSwitchSucceeded(LocaleMessages.margin, translated, locale));
        return Unit.INSTANCE;
    }

    /** Public callback entry: failed asynchronous locale switch. */
    public Unit languageSwitchFailed(CommandSender sender, Exception failure) {
        String detail = failure.getMessage();
        sender.sendMessage(formatLanguageSwitchFailed(LocaleMessages.margin, detail));
        failure.printStackTrace();
        return Unit.INSTANCE;
    }

    @Override
    public List<String> getTabCompletions() {
        return supportedLocales();
    }

    public List<String> supportedLocales() {
        return (List<String>) LocaleConfiguration.DEFAULT_POLICY.getLocales();
    }

    private static String formatLanguageSwitchSucceeded(String prefix, String translated, String locale) {
        return new StringBuilder(prefix).append("§aLanguage switched to §f").append(translated)
                .append(" §7(").append(locale).append(")").toString();
    }

    private static String formatLanguageSwitchFailed(String prefix, String detail) {
        return new StringBuilder(prefix).append("§cFailed to switch language: §f").append(detail).toString();
    }

    private static String formatLocalizedListLabel(String key, String translated) {
        return new StringBuilder(key).append(" §7(").append(translated).append("§7)").toString();
    }

    private static String formatUsage(String prefix) {
        return new StringBuilder(prefix).append("§cUsage: §f/matrix lang <locale>").toString();
    }

    private static String formatAvailableLocales(String prefix, String available) {
        return new StringBuilder(prefix).append("§7Available: §f").append(available).toString();
    }

    private static String formatUnknownLocale(String prefix, String locale, String available) {
        return new StringBuilder(prefix).append("§cUnknown locale: §f").append(locale)
                .append("§c. Available: §f").append(available).toString();
    }

    private static String formatSwitchingLanguage(String prefix) {
        return new StringBuilder(prefix).append("§7Switching language...").toString();
    }

    private static <T, E extends Throwable> T rethrow(Throwable failure) throws E {
        throw (E) failure;
    }

    private static String localeDisplayName(String locale) {
        try {
            return LocaleConfiguration.DEFAULT_POLICY.getDisplayName(locale);
        } catch (Throwable failure) {
            return LanguageCommand.<String, RuntimeException>rethrow(failure);
        }
    }

    public CharSequence localizedListLabel(String key) {
        String translated = localeDisplayName(key);
        return translated == null ? key : formatLocalizedListLabel(key, translated);
    }

    private void sendUsageAndLocales(CommandSender sender) {
        sender.sendMessage(formatUsage(LocaleMessages.margin));
        StringBuilder available = new StringBuilder();
        boolean firstLocale = true;
        for (String locale : supportedLocales()) {
            if (!firstLocale) available.append("§7, §f");
            available.append(localizedListLabel(locale));
            firstLocale = false;
        }
        sender.sendMessage(formatAvailableLocales(
                LocaleMessages.margin, available.toString()));
    }

    /** Base command callback. */
    @Override
    public void execute(CommandSender sender, String[] arguments) {
        if (arguments.length < 2) {
            sendUsageAndLocales(sender);
            return;
        }
        String locale = arguments[1];
        List<String> locales = supportedLocales();
        if (!locales.contains(locale)) {
            StringBuilder available = new StringBuilder();
            boolean firstLocale = true;
            for (String availableLocale : supportedLocales()) {
                if (!firstLocale) available.append(", ");
                available.append(availableLocale);
                firstLocale = false;
            }
            sender.sendMessage(formatUnknownLocale(
                    LocaleMessages.margin, locale, available.toString()));
            return;
        }
        sender.sendMessage(formatSwitchingLanguage(LocaleMessages.margin));
        MatrixConfigBridge companion = Matrix.Companion;
        companion.matrixInstance().getConfigManager().update(locale,
                () -> languageSwitchSucceeded(locale, sender),
                failure -> languageSwitchFailed(sender, failure));
    }

}
