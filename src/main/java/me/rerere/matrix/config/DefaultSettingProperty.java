package me.rerere.matrix.config;

import me.rerere.matrix.thirdparty.kotlin.reflect.KProperty;
import me.rerere.matrix.checks.AbstractCheckAction;
import me.rerere.matrix.checks.CheckRuntimeState;

public final class DefaultSettingProperty extends AbstractCheckAction {
    private final Object defaultValue;
    private Object value;

    public DefaultSettingProperty(String key, CheckRuntimeState check, Object defaultValue) {
        super(key, check);
        this.defaultValue = defaultValue;
        value = this.defaultValue;
    }

    public void loadFromConfig() {
        value = getCheck().configuration().get(getLabel(), defaultValue);
    }

    public final Object getValue(CheckRuntimeState ignoredCheck, KProperty<?> ignoredProperty) { return value; }
    public final Object getDefaultValue() { return defaultValue; }
}
