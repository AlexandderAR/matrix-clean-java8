package me.rerere.matrix.config;

import me.rerere.matrix.thirdparty.kotlin.jvm.internal.Intrinsics;
import me.rerere.matrix.thirdparty.org.simpleyaml.configuration.ConfigurationSection;
import me.rerere.matrix.checks.CheckCategory;

/** Associates a check type with its YAML configuration section. */
public final class CheckSetting {
    private final CheckCategory checkType;
    private final ConfigurationSection configurationSection;

    public CheckSetting(CheckCategory checkType, ConfigurationSection configurationSection) {
        this.checkType = checkType;
        this.configurationSection = configurationSection;
    }

    public CheckCategory getCheckType() { return checkType; }
    public CheckCategory component1() { return checkType; }
    public ConfigurationSection getConfigurationSection() { return configurationSection; }
    public ConfigurationSection component2() { return configurationSection; }

    public CheckSetting copy(CheckCategory checkType, ConfigurationSection configurationSection) {
        return new CheckSetting(checkType, configurationSection);
    }

    /** Kotlin data-class default-copy bridge with its unused dispatch key removed. */
    public static CheckSetting copyDefaults(CheckSetting source, CheckCategory checkType,
                                             ConfigurationSection configurationSection, int mask) {
        if ((mask & 1) != 0) checkType = source.checkType;
        if ((mask & 2) != 0) configurationSection = source.configurationSection;
        return source.copy(checkType, configurationSection);
    }

    @Override public int hashCode() { return 31 * checkType.hashCode() + configurationSection.hashCode(); }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CheckSetting)) return false;
        CheckSetting that = (CheckSetting) other;
        return Intrinsics.areEqual(checkType, that.checkType)
                && Intrinsics.areEqual(configurationSection, that.configurationSection);
    }

    @Override public String toString() {
        return "CheckSetting(checkType=" + checkType + ", configurationSection="
                + configurationSection + ")";
    }
}
