package me.rerere.matrix.commands.condition.placeholders;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** A named value substitution available to condition expressions. */
public interface Placeholder {
    boolean shouldApply(String input);

    void init(Plugin plugin);

    String doSubstitution(String input, Player player);
}
