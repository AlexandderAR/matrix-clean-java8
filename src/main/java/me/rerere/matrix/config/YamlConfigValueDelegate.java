package me.rerere.matrix.config;

import me.rerere.matrix.checks.AbstractCheckAction;
import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.thirdparty.kotlin.reflect.KProperty;

/** Mutable configuration property backed by a SimpleYaml section. */
public final class YamlConfigValueDelegate<T> extends AbstractCheckAction {
    private final T defaultValue;
    private T currentValue;

    public YamlConfigValueDelegate(String key,
                                   CheckRuntimeState manager,
                                   T defaultValue) {
        super(key, manager);
        this.defaultValue = defaultValue;
        this.currentValue = defaultValue;
    }

    public final T defaultValue() {
        return defaultValue;
    }

    @Override
    public void loadFromConfig() {
        CheckRuntimeState check = this.getCheck();
        ConfigurationSection section = check.configuration();
        String configPath = this.getLabel();
        currentValue = (T) section.get(configPath, defaultValue);
    }

    public T getValue(CheckRuntimeState manager, KProperty<?> property) {
        return currentValue;
    }

}
