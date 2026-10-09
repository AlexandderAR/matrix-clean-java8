package me.rerere.matrix.commands.condition.placeholders;

import org.bukkit.entity.Player;

/** Placeholder that reports the current online player count. */
public final class PlaceholderPlayerCount extends AbstractStandardPlaceholder {
    public PlaceholderPlayerCount() {
        super("player_count");
    }

    @Override
    public double getStat(Player player) {
        return org.bukkit.Bukkit.getOnlinePlayers().size();
    }
}
