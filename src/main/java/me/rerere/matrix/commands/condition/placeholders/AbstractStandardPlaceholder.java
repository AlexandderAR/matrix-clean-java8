package me.rerere.matrix.commands.condition.placeholders;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Shared matcher and replacement behavior for numeric standard placeholders. */
public abstract class AbstractStandardPlaceholder implements Placeholder {
    private final String placeholderToken;

    public AbstractStandardPlaceholder(String key) {
        this.placeholderToken = buildMatcher(key);
    }

    private static String buildMatcher(String key) { return "-" + key + "-"; }

    public abstract double getStat(Player player);

    @Override public final String doSubstitution(String input, Player player) {
        return input.replaceAll(placeholderToken, Double.toString(getStat(player)));
    }

    @Override public void init(Plugin plugin) { }

    @Override public final boolean shouldApply(String input) { return input.contains(placeholderToken); }
}
