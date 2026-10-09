package me.rerere.matrix.config;

import java.util.Objects;

/** Immutable value used to describe one configuration field. */
public final class ConfigField {
    private final String name;
    private final String value;
    private final boolean inline;

    public ConfigField(String name, String value, boolean inline) {
        this.name = name;
        this.value = value;
        this.inline = inline;
    }

    public boolean isInline() { return inline; }
    public boolean inline() { return inline; }
    public String value() { return value; }
    public String name() { return name; }
    public String getValue() { return value; }
    public String getName() { return name; }

    public ConfigField copy(String name, String value, boolean inline) {
        return new ConfigField(name, value, inline);
    }

    public static String renderField(String name, String value, boolean inline) {
        return "Field(name=" + name + ", value=" + value + ", inline=" + inline + ")";
    }

    public static ConfigField copy$default(ConfigField self, String name, String value,
                                           boolean inline, int mask, Object marker) {
        if ((mask & 1) != 0) name = self.name;
        if ((mask & 2) != 0) value = self.value;
        if ((mask & 4) != 0) inline = self.inline;
        return self.copy(name, value, inline);
    }

    @Override public String toString() {
        return renderField(name, value, inline);
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ConfigField)) return false;
        ConfigField that = (ConfigField) other;
        return Objects.equals(name, that.name) && Objects.equals(value, that.value)
                && inline == that.inline;
    }

    @Override public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + value.hashCode();
        result = 31 * result + Boolean.hashCode(inline);
        return result;
    }
}
