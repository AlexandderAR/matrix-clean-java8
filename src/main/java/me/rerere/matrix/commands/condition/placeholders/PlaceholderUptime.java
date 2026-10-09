package me.rerere.matrix.commands.condition.placeholders;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlaceholderUptime extends AbstractStandardPlaceholder {
    private long startedAtMillis;

    public PlaceholderUptime() { super("uptime"); }
    public void init(Plugin plugin) { startedAtMillis = System.currentTimeMillis(); }

    @Override
    public double getStat(Player player) {
        return (System.currentTimeMillis() - startedAtMillis) / 50L;
    }
}
