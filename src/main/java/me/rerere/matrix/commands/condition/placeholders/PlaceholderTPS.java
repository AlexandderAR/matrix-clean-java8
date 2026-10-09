package me.rerere.matrix.commands.condition.placeholders;

import org.bukkit.entity.Player;

import me.rerere.matrix.metrics.RollingStatistics;

public class PlaceholderTPS extends AbstractStandardPlaceholder {
    public PlaceholderTPS() { super("tps"); }
    @Override public double getStat(Player player) { return RollingStatistics.metric(); }
}
