package me.rerere.matrix.listeners;

import me.rerere.matrix.checks.CheckRuntimeState;
import me.rerere.matrix.config.YamlConfigValueDelegate;
import me.rerere.matrix.config.DefaultSettingProperty;

/** Constructs the two callback event variants and forwards each through its creator. */
public final class CallbackEventFactory {

    private CallbackEventFactory() { }

    public static YamlConfigValueDelegate<?> create(
            CheckRuntimeState creator, String name, Object value, int flags) {
        if ((flags & 2) != 0) value = null;
        return createPrimary(creator, name, value);
    }

    public static YamlConfigValueDelegate<?> createPrimary(CheckRuntimeState creator, String name, Object value) {
        YamlConfigValueDelegate<Object> event = new YamlConfigValueDelegate<Object>(name, creator, value);
        creator.addSubscription(event);
        return event;
    }

    public static DefaultSettingProperty createSecondary(CheckRuntimeState creator, String name, Object value) {
        DefaultSettingProperty event = new DefaultSettingProperty(name, creator, value);
        creator.addSubscription(event);
        return event;
    }

}
