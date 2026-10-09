package me.rerere.matrix.config;

import java.util.Objects;
import me.rerere.matrix.Matrix;
import me.rerere.matrix.i18n.LanguageResourceStore;
import me.rerere.matrix.i18n.LocaleMessages;
import me.rerere.matrix.compat.FoliaSupport;
import me.rerere.matrix.thirdparty.kotlin.Result$Failure;
import me.rerere.matrix.thirdparty.kotlin.Result;
import me.rerere.matrix.thirdparty.kotlin.ResultKt;
import me.rerere.matrix.thirdparty.kotlin.Unit;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function0;
import me.rerere.matrix.thirdparty.kotlin.jvm.functions.Function1;
import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import org.bukkit.Bukkit;

public final class ConfigurationManager {
    private static final long checksFailureDelayMillis = 5000L;
    private final MatrixConfiguration mainConfiguration;
    private final CheckMessages checksConfiguration;
    private final LocaleMessages languageConfiguration;

    public ConfigurationManager() {
        mainConfiguration = new MatrixConfiguration();
        checksConfiguration = new CheckMessages();
        languageConfiguration = new LocaleMessages();
    }

    public final MatrixConfiguration getMainConfiguration() { return mainConfiguration; }
    public final CheckMessages getChecksConfiguration() { return checksConfiguration; }
    public final LocaleMessages getLanguageConfiguration() { return languageConfiguration; }

    public final void update(String content, Function0<?> onSuccess, Function1<? super Exception, ?> onFailure) {
        FoliaSupport.INSTANCE.runTaskAsync(() -> {
            int high = 29463;
            byte low = (byte) 137;
            try {
                mainConfiguration.migrateLocaleResource(content, high, low);
                checksConfiguration.migrateLocaleResource(content, high, low);
                languageConfiguration.migrateLocaleResource(content, high, low);
                FoliaSupport.INSTANCE.runAtGlobalScheduler(() -> {
                    load();
                    onSuccess.invoke();
                    return Unit.INSTANCE;
                });
            } catch (Exception failure) {
                FoliaSupport.INSTANCE.runAtGlobalScheduler(() -> {
                    onFailure.invoke(failure);
                    return Unit.INSTANCE;
                });
            }
            return Unit.INSTANCE;
        });
    }

    @SuppressWarnings("unchecked")
    public static void updateDefaults(ConfigurationManager receiver, String content,
            Function0<?> onSuccess, Function1<? super Exception, ?> onFailure, int mask) {
        if ((mask & 2) != 0) onSuccess = () -> Unit.INSTANCE;
        if ((mask & 4) != 0) onFailure = ignored -> Unit.INSTANCE;
        receiver.update(content, onSuccess, onFailure);
    }

    public final void load() {
        Matrix matrix = Matrix.Companion.matrixInstance();
        if (!matrix.getDataFolder().exists()) matrix.getDataFolder().mkdirs();
        mainConfiguration.loadSettings();
        languageConfiguration.loadSettings();
        LanguageResourceStore.INSTANCE.reload(languageConfiguration);
        Object checksLoadResult;
        try {
            Object companion = Result.Companion;
            checksConfiguration.loadSettings();
            checksLoadResult = Unit.INSTANCE;
        } catch (Throwable checksLoadFailure) {
            Object companion = Result.Companion;
            checksLoadResult = ResultKt.createFailure(checksLoadFailure);
        }
        Throwable checksLoadFailure = checksLoadResult instanceof Result$Failure
                ? ((Result$Failure) checksLoadResult).exception : null;
        if (checksLoadFailure != null) {
            checksLoadFailure.printStackTrace();
            Matrix.Companion.logMessage("!!!!!!!! Failed to load checks.yml! Make sure your checks.yml is correct! !!!!!!!!");
            Matrix.Companion.logMessage("!!!!!!!! Failed to load checks.yml! Make sure your checks.yml is correct! !!!!!!!!");
            Object delayResult;
            try {
                Object companion = Result.Companion;
                Thread.sleep(checksFailureDelayMillis);
                delayResult = Unit.INSTANCE;
            } catch (Throwable delayFailure) {
                Object companion = Result.Companion;
                delayResult = ResultKt.createFailure(delayFailure);
            }
            if (delayResult instanceof Result$Failure) ((Result$Failure) delayResult).exception.printStackTrace();
            Bukkit.shutdown();
        }
        Matrix.Companion.logMessage("Loaded all configuration files");
    }

    @Override public int hashCode() {
        return Objects.hash(mainConfiguration, checksConfiguration, languageConfiguration);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ConfigurationManager)) return false;
        ConfigurationManager that = (ConfigurationManager) other;
        return Intrinsics.areEqual(mainConfiguration, that.mainConfiguration)
                && Intrinsics.areEqual(checksConfiguration, that.checksConfiguration)
                && Intrinsics.areEqual(languageConfiguration, that.languageConfiguration);
    }
}
